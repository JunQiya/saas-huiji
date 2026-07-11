package com.huiji.repository;

import com.huiji.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<Order> findByOrderNoAndDeletedFalse(String orderNo);

    /**
     * 订单列表筛选: 状态/门店/会员/时间。
     * storeId 强制覆盖: 角色 STAFF/CASHIER 时由 service 层强写 currentStoreId。
     */
    @Query("select o from Order o where o.tenantId = :tenantId and o.deleted = false " +
            "and (:status is null or :status = '' or o.status = :status) " +
            "and (:storeId is null or o.storeId = :storeId) " +
            "and (:memberId is null or o.memberId = :memberId) " +
            "and (:start is null or o.createdAt >= :start) " +
            "and (:end is null or o.createdAt < :end) " +
            "order by o.id desc")
    Page<Order> search(@Param("tenantId") Long tenantId,
                       @Param("status") String status,
                       @Param("storeId") Long storeId,
                       @Param("memberId") Long memberId,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end,
                       Pageable pageable);

    /** 某会员的订单(分页, 不区分删除), H5 端"我的订单"使用 */
    @Query("select o from Order o where o.tenantId = :tenantId and o.memberId = :memberId and o.deleted = false " +
            "and (:status is null or :status = '' or o.status = :status) order by o.id desc")
    Page<Order> listByMember(@Param("tenantId") Long tenantId,
                             @Param("memberId") Long memberId,
                             @Param("status") String status,
                             Pageable pageable);

    /** 今日订单统计 */
    @Query("select count(o), coalesce(sum(o.paidAmount),0) from Order o where o.tenantId = :tenantId " +
            "and o.status = 'PAID' and o.paidAt >= :start and o.paidAt < :end")
    List<Object[]> todayStats(@Param("tenantId") Long tenantId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /** 多租户已支付订单总金额(分), 用于代理商业绩统计 */
    @Query("select coalesce(sum(o.paidAmount),0) from Order o where o.tenantId in :tenantIds " +
            "and o.status = 'PAID' and o.deleted = false")
    Long sumPaidByTenantIds(@Param("tenantIds") List<Long> tenantIds);

    /** 商城订单列表: 仅含有 OrderExtend 记录的订单 */
    @Query("select o from Order o where o.tenantId = :tenantId and o.deleted = false " +
            "and o.id in (select e.orderId from OrderExtend e where e.deleted = false) " +
            "and (:status is null or :status = '' or o.status = :status) " +
            "order by o.id desc")
    Page<Order> searchMallOrders(@Param("tenantId") Long tenantId,
                                  @Param("status") String status,
                                  Pageable pageable);
}
