package com.nei10u.tip.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动配置表（对应 schema.sql: ds_config_activity）
 */
@Data
@TableName("ds_config_activity")
public class DsConfigActivity {
    private Long id;
    private Integer actId;
    private String icon;
    private String des;
    private String commissionRate;
    private Integer status;
    private String displayName;
    private String packageName;
    private String tips;
    private String banner;
    private String topTips;
    private String path;
    private String jumpType;
    private Boolean supportBanner;
    private Boolean supportApp;
    private Boolean supportMini;
    private Boolean appToMini;
    private String miniGId;
    private Integer scale;
    private String rule;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}


