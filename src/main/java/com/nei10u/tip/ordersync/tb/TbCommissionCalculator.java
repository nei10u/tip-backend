package com.nei10u.tip.ordersync.tb;

import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 淘宝分佣计算（仅 TB 口径）。
 * <p>
 * 对齐 OrderSyncTbUtil：
 * - baseCommission = pubShareFee; 若为 0 则取 pubSharePreFee
 * - shareFee = round2( baseCommission * (0.9 - discountRate) )
 * - orderDiscount = discountRate（用于解释口径）
 */
public final class TbCommissionCalculator {
    private TbCommissionCalculator() {
    }

    @Value
    public static class Result {
        double shareFee;
        double grossCommission;
        float orderDiscount;
    }

    public static Result calculate(
            String pubShareFee,
            String pubSharePreFee,
            LocalDateTime now
    ) {
        BigDecimal base = safeMoney(pubShareFee);
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            base = safeMoney(pubSharePreFee);
        }

        float discountRate = TbDiscountSchedule.chooseDiscountRate(now);

        BigDecimal factor = BigDecimal.valueOf(0.9d - discountRate);
        BigDecimal shareFee = base.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        return new Result(
                shareFee.doubleValue(),
                base.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                discountRate
        );
    }

    private static BigDecimal safeMoney(String s) {
        if (s == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}


