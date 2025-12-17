package com.nei10u.tip.ordersync.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 盈利/分成配置。
 *
 * 配置示例（application.yml）：
 * app:
 *   profit:
 *     default-rule:
 *       base-deduction-rate: 0.0
 *       platform-profit-rate: 0.02
 *       user-share-rate: 1.0
 *     rules:
 *       - rule-id: tb_dtk_2025_09_24
 *         union-platform: DTK
 *         ecommerce-platform: TB
 *         effective-from: 2025-09-24T00:00:00
 *         base-deduction-rate: 0.10
 *         platform-profit-rate: 0.05
 *         user-share-rate: 1.0
 */
@Data
@ConfigurationProperties(prefix = "app.profit")
public class ProfitProperties {
    private ProfitRule defaultRule = new ProfitRule();
    private List<ProfitRule> rules = new ArrayList<>();
}

