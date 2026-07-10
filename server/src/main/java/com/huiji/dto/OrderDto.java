package com.huiji.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 订单相关 DTO */
public class OrderDto {

    @Data
    public static class OrderItemRequest {
        @Min(value = 1, message = "商品 ID 无效")
        private Long productId;
        @Min(value = 1, message = "数量至少为 1")
        private Integer quantity;
    }

    @Data
    public static class CreateOrderRequest {
        @Min(value = 1, message = "门店 ID 无效")
        private Long storeId;
        private Long memberId;
        @NotEmpty(message = "订单明细不能为空")
        @Valid
        private List<OrderItemRequest> items;
        /** 优惠券核销码(可选) */
        private String couponCode;
        /** 优惠金额(分), 与 couponCode 二选一, 由前端按券面值传入 */
        private Long discountAmount;
        /** 备注 */
        private String remark;
        /** 支付方式: CASH/WECHAT/ALIPAY/BALANCE; 为空时订单置 PENDING */
        private String payMethod;
    }

    @Data
    public static class PayRequest {
        @NotBlank(message = "支付方式不能为空")
        private String payMethod;
        private Long paidAmount;
    }

    @Data
    public static class RefundRequest {
        private String reason;
    }
}
