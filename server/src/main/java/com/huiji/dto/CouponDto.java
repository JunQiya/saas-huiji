package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 优惠券相关 DTO */
public class CouponDto {

    @Data
    public static class CouponRequest {
        @NotBlank(message = "券名称不能为空")
        private String name;
        @NotBlank(message = "券类型不能为空")
        private String type;
        private Long faceValue;
        private Long threshold;
        private String validType;
        private Integer validDays;
        private LocalDate validStart;
        private LocalDate validEnd;
        private Integer total;
        private Integer perLimit;
        private String scope;
    }

    @Data
    public static class GrantRequest {
        private List<Long> memberIds;
        private Long storeId;
    }

    @Data
    public static class VerifyRequest {
        @NotBlank(message = "核销码不能为空")
        private String code;
        private Long storeId;
    }
}
