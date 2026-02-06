package com.nei10u.tip.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nei10u.tip.dto.OrderDto;
import com.nei10u.tip.ordersync.tb.TbOrderSyncService;
import com.nei10u.tip.ordersync.tb.TbSyncType;
import com.nei10u.tip.service.OrderService;
import com.nei10u.tip.vo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单控制器
 */
@Tag(name = "订单接口")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    @Autowired
    private final OrderService orderService;

    @Autowired
    private TbOrderSyncService tbOrderSyncService;

    @Operation(summary = "获取订单列表")
    @GetMapping("/list")
    public ResponseVO<IPage<OrderDto>> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam String userId) {
        IPage<OrderDto> orders = orderService.getOrders(page, userId);
        return ResponseVO.success(orders);
    }

    @Operation(summary = "根据状态获取订单")
    @GetMapping("/list/status")
    public ResponseVO<IPage<OrderDto>> getOrdersByStatus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam String userId,
            @RequestParam List<Byte> statusList) {
        IPage<OrderDto> orders = orderService.getOrdersByStatus(page, userId, statusList);
        return ResponseVO.success(orders);
    }

    @Operation(summary = "根据订单号查询")
    @GetMapping("/{orderSn}")
    public ResponseVO<OrderDto> getOrderByOrderSn(@PathVariable String orderSn) {
        OrderDto order = orderService.getOrderByOrderSn(orderSn);
        return ResponseVO.success(order);
    }

    @Operation(summary = "启动同步获取订单接口")
    @GetMapping("/sync/minutely")
    public ResponseVO<Integer> startSyncOrdersMinutely() {
        LocalDateTime oneHourAgo = LocalDateTime.now().plusHours(1);
        int count = tbOrderSyncService.syncRange(LocalDateTime.now(), oneHourAgo, TbSyncType.DAY);
        return ResponseVO.success(count);
    }
}
