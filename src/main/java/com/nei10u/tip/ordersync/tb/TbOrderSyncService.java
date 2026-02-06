package com.nei10u.tip.ordersync.tb;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.mapper.TbOrderRefundMapper;
import com.nei10u.tip.model.Order;
import com.nei10u.tip.model.TbOrderRefund;
import com.nei10u.tip.ordersync.util.OrderSyncParseUtil;
import com.nei10u.tip.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 淘宝订单同步（直连淘宝开放平台）。
 * <p>
 * 参考 legacy OrderSyncTbUtil：
 * - pageSize=100
 * - positionIndex 游标分页 + hasNext
 * - SyncType -> queryType 映射
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbOrderSyncService {

    private final TbOpenApiService tbOpenApiService;
    private final OrderService orderService;
    private final TbOrderRefundMapper tbOrderRefundMapper;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 淘宝订单查询接口通常限制单次查询时间窗（常见为 30 分钟级别）。
     * 这里做“硬保护”，避免被动触发 API 限制导致漏单或错误重试风暴。
     */
    @Value("${app.tb.order.max-window-minutes:20}")
    private int maxWindowMinutes;

    @Value("${app.tb.order.page-size:100}")
    private int pageSize;

    /**
     * 官方 fields 可按需调整；默认取我们当前最关键的字段。
     * 参考：淘宝开放平台 taobao.tbk.order.details.get 文档（docId=43328）
     */
    @Value("${app.tb.order.fields:trade_id,tk_status,refund_tag,pub_share_fee,pub_share_pre_fee,adzone_id,relation_id,special_id,tk_paid_time,tk_create_time,tk_earning_time,tk_modified_time,item_title,item_img,alipay_total_price,pay_price,deposit_price,item_id}")
    private String fieldsCsv;

    public int syncRange(LocalDateTime start, LocalDateTime end, TbSyncType syncType) {
        return syncRange(start, end, 1L, syncType);
    }

    /**
     * @param orderScene 口径：筛选订单类型，1:所有订单，2:渠道订单，3:会员运营订单，默认为1
     */
    public int syncRange(LocalDateTime start, LocalDateTime end, long orderScene, TbSyncType syncType) {
        if (start == null || end == null) return 0;
        TbSyncType type = (syncType == null) ? TbSyncType.DAY : syncType;

        // 保护：按官方常见限制，将长窗口切分成多个小窗口执行
        int maxMin = Math.max(1, maxWindowMinutes);
        // 从 start 到 end 一共有多少分钟
        long totalMin = ChronoUnit.MINUTES.between(start, end);
        if (totalMin > maxMin) {
            int sum = 0;
            LocalDateTime cursor = start;
            while (cursor.isBefore(end)) {
                LocalDateTime next = cursor.plusMinutes(maxMin);
                if (next.isAfter(end)) next = end;
                sum += syncRange(cursor, next, orderScene, type);
                cursor = next;
            }
            return sum;
        }

        String startStr = start.format(TIME_FMT);
        String endStr = end.format(TIME_FMT);

        int total = 0;
        long pageNo = 1;
        String positionIndex = null;
        boolean hasNext = true;

        while (true) {
            JSONObject resp = callTb(startStr, endStr, orderScene, pageNo, positionIndex, type);
            if (resp == null) break;

            TbPage page = parsePage(resp);
            hasNext = page.hasNext;
            List<Order> orders = page.orders;
            if (!CollectionUtils.isEmpty(orders)) {
                int count = orderService.insertOrUpdateOrder(orders);
                total += count;
            }

            if (!hasNext) break;
            positionIndex = page.positionIndex;
            pageNo++;
            if (pageNo > 5000) { // 安全熔断，避免死循环
                log.warn("TB sync abort: too many pages, start={}, end={}", startStr, endStr);
                break;
            }
        }

        return total;
    }

    private JSONObject callTb(String start, String end, long orderScene, long pageNo, String positionIndex, TbSyncType syncType) {
        try {
            JSONObject req = new JSONObject();
            req.put("start_time", start);
            req.put("end_time", end);
            req.put("query_type", String.valueOf(syncType.getQueryType()));
            req.put("page_no", String.valueOf(pageNo));
            req.put("page_size", String.valueOf(Math.max(1, pageSize)));
            if (orderScene > 0) req.put("order_scene", String.valueOf(orderScene));
            if (StringUtils.hasText(positionIndex)) req.put("position_index", positionIndex);

            // fields：建议按业务最小集配置化（避免字段缺失导致映射失败）
            req.put("fields", fieldsCsv);

            Map<String, String> biz = new HashMap<>();
            for (String k : req.keySet()) {
                Object v = req.get(k);
                if (v != null) biz.put(k, String.valueOf(v));
            }
            return tbOpenApiService.orderDetailsGet(biz);
        } catch (Exception e) {
            log.error("TB sync call failed: start={}, end={}, pageNo={}, pos={}", start, end, pageNo, positionIndex, e);
            return null;
        }
    }

    private record TbPage(List<Order> orders, boolean hasNext, String positionIndex) {
    }

    private TbPage parsePage(JSONObject resp) {
        // 兼容：tbk_order_details_get_response -> data -> results -> publisher_order_dto
        JSONObject root = resp.getJSONObject("tbk_order_details_get_response");
        if (root == null) root = resp;
        JSONObject data = root.getJSONObject("data");
        if (data == null) data = root.getJSONObject("result");

        boolean hasNext = false;
        String positionIndex = null;
        if (data != null) {
            hasNext = data.getBooleanValue("has_next");
            positionIndex = data.getString("position_index");
        }

        JSONArray list = null;
        if (data != null) {
            JSONObject results = data.getJSONObject("results");
            if (results != null) list = results.getJSONArray("publisher_order_dto");
            if (list == null) list = data.getJSONArray("publisher_order_dto");
        }

        List<Order> mapped = new ArrayList<>();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                JSONObject o = list.getJSONObject(i);
                Order mappedOrder = mapTbOrder(o);
                if (mappedOrder != null) mapped.add(mappedOrder);
            }
        }

        return new TbPage(mapped, hasNext, positionIndex);
    }

    private Order mapTbOrder(JSONObject raw) {
        if (raw == null) return null;

        String tradeId = OrderSyncParseUtil.firstNonBlank(raw, "trade_id", "tradeId");
        if (!StringUtils.hasText(tradeId)) return null;

        LocalDateTime payTime = OrderSyncParseUtil.parseDateTime(OrderSyncParseUtil.firstNonBlank(raw, "tk_paid_time", "tkPaidTime"));
        LocalDateTime createTime = OrderSyncParseUtil.parseDateTime(OrderSyncParseUtil.firstNonBlank(raw, "tk_create_time", "tkCreateTime"));
        LocalDateTime earnTime = OrderSyncParseUtil.parseDateTime(OrderSyncParseUtil.firstNonBlank(raw, "tk_earning_time", "tkEarningTime"));
        LocalDateTime modifyTime = OrderSyncParseUtil.parseDateTime(OrderSyncParseUtil.firstNonBlank(raw, "tk_modified_time", "tkModifiedTime"));

        // 3:订单结算，12:订单付款，13:订单失效，14:订单成功
        Long tkStatus = null;
        try {
            tkStatus = raw.getLong("tk_status");
        } catch (Exception ignore) {
        }

        // 订单状态 (标准化) 0: 创建/未支付, 1: 已支付, 2: 已结算, 3: 已失效
        byte orderStatus = 1;
        if (tkStatus != null) {
            if (tkStatus == 3) orderStatus = 2;
            else if (tkStatus == 13) orderStatus = 3;
        }

        Long relationId = raw.getLong("relation_id");
        if (relationId == null) relationId = raw.getLong("relationId");
        Long specialId = raw.getLong("special_id");
        if (specialId == null) specialId = raw.getLong("specialId");
        Long adzoneId = raw.getLong("adzone_id");
        if (adzoneId == null) adzoneId = raw.getLong("adzoneId");

        String pubShareFee = raw.getString("pub_share_fee");
        String pubSharePreFee = raw.getString("pub_share_pre_fee");

        TbCommissionCalculator.Result calc = TbCommissionCalculator.calculate(pubShareFee, pubSharePreFee, LocalDateTime.now());

        // 0 含义为非维权、1 含义为维权订单 - refundStatus=101
        Integer refundTag = raw.getInteger("refund_tag");
        int refundStatus = 0;
        if (refundTag != null && refundTag == 1) refundStatus = 101;

        Order order = new Order();
        order.setOrderSn("TB_OPEN_" + tradeId);
        order.setDsOrderSn(tradeId);
        order.setUnionPlatform("TB_OPEN"); // 这里不再是“联盟平台”，仅表示同步来源
        order.setTypeNo(1);
        order.setTypeName("淘宝");

        order.setOrderTitle(OrderSyncParseUtil.firstNonBlank(raw, "item_title", "itemTitle"));
        order.setImg(OrderSyncParseUtil.firstNonBlank(raw, "item_img", "itemImg"));

        // 归因：保留拆分字段；sid 兼容存 relationId/specialId 之一（便于历史逻辑复用）
        order.setRelationId(relationId);
        order.setSpecialId(specialId);
        order.setAdZoneId(adzoneId);
        if (relationId != null) order.setSid(String.valueOf(relationId));
        else if (specialId != null) order.setSid(String.valueOf(specialId));

        // 金额
        order.setPayPrice(OrderSyncParseUtil.firstPositiveDouble(raw, "pay_price", "payPrice", "alipay_total_price"));
        order.setOrderPrice(OrderSyncParseUtil.firstPositiveDouble(raw, "alipay_total_price", "alipayTotalPrice"));

        // 分佣口径字段
        order.setGrossShareFee(calc.getGrossCommission());
        order.setShareFee(calc.getShareFee());
        order.setOrderDiscount((double) calc.getOrderDiscount());

        order.setOrderStatus(orderStatus);
        order.setOrderRealStatus(tkStatus == null ? null : tkStatus.intValue());
        order.setRefundStatus(refundStatus);
        order.setStatusContent(refundStatus == 101 ? "本单发生退款，佣金重新计算中" : null);

        // 时间
        order.setPayTime(OrderSyncParseUtil.toDate(payTime));
        order.setEarnTime(OrderSyncParseUtil.toDate(earnTime));
        order.setModifyTime(OrderSyncParseUtil.toDate(modifyTime));
        order.setCreateTime(OrderSyncParseUtil.toDate(createTime) != null ? OrderSyncParseUtil.toDate(createTime) : new Date());

        // payMonth / estimateDate：沿用当前系统口径（以 earnTime 计算）
        if (earnTime != null) {
            LocalDateTime k = earnTime.plusMonths(1).withDayOfMonth(20).withHour(0).withMinute(0).withSecond(0).withNano(0);
            order.setPayMonth(k.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            order.setEstimateDate(k.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        order.setUpdateTime(new Date());

        // 退款证据链：当 refundTag=1 时，保存原始 JSON（最小审计实现）。
        if (refundStatus == 101) {
            try {
                TbOrderRefund r = new TbOrderRefund();
                r.setTradeId(tradeId);
                r.setOrderSn(order.getOrderSn());
                r.setRawJson(raw.toJSONString());
                Date now = new Date();
                r.setCreateTime(now);
                r.setUpdateTime(now);
                tbOrderRefundMapper.upsert(r);
            } catch (Exception e) {
                log.warn("Upsert tb_order_refund failed: tradeId={}", tradeId, e);
            }
        }

        return order;
    }
}


