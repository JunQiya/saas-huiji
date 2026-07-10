package com.huiji.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 商品/服务 DTO */
public class ProductDto {

    @Data
    public static class ProductRequest {
        @NotBlank(message = "商品名称不能为空")
        private String name;
        /** SERVICE / GOODS */
        @NotBlank(message = "商品类型不能为空")
        private String category;
        private String cover;
        @Min(value = 0, message = "价格不能小于 0")
        private Long price;
        private Long costPrice;
        /** 仅 GOODS 有效; SERVICE 留空 */
        private Integer stock;
        /** ACTIVE / DISABLED */
        private String status;
        private String description;
        private List<Long> storeIds;
    }

    @Data
    public static class StockRequest {
        /** 调整方式: SET 直接设置 / INC 增加(可为负) */
        @NotBlank(message = "调整方式不能为空")
        private String mode;
        @Min(value = 0, message = "库存数不能小于 0")
        private Integer value;
    }
}
