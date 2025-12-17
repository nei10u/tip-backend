package com.nei10u.tip.ordersync.support;

import com.nei10u.tip.ordersync.model.EcommercePlatform;
import com.nei10u.tip.ordersync.model.UnionPlatform;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盈利/分成规则（配置驱动）。
 *
 * 口径约定：
 * - baseDeductionRate：固定扣点（例如 TB/JTK 出现的 0.1，可视为联盟服务费/固定成本）
 * - platformProfitRate：本站平台盈利比例（可动态调）
 * - userShareRate：净额分给用户的比例（例如 0.6 表示用户拿 60%）
 */
@Data
public class ProfitRule {
    private String ruleId;
    private UnionPlatform unionPlatform;
    private EcommercePlatform ecommercePlatform;

    /** 规则生效时间（用于处理“多版本折扣”） */
    private LocalDateTime effectiveFrom;

    private double baseDeductionRate = 0.0d;
    private double platformProfitRate = 0.0d;
    private double userShareRate = 1.0d;
}

