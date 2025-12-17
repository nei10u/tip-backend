package com.nei10u.tip.ordersync.support;

import lombok.Builder;
import lombok.Value;

/**
 * 盈利拆解结果（用于落库与展示）。
 */
@Value
@Builder
public class ProfitBreakdown {
    /** 规则 ID（便于追溯） */
    String ruleId;

    /** 固定扣点比例（例如 0.1） */
    double baseDeductionRate;
    /** 固定扣点金额 */
    double baseDeductionFee;

    /** 本站盈利比例 */
    double platformProfitRate;
    /** 本站盈利金额 */
    double platformProfitFee;

    /** 用户分佣比例（对净额池） */
    double userShareRate;

    /** 联盟返回的佣金基数（gross） */
    double grossCommission;

    /** 用户可得预估（net） */
    double userEstimateFee;
}

