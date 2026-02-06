package com.nei10u.tip.ordersync.tb;

import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 淘宝分佣折扣时间表（对齐 legacy AppConfig 常量）。
 * <p>
 * 口径（来自反编译字节码）：
 * shareFee = baseCommission * (0.9 - discountRate)
 * 其中 discountRate 由“当前时间 now”按阈值选择。
 */
public final class TbDiscountSchedule {
    private TbDiscountSchedule() {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 默认折扣（早期口径）。
     * legacy: TB_DISCOUNT = 0.011f
     */
    public static final float TB_DISCOUNT = 0.011f;

    @Value
    public static class Rule {
        String effectiveFrom; // yyyy-MM-dd HH:mm:ss
        float discountRate;

        public LocalDateTime effectiveFromTime() {
            return LocalDateTime.parse(effectiveFrom, FMT);
        }
    }

    /**
     * legacy:
     * - TB_DISCOUNT_DATE_NEW      = 2022-11-04 03:10:00, TB_DISCOUNT_NEW   = 0.04
     * - TB_DISCOUNT_DATE_NEW_2    = 2023-09-12 04:00:00, TB_DISCOUNT_NEW_2 = 0.02
     * - TB_DISCOUNT_DATE_NEW_3    = 2024-03-25 00:00:00, TB_DISCOUNT_NEW_3 = 0.01
     * - TB_DISCOUNT_DATE_NEW_4    = 2024-04-11 17:10:00, TB_DISCOUNT_NEW_4 = 0.00
     * - TB_DISCOUNT_DATE_NEW_5    = 2024-04-15 05:50:00, TB_DISCOUNT_NEW_5 = 0.02
     * - TB_DISCOUNT_DATE_NEW_6    = 2024-04-23 14:30:00, TB_DISCOUNT_NEW_6 = 0.07
     * - TB_DISCOUNT_DATE_NEW_7    = 2024-04-29 00:00:00, TB_DISCOUNT_NEW_7 = 0.06
     * - TB_DISCOUNT_DATE_NEW_8    = 2025-09-24 00:00:00, TB_DISCOUNT_NEW_8 = 0.05
     */
    private static final List<Rule> RULES_DESC = List.of(
            new Rule("2025-09-24 00:00:00", 0.05f),
            new Rule("2024-04-29 00:00:00", 0.06f),
            new Rule("2024-04-23 14:30:00", 0.07f),
            new Rule("2024-04-15 05:50:00", 0.02f),
            new Rule("2024-04-11 17:10:00", 0.00f),
            new Rule("2024-03-25 00:00:00", 0.01f),
            new Rule("2023-09-12 04:00:00", 0.02f),
            new Rule("2022-11-04 03:10:00", 0.04f)
    );

    public static float chooseDiscountRate(LocalDateTime now) {
        LocalDateTime t = (now == null) ? LocalDateTime.now() : now;
        for (Rule r : RULES_DESC) {
            if (!t.isBefore(r.effectiveFromTime())) {
                return r.discountRate;
            }
        }
        return TB_DISCOUNT;
    }
}


