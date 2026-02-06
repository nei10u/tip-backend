package com.nei10u.tip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nei10u.tip.dto.OrderDto;
import com.nei10u.tip.mapper.OrderMapper;
import com.nei10u.tip.model.Order;
import com.nei10u.tip.service.OrderService;
import com.nei10u.tip.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final UserService userService;

    @Override
    public IPage<OrderDto> getOrders(int page, String userId) {
        Page<Order> pageParam = new Page<>(page, 20);
        Long uid = parseUserId(userId);
        if (uid == null) {
            return new Page<OrderDto>(page, 20, 0);
        }
        IPage<Order> orderPage = orderMapper.getOrdersByUserId(pageParam, uid);

        return orderPage.convert(this::convertToDto);
    }

    @Override
    public IPage<OrderDto> getOrdersByStatus(int page, String userId, List<Byte> statusList) {
        Page<Order> pageParam = new Page<>(page, 20);
        Long uid = parseUserId(userId);
        if (uid == null) {
            return new Page<OrderDto>(page, 20, 0);
        }
        IPage<Order> orderPage = orderMapper.getOrdersByStatus(pageParam, uid, statusList);

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
        // 注意：Order 主键是 Long id，不能用 selectBatchIds 传 orderSn（String）。
        // 这里改用条件查询按 orderSn 批量拉取已有订单。

        List<Order> existingList = orderMapper
                .selectList(new LambdaQueryWrapper<Order>().in(Order::getOrderSn, orderSns));

        Map<String, Order> existingMap = existingList.stream()
                .collect(Collectors.toMap(Order::getOrderSn, o -> o, (a, b) -> a));

        // 2. 批量查找用户 (sid -> userId)
        // 这一步比较耗时，可以选择缓存或批量查询优化。
        // 这里简单遍历处理，因为 orders 批次通常仅 100 条。

        for (Order newOrder : orders) {
            // TODO: 解析 sid 对应的 User (关联 ID 或 专享 ID)
            // 这里假设 sid 主要是 relationId (淘宝) 或 pddPid (拼多多)
            // 简单逻辑：尝试查找用户。

            Long resolvedUserId = resolveUserId(newOrder);
            if (resolvedUserId != null) {
                newOrder.setUserId(resolvedUserId);
            }

            Order oldOrder = existingMap.get(newOrder.getOrderSn());
            // 如果是更新，保留部分不应变的字段(如主键ID)
            if (oldOrder != null) {
                newOrder.setId(oldOrder.getId());
                // 若历史数据已有 userId，优先保留（避免解析失败覆盖为 null）
                if (newOrder.getUserId() == null) {
                    newOrder.setUserId(oldOrder.getUserId());
                }
                // 新订单：仅同步落库；结算/入账由独立的结算任务处理
            }
        }

        int count = orderMapper.insertOrUpdateBatch(orders);
        log.info("批量插入/更新订单完成, count={}", count);

        return count;
    }

    private Long resolveUserId(Order order) {
        if (order == null) return null;

        // 1) TB 优先走拆分字段（更可审计，且避免 sid 混合口径）
        if (order.getRelationId() != null) {
            com.nei10u.tip.dto.UserDto user = userService.getUserByRelationId(order.getRelationId());
            if (user != null) return user.getId();
        }
        if (order.getSpecialId() != null) {
            com.nei10u.tip.dto.UserDto user = userService.getUserBySpecialId(order.getSpecialId());
            if (user != null) return user.getId();
        }

        // 简单策略：根据 sid 查 User
        // 实际业务中 sid 可能是 relationId(Long) 也可能是 String
        if (!org.springframework.util.StringUtils.hasText(order.getSid()))
            return null;
        try {
            // 尝试当作 relationId
            Long relationId = Long.parseLong(order.getSid());
            com.nei10u.tip.dto.UserDto user = userService.getUserByRelationId(relationId);
            if (user != null)
                return user.getId();

            // 尝试当作 specialId
            user = userService.getUserBySpecialId(relationId);
            if (user != null)
                return user.getId();
        } catch (NumberFormatException e) {
            // 解析失败，可能是 pddPid / unionId 等字符串
            com.nei10u.tip.dto.UserDto user = userService.getUserByPddPid(order.getSid());
            if (user != null) return user.getId();
            // 京东：可能写入 jdAuthId（字符串）
            user = userService.getUserByJdAuthId(order.getSid());
            if (user != null) return user.getId();
            user = userService.getUserByUnionId(order.getSid());
            if (user != null) return user.getId();
        }
        return null;
    }

    private Long parseUserId(String userId) {
        if (!StringUtils.hasText(userId)) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
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
