package com.nei10u.tip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nei10u.tip.dto.OrderDto;
import com.nei10u.tip.model.Order;

import java.util.List;

/**
 * 订单服务接口 (基于参考实现)
 */
public interface OrderService {

    /**
     * 获取用户订单列表
     */
    IPage<OrderDto> getOrders(int page, String userId);

    /**
     * 根据状态获取订单
     */
    IPage<OrderDto> getOrdersByStatus(int page, String userId, List<Byte> statusList);

    /**
     * 批量插入或更新订单
     */
    int insertOrUpdateOrder(List<Order> orders);

    /**
     * 根据订单号查询订单
     */
    OrderDto getOrderByOrderSn(String orderSn);
}
