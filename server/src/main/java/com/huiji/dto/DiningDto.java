package com.huiji.dto;

import lombok.Data;

import java.util.List;

/** 线下门店点餐相关 DTO */
public class DiningDto {

    @Data
    public static class TableRequest {
        private Long id;
        private Long storeId;
        private String name;
        private String area;
        private Integer seats;
        private Integer sortOrder;
    }

    @Data
    public static class CategoryRequest {
        private Long id;
        private Long storeId;
        private String name;
        private Integer sortOrder;
        private String status;
    }

    @Data
    public static class KitchenOrderItem {
        private Long productId;
        private String name;
        private Integer quantity;
        private String remark;
    }

    @Data
    public static class H5OrderRequest {
        private Long tableId;
        private Long storeId;
        private String orderType;
        private List<H5OrderItem> items;
        private String remark;
    }

    @Data
    public static class H5OrderItem {
        private Long productId;
        private Integer quantity;
        private String remark;
    }

    @Data
    public static class BindProductsRequest {
        private List<Long> productIds;
    }

    @Data
    public static class KitchenStatusRequest {
        private String status;
    }
}
