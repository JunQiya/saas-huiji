package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 鉴权相关请求 DTO */
public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class PasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private long expiresIn;
        private UserProfile user;
    }

    @Data
    public static class UserProfile {
        private Long id;
        private String username;
        private String name;
        private String role;
        private Long storeId;
        private Long tenantId;
    }
}
