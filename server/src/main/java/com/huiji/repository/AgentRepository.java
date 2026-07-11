package com.huiji.repository;

import com.huiji.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByStatus(String status);

    Optional<Agent> findByIdAndStatus(Long id, String status);
}
