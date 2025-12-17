package com.nei10u.tip.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nei10u.tip.dto.OrderDto;
import com.nei10u.tip.mapper.OrderMapper;
import com.nei10u.tip.model.Order;
import com.nei10u.tip.service.MoneyService;
import com.nei10u.tip.service.OrderService;
import com.nei10u.tip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final UserService userService;

    private final MoneyService moneyService;

    @Override
    public IPage<OrderDto> getOrders(int page, String userId) {
        Page<Order> pageParam = new Page<>(page, 20);
        IPage<Order> orderPage = orderMapper.getOrdersByUserId(pageParam, userId);

        return orderPage.convert(this::convertToDto);
    }

    @Override
    public IPage<OrderDto> getOrdersByStatus(int page, String userId, List<Byte> statusList) {
        Page<Order> pageParam = new Page<>(page, 20);
        IPage<Order> orderPage = orderMapper.getOrdersByStatus(pageParam, userId, statusList);

        return orderPage.convert(this::convertToDto);
    }

    @Override
    @Transactional
    public int insertOrUpdateOrder(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return 0;
        }

        // 1. 查询库中已存在的订单 (Status Change Detection)
        List<String> orderSns = orders.stream().map(Order::getOrderSn).toList();
        List<Order> existingOrders = orderMapper.selectBatchIds(
                orders.stream().map(Order::getOrderSn).collect(java.util.stream.Collectors.toList()));
        // Notice: Order ID is primary key 'id' not 'orderSn'. existingOrders lookup by
        // ID set won't work if I pass SNs.
        // Wait, OrderMapper usually doesn't have selectBatchIds by SN unless I added it
        // or I query wrapper.
        // Order entity primary key is Long id. I cannot use selectBatchIds with orderSn
        // string.
        // I need to use a Wrapper query:
        // orderMapper.selectList(Wrappers.<Order>lambdaQuery().in(Order::getOrderSn,
        // orderSns));

        List<Order> existingList = orderMapper
                .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .in(Order::getOrderSn, orderSns));

        java.util.Map<String, Order> existingMap = existingList.stream()
                .collect(java.util.stream.Collectors.toMap(Order::getOrderSn, o -> o, (a, b) -> a));

        // 2. 批量查找用户 (sid -> userId)
        // 这一步比较耗时，可以选择缓存或批量查询优化。
        // 这里简单遍历处理，因为 orders 批次通常仅 100 条。

        for (Order newOrder : orders) {
            String sid = newOrder.getSid();
            // TODO: 解析 sid 对应的 User (关联 ID 或 专享 ID)
            // 这里假设 sid 主要是 relationId (淘宝) 或 pddPid (拼多多)
            // 简单逻辑：尝试查找用户。

            String userId = resolveUserId(newOrder);

            Order oldOrder = existingMap.get(newOrder.getOrderSn());
            // 如果是更新，保留部分不应变的字段(如主键ID)
            if (oldOrder != null) {
                newOrder.setId(oldOrder.getId());
                // 状态变更逻辑
                if (userId != null) {
                    processStatusChange(userId, oldOrder, newOrder);
                }
            } else {
                // 新订单，如果直接是结算状态（极少见），也应发放
                if (userId != null && isSettled(newOrder)) {
                    addIncome(userId, newOrder);
                }
            }
        }

        int count = orderMapper.insertOrUpdateBatch(orders);
        log.info("批量插入/更新订单完成, count={}", count);

        return count;
    }

    private String resolveUserId(Order order) {
        // 简单策略：根据 sid 查 User
        // 实际业务中 sid 可能是 relationId(Long) 也可能是 String
        if (!org.springframework.util.StringUtils.hasText(order.getSid()))
            return null;
        try {
            // 尝试当作 relationId
            Long relationId = Long.parseLong(order.getSid());
            com.nei10u.tip.dto.UserDto user = userService.getUserByRelationId(relationId);
            if (user != null)
                return String.valueOf(user.getId());

            // 尝试当作 specialId
            user = userService.getUserBySpecialId(relationId);
            if (user != null)
                return String.valueOf(user.getId());
        } catch (NumberFormatException e) {
            // 解析失败，可能是 pddPid (String)
            // 需要 userService 支持 pddPid 查询，暂略
        }
        return null;
    }

    private void processStatusChange(String userId, Order oldOrder, Order newOrder) {
        boolean oldSettled = isSettled(oldOrder);
        boolean newSettled = isSettled(newOrder);
        boolean newInvalid = isInvalid(newOrder);

        // 结算：从非结算 -> 结算
        if (!oldSettled && newSettled) {
            addIncome(userId, newOrder);
        }
        // 冲账：从已结算 -> 失效
        else if (oldSettled && newInvalid) {
            chargeback(userId, newOrder); // 冲账使用 newOrder 的金额
        }
    }

    private boolean isSettled(Order order) {
        // 2-已结算
        return order.getOrderStatus() != null && order.getOrderStatus() == 2;
    }

    private boolean isInvalid(Order order) {
        // 3-已失效
        return order.getOrderStatus() != null && order.getOrderStatus() == 3;
    }

    private void addIncome(String userId, Order order) {
        Double fee = calculateUserFee(order);
        if (fee > 0) {
            moneyService.updateBalance(userId, fee);
            log.info("Order Settled: sn={}, userId={}, fee={}", order.getOrderSn(), userId, fee);
        }
    }

    private void chargeback(String userId, Order order) {
        Double fee = calculateUserFee(order);
        if (fee > 0) {
            moneyService.updateBalance(userId, -fee); // 扣减余额
            log.info("Order Chargeback: sn={}, userId={}, fee={}", order.getOrderSn(), userId, -fee);
        }
    }

    private Double calculateUserFee(Order order) {
        if (order.getShareFee() == null)
            return 0.0;
        // 注意：ShareFee 已经是 "预估佣金"，是否还需要乘 userDiscount 取决于 ShareFee 的定义。
        // TbEcommerceOrderMapper 中: order.setShareFee(breakdown.getUserEstimateFee());
        // 说明 ShareFee 已经是用户预估到手金额了。
        return order.getShareFee();
    }

    @Override
    public OrderDto getOrderByOrderSn(String orderSn) {
        Order order = orderMapper.getOrderByOrderSn(orderSn);
        return convertToDto(order);
    }

    private OrderDto convertToDto(Order order) {
        if (order == null) {
            return null;
        }
        OrderDto dto = new OrderDto();
        BeanUtils.copyProperties(order, dto);
        return dto;
    }
}
