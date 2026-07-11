package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单扩展信息: 商城订单的物流/自提信息, 与 Order 一对一, 不修改现有 Order 实体。
 */
@Entity
@Table(name = "order_extend", indexes = {
        @Index(name = "idx_order_extend_order", columnList = "order_id", unique = true),
        @Index(name = "idx_order_extend_tenant", columnList = "tenant_id")
})
@Getter
@Setter
public class OrderExtend extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** DELIVERY 配送 / PICKUP 自提 */
    @Column(name = "delivery_type", nullable = false)
    private String deliveryType = "DELIVERY";

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    /** 详细地址 */
    @Column(name = "receiver_address")
    private String receiverAddress;

    @Column(name = "receiver_province")
    private String receiverProvince;

    @Column(name = "receiver_city")
    private String receiverCity;

    @Column(name = "receiver_district")
    private String receiverDistrict;

    /** 自提门店 ID, PICKUP 时使用 */
    @Column(name = "store_id")
    private Long storeId;

    /** 运费(分) */
    @Column
    private Long freight = 0L;

    /** 物流单号, 可空 */
    @Column(name = "tracking_no")
    private String trackingNo;

    /** 物流公司, 可空 */
    @Column(name = "tracking_company")
    private String trackingCompany;
}
