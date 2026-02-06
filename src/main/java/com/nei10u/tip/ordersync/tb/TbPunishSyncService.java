package com.nei10u.tip.ordersync.tb;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nei10u.tip.mapper.OrderMapper;
import com.nei10u.tip.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * TB 处罚/违规补偿（近似对齐 legacy PunishOrderSyncScheduler）：
 * - legacy 通过 DTK 的 TbkScPunishOrderGet 拉取处罚订单，再对本地订单做锁单/写原因。
 * - tip-backend 这里提供一个“可运行的最小实现”：尝试调用淘宝 Open API 的 punish 接口（如无权限会失败并仅记录日志）。
 * <p>
 * 注意：不同 appKey 权限可能无法调用该接口；此时你可以替换为 DTK 侧接口实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbPunishSyncService {

    private final TbOpenApiService tbOpenApiService;
    private final OrderMapper orderMapper;

    public int syncByStartTime(String startTime, int pageNo, int pageSize) {
        int page = Math.max(1, pageNo);
        int size = Math.max(1, pageSize);
        int total = 0;

        while (true) {
            JSONObject resp = callPunish(startTime, page, size);
            if (resp == null) break;

            JSONArray list = extractResults(resp);
            if (list == null || list.isEmpty()) break;

            for (int i = 0; i < list.size(); i++) {
                JSONObject o = list.getJSONObject(i);
                if (o == null) continue;
                if (handleOne(o)) total++;
            }

            if (list.size() < size) break;
            page++;
            if (page > 2000) {
                log.warn("TB punish sync abort: too many pages, startTime={}", startTime);
                break;
            }
        }

        return total;
    }

    private JSONObject callPunish(String startTime, int pageNo, int pageSize) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("page_no", String.valueOf(pageNo));
            params.put("page_size", String.valueOf(pageSize));
            params.put("start_time", startTime);
            params.put("span", "1");
            return tbOpenApiService.punishOrderGet(params);
        } catch (Exception e) {
            log.error("TB punish call failed: startTime={}, pageNo={}, pageSize={}", startTime, pageNo, pageSize, e);
            return null;
        }
    }

    private JSONArray extractResults(JSONObject resp) {
        // 兼容：tbk_sc_punish_order_get_response -> results / data / result 等
        JSONObject root = resp.getJSONObject("tbk_sc_punish_order_get_response");
        if (root == null) root = resp;

        JSONObject data = root.getJSONObject("data");
        if (data == null) data = root.getJSONObject("result");
        if (data == null) data = root.getJSONObject("results");

        if (data == null) return root.getJSONArray("results");

        JSONArray arr = data.getJSONArray("results");
        if (arr != null) return arr;
        arr = data.getJSONArray("data");
        if (arr != null) return arr;
        return null;
    }

    private boolean handleOne(JSONObject o) {
        String tradeId = firstNonBlank(o, "tb_trade_id", "tbTradeId", "trade_id", "tradeId");
        if (!StringUtils.hasText(tradeId)) return false;

        String violationType = firstNonBlank(o, "violation_type", "violationType");
        String punishStatus = firstNonBlank(o, "punish_status", "punishStatus");
        String reason = (StringUtils.hasText(violationType) ? violationType : "punish")
                + (StringUtils.hasText(punishStatus) ? (":" + punishStatus) : "");

        try {
            LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
            uw.eq(Order::getDsOrderSn, tradeId);
            uw.set(Order::getOrderLock, 1);
            uw.set(Order::getOrderRealStatus, 4);
            uw.set(Order::getOrderStatus, (byte) 6);
            uw.set(Order::getPunishReason, reason);
            uw.set(Order::getStatusContent, "订单处罚/违规：" + reason);
            uw.set(Order::getUpdateTime, new Date());
            orderMapper.update(null, uw);
            return true;
        } catch (Exception e) {
            log.warn("Update order punish failed: tradeId={}", tradeId, e);
            return false;
        }
    }

    private static String firstNonBlank(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.getString(k);
            if (StringUtils.hasText(v)) return v;
            try {
                Long l = o.getLong(k);
                if (l != null) return String.valueOf(l);
            } catch (Exception ignore) {}
        }
        return null;
    }
}


