package com.huiji.dto;

import lombok.Data;

import java.util.List;

/** 商城相关 DTO */
public class MallDto {

    /** 分类创建/更新 */
    @Data
    public static class CategoryRequest {
        private Long id;
        private String name;
        private String icon;
        private Integer sortOrder;
        /** ENABLED / DISABLED */
        private String status;
    }

    /** 加购请求 */
    @Data
    public static class CartAddRequest {
        private Long productId;
        private Integer quantity;
    }

    /** 更新购物车项 */
    @Data
    public static class CartUpdateRequest {
        private Integer quantity;
        private Boolean selected;
    }

    /** 结算下单 */
    @Data
    public static class CheckoutRequest {
        /** 指定结算的购物车项 ID, 为空则取选中项 */
        private Long[] itemIds;
        /** DELIVERY 配送 / PICKUP 自提 */
        private String deliveryType;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String receiverProvince;
        private String receiverCity;
        private String receiverDistrict;
        /** 自提门店 ID */
        private Long storeId;
        private String remark;
        /** 优惠券 ID */
        private Long couponId;
        /** 支付方式: WECHAT/ALIPAY/BALANCE */
        private String payMethod;
    }

    /** 商城订单列表查询 */
    @Data
    public static class OrderListQuery {
        private String status;
        private Integer page;
        private Integer size;
    }
}
