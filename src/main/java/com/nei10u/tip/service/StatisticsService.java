package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 数据统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderMapper orderMapper;

    /**
     * 获取用户统计概览
     */
    public JSONObject getUserSummary(Long userId) {
        String sid = String.valueOf(userId);
        JSONObject result = new JSONObject();

        // 1. 订单总数 (所有状态)
        Integer totalOrders = orderMapper.countOrdersByStatus(sid, null);
        result.put("totalOrders", totalOrders);

        // 2. 有效订单数 (已结算 + 已付款)
        // 假设状态: 12-已付款, 3-已结算 (具体状态码需根据实际业务调整)
        List<Byte> validStatus = Arrays.asList((byte) 12, (byte) 3);
        Integer validOrders = orderMapper.countOrdersByStatus(sid, validStatus);
        result.put("validOrders", validOrders);

        // 3. 预估总收入 (所有状态)
        Double totalIncome = orderMapper.sumShareFeeByStatus(sid, null);
        result.put("totalEstimateIncome", totalIncome);

        // 4. 已结算收入
        List<Byte> settledStatus = Arrays.asList((byte) 3);
        Double settledIncome = orderMapper.sumShareFeeByStatus(sid, settledStatus);
        result.put("settledIncome", settledIncome);

        return result;
    }
}
