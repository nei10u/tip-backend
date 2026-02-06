package com.nei10u.tip.ordersync.tb;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nei10u.tip.mapper.OrderMapper;
import com.nei10u.tip.mapper.TbOrderRefundMapper;
import com.nei10u.tip.model.Order;
import com.nei10u.tip.model.TbOrderRefund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 淘宝退款补偿同步（对齐 legacy RefundOrderSyncScheduler 的“逐日 startTime + 翻页”思路）。
 *
 * 说明：
 * - 该接口返回的是“关系退款报表”维度数据，主要用于更新订单退款状态，并落 tb_order_refund 作为证据链。
 * - 由于不同商家/权限下字段可能不同，这里采取“尽量解析 + 失败不阻塞”的策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbRefundSyncService {

    private final TbOpenApiService tbOpenApiService;
    private final OrderMapper orderMapper;
    private final TbOrderRefundMapper tbOrderRefundMapper;

    public int syncByStartTime(String startTime, long bizType) {
        return syncByStartTime(startTime, bizType, 1L);
    }

    public int syncByStartTime(String startTime, long bizType, long pageNo) {
        long page = Math.max(1L, pageNo);
        int total = 0;

        while (true) {
            JSONObject resp = callRefund(startTime, bizType, page);
            if (resp == null) break;

            JSONArray results = extractResults(resp);
            if (results == null || results.isEmpty()) break;

            for (int i = 0; i < results.size(); i++) {
                JSONObject r = results.getJSONObject(i);
                if (r == null) continue;
                handleOne(r);
                total++;
            }

            // 简化：接口若总是返回满页，继续翻页；否则停止
            if (results.size() < 100) break;
            page++;
            if (page > 2000) { // 熔断防死循环
                log.warn("TB refund sync abort: too many pages, startTime={}", startTime);
                break;
            }
        }

        return total;
    }

    private JSONObject callRefund(String startTime, long bizType, long pageNo) {
        try {
            Map<String, String> params = new HashMap<>();
            // 这里按淘宝 Open API 形式拼装：search_option.*（兼容不同网关的参数习惯）
            params.put("search_option.page_size", "100");
            params.put("search_option.search_type", "1");
            params.put("search_option.refund_type", "0");
            params.put("search_option.start_time", startTime);
            params.put("search_option.page_no", String.valueOf(pageNo));
            params.put("search_option.biz_type", String.valueOf(bizType));
            return tbOpenApiService.relationRefund(params);
        } catch (Exception e) {
            log.error("TB refund call failed: startTime={}, bizType={}, pageNo={}", startTime, bizType, pageNo, e);
            return null;
        }
    }

    private JSONArray extractResults(JSONObject resp) {
        // 兼容：tbk_relation_refund_response -> result -> data -> results
        JSONObject root = resp.getJSONObject("tbk_relation_refund_response");
        if (root == null) root = resp;

        JSONObject result = root.getJSONObject("result");
        if (result == null) result = root.getJSONObject("rpc_result");
        if (result == null) result = root.getJSONObject("data");

        JSONObject data = result == null ? null : result.getJSONObject("data");
        if (data == null && result != null) data = result.getJSONObject("page_result");

        if (data == null) {
            // 有些返回可能直接是数组
            JSONArray arr = root.getJSONArray("results");
            if (arr != null) return arr;
            return null;
        }

        Object results = data.get("results");
        if (results instanceof JSONArray) return (JSONArray) results;
        if (results instanceof JSONObject) {
            // 某些返回可能是 {results:{result:[...]}}
            JSONObject rr = (JSONObject) results;
            JSONArray arr = rr.getJSONArray("result");
            if (arr != null) return arr;
        }
        return null;
    }

    private void handleOne(JSONObject r) {
        // tradeId 字段名兼容
        String tradeId = firstNonBlank(r, "tb_trade_id", "tbTradeId", "trade_id", "tradeId");
        if (!StringUtils.hasText(tradeId)) return;

        // 落证据链
        try {
            TbOrderRefund evidence = new TbOrderRefund();
            evidence.setTradeId(tradeId);
            evidence.setOrderSn("TB_OPEN_" + tradeId);
            evidence.setRawJson(r.toJSONString());
            Date now = new Date();
            evidence.setCreateTime(now);
            evidence.setUpdateTime(now);
            tbOrderRefundMapper.upsert(evidence);
        } catch (Exception e) {
            log.warn("Upsert tb_order_refund failed: tradeId={}", tradeId, e);
        }

        // refund_status 映射：legacy 关心 2/3/4；这里统一将“已发生退款且进入报表”标记为 103（已扣回/已确认）
        Integer refundStatus = safeInt(r, "refund_status", "refundStatus");
        Integer mapped = null;
        if (refundStatus != null) {
            if (refundStatus == 2 || refundStatus == 3 || refundStatus == 4) mapped = 103;
        } else {
            mapped = 103;
        }

        // 更新 orders 表：按 ds_order_sn=tradeId 定位（比 order_sn 更稳）
        try {
            LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
            uw.eq(Order::getDsOrderSn, tradeId);
            uw.set(Order::getRefundStatus, mapped);
            uw.set(Order::getStatusContent, "本单发生退款，佣金重新计算中");
            uw.set(Order::getUpdateTime, new Date());
            orderMapper.update(null, uw);
        } catch (Exception e) {
            log.warn("Update order refund status failed: tradeId={}", tradeId, e);
        }
    }

    private static Integer safeInt(JSONObject o, String... keys) {
        for (String k : keys) {
            try {
                Integer v = o.getInteger(k);
                if (v != null) return v;
            } catch (Exception ignore) {}
        }
        return null;
    }

    private static String firstNonBlank(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.getString(k);
            if (StringUtils.hasText(v)) return v;
            // 有些字段是 Long
            try {
                Long l = o.getLong(k);
                if (l != null) return String.valueOf(l);
            } catch (Exception ignore) {}
        }
        return null;
    }
}


