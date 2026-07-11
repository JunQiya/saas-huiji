package com.huiji.repository;

import com.huiji.entity.GamePlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GamePlayRepository extends JpaRepository<GamePlay, Long> {

    List<GamePlay> findByTenantIdAndGameIdAndMemberIdOrderByPlayedAtDesc(Long tenantId, Long gameId, Long memberId);

    long countByTenantIdAndGameIdAndMemberIdAndDayKey(Long tenantId, Long gameId, Long memberId, String dayKey);

    long countByTenantIdAndGameIdAndMemberId(Long tenantId, Long gameId, Long memberId);

    @Query("select count(p) from GamePlay p where p.tenantId = :tenantId and p.gameId = :gameId and p.deleted = false")
    long countByTenantIdAndGameId(@Param("tenantId") Long tenantId, @Param("gameId") Long gameId);

    @Query("select count(p) from GamePlay p where p.tenantId = :tenantId and p.gameId = :gameId " +
            "and p.isWin = true and p.deleted = false")
    long countWinByTenantIdAndGameId(@Param("tenantId") Long tenantId, @Param("gameId") Long gameId);

    @Query("select p.prizeId, p.prizeName, count(p) from GamePlay p " +
            "where p.tenantId = :tenantId and p.gameId = :gameId and p.isWin = true and p.deleted = false " +
            "group by p.prizeId, p.prizeName")
    List<Object[]> countByPrize(@Param("tenantId") Long tenantId, @Param("gameId") Long gameId);
}
