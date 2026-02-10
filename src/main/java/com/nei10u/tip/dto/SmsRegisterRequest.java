package com.nei10u.tip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsRegisterRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "密码不能为空")
    private String password;
}

