package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 员工相关 DTO */
public class EmployeeDto {

    @Data
    public static class EmployeeRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "姓名不能为空")
        private String name;
        private String phone;
        @NotBlank(message = "角色不能为空")
        private String role;
        private List<Long> storeIds;
    }

    @Data
    public static class EmployeeUpdate {
        private String name;
        private String phone;
        private String role;
        private List<Long> storeIds;
        private String status;
    }

    @Data
    public static class PasswordReset {
        private String password;
    }
}
