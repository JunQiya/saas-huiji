package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** H5 会员端 DTO */
public class H5Dto {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "验证码不能为空")
        private String code;
    }
}
