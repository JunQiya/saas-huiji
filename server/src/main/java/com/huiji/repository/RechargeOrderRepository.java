package com.huiji.repository;

import com.huiji.entity.RechargeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RechargeOrderRepository extends JpaRepository<RechargeOrder, Long> {

    Optional<RechargeOrder> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<RechargeOrder> findByOutTradeNoAndDeletedFalse(String outTradeNo);

    /** 超时未支付的充值单(用于定时关单) */
    List<RechargeOrder> findByStatusAndCreatedAtBefore(String status, LocalDateTime before);

    @Query("select coalesce(sum(r.amount),0) from RechargeOrder r where r.tenantId = :tenantId and r.status = 'SUCCESS' and r.createdAt >= :start and r.createdAt < :end")
    Long sumSuccessAmount(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
