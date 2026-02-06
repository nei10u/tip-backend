package com.nei10u.tip.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 用户DTO
 */
@Data
public class UserDto {

    private Long id;

    // CPS字段
    private Long relationId;
    private Long specialId;
    private String tbUserId;
    private String pddPid;
    private String jdAuthId;

    // 微信字段
    private String mpOpenId;
    private String mnOpenId;
    private String unionId;
    private String nickname;
    private String avatarUrl;

    // 支付宝字段
    private String aliPayAccount;
    private String aliPayName;

    // 实名信息
    private String idCardName;
    private String idCardNum;

    // 基础信息
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String email;

    private String token;

    // 状态
    private Boolean mpStatus;
    private Boolean pddStatus;
    private Boolean jdStatus;

    // 用户状态（1正常/0禁用）
    private Integer status;

    // 冗余统计字段（可选）
    private Double userDiscount;
    private Double totalActualFee;
    private Double frozenFee;
}
