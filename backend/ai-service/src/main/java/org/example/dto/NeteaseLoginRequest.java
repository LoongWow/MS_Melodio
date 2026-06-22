package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 网易云登录请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeteaseLoginRequest {
    
    @NotBlank(message = "登录类型不能为空")
    @Pattern(regexp = "^(phone|email)$", message = "登录类型只能是 phone 或 email")
    private String loginType;

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
