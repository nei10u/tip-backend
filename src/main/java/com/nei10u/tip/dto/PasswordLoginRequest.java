package com.nei10u.tip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordLoginRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}

