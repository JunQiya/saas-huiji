package com.huiji.repository;

import com.huiji.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a where a.tenantId = :tenantId " +
            "and (:operator is null or :operator = '' or a.operatorName like concat('%', :operator, '%')) " +
            "and (:action is null or :action = '' or a.action like concat('%', :action, '%')) " +
            "and (:start is null or a.createdAt >= :start) " +
            "and (:end is null or a.createdAt < :end) order by a.id desc")
    Page<AuditLog> search(@Param("tenantId") Long tenantId,
                          @Param("operator") String operator,
                          @Param("action") String action,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          Pageable pageable);
}
