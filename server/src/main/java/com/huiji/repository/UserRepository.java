package com.huiji.repository;

import com.huiji.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    @Query("select u from User u where u.tenantId = :tenantId and u.deleted = false " +
            "and (:storeId is null or u.storeIds like concat('%', :storeId, '%')) " +
            "and (:role is null or u.role = :role) order by u.id desc")
    List<User> listByTenant(@Param("tenantId") Long tenantId,
                            @Param("storeId") String storeId,
                            @Param("role") String role);

    boolean existsByUsernameAndDeletedFalse(String username);

    /** 用户名在当前租户内是否已存在(租户级唯一) */
    boolean existsByUsernameAndTenantIdAndDeletedFalse(String username, Long tenantId);

    List<User> findByTenantIdAndDeletedFalse(Long tenantId);
}
