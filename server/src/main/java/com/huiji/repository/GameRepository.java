package com.huiji.repository;

import com.huiji.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("select g from Game g where g.tenantId = :tenantId and g.deleted = false " +
            "and (:status is null or :status = '' or g.status = :status) order by g.id desc")
    List<Game> findByTenantIdAndStatus(@Param("tenantId") Long tenantId,
                                       @Param("status") String status);

    @Query("select g from Game g where g.tenantId = :tenantId and g.storeId = :storeId " +
            "and g.deleted = false order by g.id desc")
    List<Game> findByTenantIdAndStoreId(@Param("tenantId") Long tenantId,
                                        @Param("storeId") Long storeId);

    Optional<Game> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);
}
