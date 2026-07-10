package com.huiji.repository;

import com.huiji.entity.MessageTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MessageTaskRepository extends JpaRepository<MessageTask, Long> {

    Optional<MessageTask> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    /**
     * 列表搜索: 状态/渠道/时间区间过滤
     */
    @Query("select m from MessageTask m where m.tenantId = :tenantId and m.deleted = false " +
            "and (:status is null or :status = '' or m.status = :status) " +
            "and (:channel is null or :channel = '' or m.channel = :channel) " +
            "and (:start is null or m.createdAt >= :start) " +
            "and (:end is null or m.createdAt < :end) " +
            "order by m.id desc")
    Page<MessageTask> search(@Param("tenantId") Long tenantId,
                              @Param("status") String status,
                              @Param("channel") String channel,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              Pageable pageable);

    /** 区间内总费用(已完成) */
    @Query("select coalesce(sum(m.cost),0) from MessageTask m where m.tenantId = :tenantId " +
            "and m.status = 'COMPLETED' and m.createdAt >= :start and m.createdAt < :end")
    Long sumCost(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 区间内总发送数(已完成) */
    @Query("select coalesce(sum(m.sentCount),0) from MessageTask m where m.tenantId = :tenantId " +
            "and m.status = 'COMPLETED' and m.createdAt >= :start and m.createdAt < :end")
    Long sumSent(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 区间内任务数(已完成) */
    @Query("select count(m) from MessageTask m where m.tenantId = :tenantId " +
            "and m.status = 'COMPLETED' and m.createdAt >= :start and m.createdAt < :end")
    Long countCompleted(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
