package com.nei10u.tip.service;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 数据统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderMapper orderMapper;
    private static final DateTimeFormatter PAY_MONTH_KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 获取用户统计概览
     */
    public JSONObject getUserSummary(Long userId) {
        JSONObject result = new JSONObject();

        // 1. 订单总数 (所有状态)
        Integer totalOrders = userId == null ? 0 : orderMapper.countOrdersByStatus(userId, null);
        result.put("totalOrders", totalOrders);

        // 2. 有效订单数 (已结算 + 已付款)
        // order_status 为内部标准化状态：1-已支付，2-已结算
        List<Byte> validStatus = Arrays.asList((byte) 1, (byte) 2);
        Integer validOrders = userId == null ? 0 : orderMapper.countOrdersByStatus(userId, validStatus);
        result.put("validOrders", validOrders);

        // 3. 预估总收入 (所有状态)
        Double totalIncome = userId == null ? 0.0d : orderMapper.sumShareFeeByStatus(userId, null);
        result.put("totalEstimateIncome", totalIncome);

        // 4. 已结算收入
        List<Byte> settledStatus = Arrays.asList((byte) 2);
        Double settledIncome = userId == null ? 0.0d : orderMapper.sumShareFeeByStatus(userId, settledStatus);
        result.put("settledIncome", settledIncome);

        // 5. payMonth 维度：本月/下月/待入账（按“下一个 20 号”为本月结算窗口）
        if (userId != null) {
            String currentKey = currentPayMonthKey();
            String nextKey = nextPayMonthKey(currentKey);
            result.put("currentMonthKey", currentKey);
            result.put("nextMonthKey", nextKey);

            Double currentMonthReceivable = orderMapper.sumReceivableByPayMonth(userId, currentKey);
            Double nextMonthReceivable = orderMapper.sumReceivableByPayMonth(userId, nextKey);
            Double toBeReceive = orderMapper.sumToBeReceiveAfterPayMonth(userId, nextKey);
            Double creditedTotal = orderMapper.sumCreditedFee(userId);

            result.put("currentMonthReceivable", currentMonthReceivable);
            result.put("nextMonthReceivable", nextMonthReceivable);
            result.put("toBeReceive", toBeReceive);
            result.put("creditedTotal", creditedTotal);
        }

        return result;
    }

    private static String currentPayMonthKey() {
        LocalDate today = LocalDate.now();
        LocalDate thisMonth20 = today.withDayOfMonth(20);
        LocalDate keyDate = today.isAfter(thisMonth20) ? thisMonth20.plusMonths(1) : thisMonth20;
        return keyDate.format(PAY_MONTH_KEY_FMT);
    }

    private static String nextPayMonthKey(String currentKey) {
        LocalDate cur = LocalDate.parse(currentKey, PAY_MONTH_KEY_FMT);
        return cur.plusMonths(1).format(PAY_MONTH_KEY_FMT);
    }
}
