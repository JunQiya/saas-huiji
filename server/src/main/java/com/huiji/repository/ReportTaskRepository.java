package com.huiji.repository;

import com.huiji.entity.ReportTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {

    Optional<ReportTask> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    @Query("select r from ReportTask r where r.tenantId = :tenantId and r.deleted = false order by r.id desc")
    Page<ReportTask> listByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    /** 定时调度: 找出到期的启用任务 */
    @Query("select r from ReportTask r where r.deleted = false and r.enabled = true " +
            "and r.nextRunAt is not null and r.nextRunAt <= :now")
    List<ReportTask> findDue(@Param("now") LocalDateTime now);

    /** 用于统计: 租户下报告任务总数与启用数 */
    @Query("select count(r) from ReportTask r where r.tenantId = :tenantId and r.deleted = false")
    long countByTenant(@Param("tenantId") Long tenantId);

    @Query("select count(r) from ReportTask r where r.tenantId = :tenantId and r.deleted = false and r.enabled = true")
    long countEnabledByTenant(@Param("tenantId") Long tenantId);
}
