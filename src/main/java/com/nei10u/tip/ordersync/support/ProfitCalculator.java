package com.nei10u.tip.ordersync.support;

import com.nei10u.tip.ordersync.model.EcommercePlatform;
import com.nei10u.tip.ordersync.model.UnionPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

/**
 * 本站盈利/用户可得计算器（配置驱动）。
 *
 * 目标：
 * - 把“平台盈利比率”从散落在各同步器里的 if/else 收敛到统一抽象
 * - 支持按 union + ecommerce + 生效时间选择规则（兼容历史版本）
 * - 输出可落库字段：gross / baseDeduction / platformProfit / userEstimate
 */
@Component
@RequiredArgsConstructor
public class ProfitCalculator {

    private final ProfitProperties profitProperties;

    public ProfitBreakdown calculate(
            UnionPlatform unionPlatform,
            EcommercePlatform ecommercePlatform,
            LocalDateTime orderTime,
            double grossCommission
    ) {
        final ProfitRule rule = chooseRule(unionPlatform, ecommercePlatform, orderTime);

        final double baseDeductionRate = clampRate(rule.getBaseDeductionRate());
        final double platformProfitRate = clampRate(rule.getPlatformProfitRate());
        final double userShareRate = clampRate(rule.getUserShareRate());

        // 净额池 = gross * (1 - 固定扣点 - 平台盈利)
        final double netPoolRate = clampRate(1.0d - baseDeductionRate - platformProfitRate);
        final double baseDeductionFee = money(grossCommission * baseDeductionRate);
        final double platformProfitFee = money(grossCommission * platformProfitRate);
        final double userEstimateFee = money(grossCommission * netPoolRate * userShareRate);

        return ProfitBreakdown.builder()
                .ruleId(rule.getRuleId())
                .baseDeductionRate(baseDeductionRate)
                .baseDeductionFee(baseDeductionFee)
                .platformProfitRate(platformProfitRate)
                .platformProfitFee(platformProfitFee)
                .userShareRate(userShareRate)
                .grossCommission(money(grossCommission))
                .userEstimateFee(userEstimateFee)
                .build();
    }

    private ProfitRule chooseRule(UnionPlatform unionPlatform, EcommercePlatform ecommercePlatform, LocalDateTime orderTime) {
        final LocalDateTime t = (orderTime == null) ? LocalDateTime.now() : orderTime;

        Optional<ProfitRule> matched = profitProperties.getRules().stream()
                .filter(r -> r.getUnionPlatform() == unionPlatform && r.getEcommercePlatform() == ecommercePlatform)
                .filter(r -> r.getEffectiveFrom() == null || !r.getEffectiveFrom().isAfter(t))
                .max(Comparator.comparing(r -> r.getEffectiveFrom() == null ? LocalDateTime.MIN : r.getEffectiveFrom()));

        if (matched.isPresent()) return matched.get();

        // defaultRule 没有指定平台时，允许作为兜底（只提供比例）
        ProfitRule def = profitProperties.getDefaultRule();
        if (def.getRuleId() == null || def.getRuleId().isBlank()) {
            def.setRuleId("default");
        }
        return def;
    }

    private static double clampRate(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v < 0.0d) return 0.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private static double money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

