package com.nei10u.tip.ordersync.impl.tb;

import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.model.Order;
import com.nei10u.tip.ordersync.model.EcommercePlatform;
import com.nei10u.tip.ordersync.model.RawUnionOrder;
import com.nei10u.tip.ordersync.spi.EcommerceOrderMapper;
import com.nei10u.tip.ordersync.support.ProfitBreakdown;
import com.nei10u.tip.ordersync.support.ProfitCalculator;
import com.nei10u.tip.ordersync.util.OrderSyncParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 淘宝订单映射器。
 * <p>
 * 输入：联盟平台原始订单 JSON（字段存在差异，做强容错）
 * 输出：本站 Order（落库口径）
 */
@Slf4j
@Component
public class TbEcommerceOrderMapper implements EcommerceOrderMapper {

    @Override
    public EcommercePlatform ecommercePlatform() {
        return EcommercePlatform.TB;
    }

    @Override
    public Order mapToOrder(RawUnionOrder rawOrder, ProfitCalculator profitCalculator) {
        JSONObject rawOrderJSON = rawOrder.getRaw();
        if (rawOrderJSON == null)
            return null;

        String dsOrderSn = OrderSyncParseUtil.firstNonBlank(rawOrderJSON,
                "tradeId", "trade_id", "orderId", "order_id", "dsOrderSn", "ds_order_sn");
        if (!StringUtils.hasText(dsOrderSn)) {
            log.warn("Skip TB order: missing dsOrderSn, keys={}", rawOrderJSON.keySet());
            return null;
        }

        String orderSn = rawOrder.getUnionPlatform().name() + "_" + dsOrderSn;

        String title = OrderSyncParseUtil.firstNonBlank(rawOrderJSON,
                "itemTitle", "title", "orderTitle", "order_title", "goodsTitle", "goods_title");
        String img = OrderSyncParseUtil.firstNonBlank(rawOrderJSON,
                "itemImg", "img", "mainPic", "main_pic", "pictUrl", "pict_url");
        String sid = OrderSyncParseUtil.firstNonBlank(rawOrderJSON,
                "sid", "specialId", "special_id", "relationId", "relation_id", "unionId", "union_id");

        Double payPrice = OrderSyncParseUtil.firstPositiveDouble(rawOrderJSON,
                "payPrice", "pay_price", "alipayTotalPrice", "alipay_total_price", "paidAmount");
        Double orderPrice = OrderSyncParseUtil.firstPositiveDouble(rawOrderJSON,
                "orderPrice", "order_price", "totalPrice", "total_price");

        // 佣金比例：多数平台是百分比口径（如 10.5 表示 10.5%），这里仅存储展示，不作为净额计算基准
        Double shareRate = OrderSyncParseUtil.firstPositiveDouble(rawOrderJSON,
                "commissionRate", "commission_rate", "tkRate", "tk_rate", "shareRate", "share_rate");

        // grossCommission：联盟返回的“可分配金额口径”（TB 场景常见 pubShareFee / pubSharePreFee）
        Double grossCommission = OrderSyncParseUtil.firstPositiveDouble(rawOrderJSON,
                "pubShareFee", "pub_share_fee",
                "pubSharePreFee", "pub_share_pre_fee",
                "commission", "estimateAmount", "estimate_amount");
        if (grossCommission == null)
            grossCommission = 0.0d;

        LocalDateTime payTime = OrderSyncParseUtil.parseDateTime(
                OrderSyncParseUtil.firstNonBlank(rawOrderJSON, "tkPaidTime", "payTime", "pay_time", "paidTime"));
        Date payTimeDate = OrderSyncParseUtil.toDate(payTime);

        // 订单状态映射
        // DTK/TB 常见状态：
        // 12-付款，14-确认收货(未结算)，3-结算成功，13-失效
        // 映射到内部状态(Order.orderStatus)：
        // 0-创建，1-已支付(12, 14)，2-已结算(3)，3-已失效(13)
        Integer tkStatus = null;
        try {
            tkStatus = OrderSyncParseUtil.firstPositiveInteger(rawOrderJSON, "tkStatus", "tk_status", "status");
        } catch (Exception ignore) {
        }

        Byte orderStatus = 1; // 默认已支付
        if (tkStatus != null) {
            if (tkStatus == 3) {
                orderStatus = 2; // 已结算
            } else if (tkStatus == 13) {
                orderStatus = 3; // 已失效
            } else {
                orderStatus = 1; // 12, 14 等归为已支付
            }
        }

        String statusContent = OrderSyncParseUtil.firstNonBlank(rawOrderJSON, "statusContent", "status_content", "status");
        if (!StringUtils.hasText(statusContent)) {
            // 根据状态码补全描述
            if (tkStatus != null) {
                if (tkStatus == 12)
                    statusContent = "已付款";
                else if (tkStatus == 14)
                    statusContent = "确认收货";
                else if (tkStatus == 3)
                    statusContent = "已结算";
                else if (tkStatus == 13)
                    statusContent = "已失效";
                else
                    statusContent = "未知状态(" + tkStatus + ")";
            } else {
                statusContent = "已支付";
            }
        }

        ProfitBreakdown breakdown = profitCalculator.calculate(
                rawOrder.getUnionPlatform(),
                EcommercePlatform.TB,
                payTime,
                grossCommission);

        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setDsOrderSn(dsOrderSn);
        order.setOrderTitle(title);
        order.setImg(img);
        order.setSid(sid);
        order.setUnionPlatform(rawOrder.getUnionPlatform().name());
        order.setTypeNo(EcommercePlatform.TB.getTypeNo());
        order.setTypeName(EcommercePlatform.TB.getDisplayName());

        order.setOrderPrice(orderPrice);
        order.setPayPrice(payPrice);
        order.setShareRate(shareRate);

        // 统一口径：shareFee = 用户可得预估（净额）
        order.setShareFee(breakdown.getUserEstimateFee());

        // 盈利拆解字段：用于对账/展示
        order.setGrossShareFee(breakdown.getGrossCommission());
        order.setBaseDeductionRate(breakdown.getBaseDeductionRate());
        order.setBaseDeductionFee(breakdown.getBaseDeductionFee());
        order.setPlatformProfitRate(breakdown.getPlatformProfitRate());
        order.setPlatformProfitFee(breakdown.getPlatformProfitFee());
        order.setUserDiscount(breakdown.getUserShareRate());
        // orderDiscount：记录“总扣点”（固定扣点 + 平台盈利），便于解释展示口径
        order.setOrderDiscount(breakdown.getBaseDeductionRate() + breakdown.getPlatformProfitRate());

        order.setOrderStatus(orderStatus);
        order.setStatusContent(statusContent);
        order.setPayTime(payTimeDate);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        return order;
    }
}
