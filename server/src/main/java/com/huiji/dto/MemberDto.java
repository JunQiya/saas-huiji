package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 会员相关 DTO */
public class MemberDto {

    @Data
    public static class MemberRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;
        @NotBlank(message = "手机号不能为空")
        private String phone;
        private String gender;
        private LocalDate birthday;
        private List<Long> storeIds;
        private List<String> tags;
        private String remark;
    }

    @Data
    public static class RechargeRequest {
        private Long amount;
        private Long gift;
        private String payMethod;
        private String remark;
    }

    @Data
    public static class ConsumeRequest {
        private Long amount;
        private Long storeId;
        private String items;
        private String remark;
        /** 本次消费使用的券核销码(可选), 传入则核销对应券 */
        private String couponCode;
    }

    @Data
    public static class TagsRequest {
        private List<String> tags;
    }

    @Data
    public static class BatchTagsRequest {
        private List<Long> memberIds;
        private List<String> tags;
    }

    @Data
    public static class BatchLevelRequest {
        private List<Long> memberIds;
        private Integer level;
    }

    @Data
    public static class PointsAdjustRequest {
        /** 积分变化量，正数为增加，负数为扣减 */
        private Long delta;
        private String reason;
    }
}
