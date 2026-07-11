package com.huiji.repository;

import com.huiji.entity.GamePrize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GamePrizeRepository extends JpaRepository<GamePrize, Long> {

    List<GamePrize> findByGameIdOrderBySortOrderAsc(Long gameId);

    Optional<GamePrize> findByGameIdAndId(Long gameId, Long id);

    List<GamePrize> findByGameIdAndDeletedFalseOrderBySortOrderAsc(Long gameId);
}
