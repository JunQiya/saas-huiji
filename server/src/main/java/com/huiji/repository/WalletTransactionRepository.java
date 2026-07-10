package com.huiji.repository;

import com.huiji.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Query("select t from WalletTransaction t where t.tenantId = :tenantId and t.memberId = :memberId " +
            "and (:type is null or :type = '' or t.type = :type) order by t.id desc")
    Page<WalletTransaction> findByMember(@Param("tenantId") Long tenantId,
                                         @Param("memberId") Long memberId,
                                         @Param("type") String type,
                                         Pageable pageable);

    List<WalletTransaction> findTop5ByTenantIdAndMemberIdOrderByIdDesc(Long tenantId, Long memberId);

    /** 区间内充值总额(含赠送) */
    @Query("select coalesce(sum(t.amount),0) from WalletTransaction t where t.tenantId = :tenantId " +
            "and t.type = 'RECHARGE' and t.createdAt >= :start and t.createdAt < :end")
    Long sumRecharge(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 区间内消费总额(取绝对值) */
    @Query("select coalesce(sum(abs(t.amount)),0) from WalletTransaction t where t.tenantId = :tenantId " +
            "and t.type = 'CONSUME' and t.createdAt >= :start and t.createdAt < :end")
    Long sumConsume(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 区间内消费笔数 */
    @Query("select count(t) from WalletTransaction t where t.tenantId = :tenantId " +
            "and t.type = 'CONSUME' and t.createdAt >= :start and t.createdAt < :end")
    Long countConsume(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按小时统计消费笔数(0-23) */
    @Query("select hour(t.createdAt), count(t) from WalletTransaction t where t.tenantId = :tenantId " +
            "and t.type = 'CONSUME' and t.createdAt >= :start group by hour(t.createdAt) order by hour(t.createdAt)")
    List<Object[]> countByHour(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start);

    @Query("select t from WalletTransaction t where t.tenantId = :tenantId and t.type = 'CONSUME' " +
            "and t.createdAt >= :start and t.createdAt < :end order by t.id desc")
    List<WalletTransaction> consumeInRange(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 全局流水查询: 按租户/类型/门店/会员/时间区间筛选, memberIds 为 null 时不限制。
     * 排序: id desc。
     */
    @Query("select t from WalletTransaction t where t.tenantId = :tenantId " +
            "and (:type is null or :type = '' or t.type = :type) " +
            "and (:storeId is null or t.storeId = :storeId) " +
            "and (:memberId is null or t.memberId = :memberId) " +
            "and (:start is null or t.createdAt >= :start) " +
            "and (:end is null or t.createdAt < :end) " +
            "and (:memberIds is null or t.memberId in :memberIds) " +
            "order by t.id desc")
    Page<WalletTransaction> searchGlobal(@Param("tenantId") Long tenantId,
                                         @Param("type") String type,
                                         @Param("storeId") Long storeId,
                                         @Param("memberId") Long memberId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("memberIds") Collection<Long> memberIds,
                                         Pageable pageable);

    /**
     * 全局流水汇总: 按 type 分组返回 [type, sumAbsAmount, count]。
     * memberIds 为 null 时不限制。
     */
    @Query("select t.type, coalesce(sum(abs(t.amount)),0), count(t) from WalletTransaction t " +
            "where t.tenantId = :tenantId " +
            "and (:type is null or :type = '' or t.type = :type) " +
            "and (:storeId is null or t.storeId = :storeId) " +
            "and (:memberId is null or t.memberId = :memberId) " +
            "and (:start is null or t.createdAt >= :start) " +
            "and (:end is null or t.createdAt < :end) " +
            "and (:memberIds is null or t.memberId in :memberIds) " +
            "group by t.type")
    List<Object[]> summaryGlobal(@Param("tenantId") Long tenantId,
                                 @Param("type") String type,
                                 @Param("storeId") Long storeId,
                                 @Param("memberId") Long memberId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("memberIds") Collection<Long> memberIds);
}
