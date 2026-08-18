package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    /** H5 会员充值请求 */
    @Data
    public static class RechargeRequest {
        /** 充值金额(分) */
        @NotNull(message = "充值金额不能为空")
        @Positive(message = "充值金额必须大于 0")
        private Long amount;
        /** 支付方式: WECHAT / BALANCE / CASH, 演示环境直接到账 */
        private String payMethod;

        /** 转换为会员充值内部请求 */
        public MemberDto.RechargeRequest toMemberRecharge() {
            MemberDto.RechargeRequest req = new MemberDto.RechargeRequest();
            req.setAmount(this.amount);
            req.setPayMethod(this.payMethod == null ? "WECHAT" : this.payMethod);
            req.setRemark("H5 会员端充值");
            return req;
        }
    }
}
