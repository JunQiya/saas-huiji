package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 门店相关 DTO */
public class StoreDto {

    @Data
    public static class StoreRequest {
        @NotBlank(message = "门店名称不能为空")
        private String name;
        private String address;
        private String phone;
        private String businessHours;
        private String status;
    }
}
