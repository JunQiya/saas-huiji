package com.huiji.repository;

import com.huiji.entity.OrderExtend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderExtendRepository extends JpaRepository<OrderExtend, Long> {

    /** 按订单 ID 查扩展信息 */
    Optional<OrderExtend> findByOrderIdAndDeletedFalse(Long orderId);
}
