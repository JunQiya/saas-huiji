package com.huiji.repository;

import com.huiji.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<Member> findByPhoneAndTenantIdAndDeletedFalse(String phone, Long tenantId);

    /** 微信授权登录: 按 openid 查找会员 */
    Optional<Member> findByWxOpenidAndTenantIdAndDeletedFalse(String wxOpenid, Long tenantId);

    /** H5 登录: 按手机号跨租户查找(演示单租户场景) */
    Optional<Member> findFirstByPhoneAndDeletedFalseOrderByIdAsc(String phone);

    boolean existsByPhoneAndTenantIdAndDeletedFalse(String phone, Long tenantId);

    /**
     * 会员列表查询: 关键字(姓名/手机)、等级、门店、标签筛选。
     * storeIds 以 like 匹配(逗号分隔存储)。
     */
    @Query("select m from Member m where m.tenantId = :tenantId and m.deleted = false " +
            "and (:keyword is null or :keyword = '' or lower(m.name) like lower(concat('%', :keyword, '%')) or m.phone like concat('%', :keyword, '%')) " +
            "and (:level is null or m.level = :level) " +
            "and (:storeFilter is null or m.storeIds like concat('%', :storeFilter, '%')) " +
            "order by m.id desc")
    Page<Member> search(@Param("tenantId") Long tenantId,
                        @Param("keyword") String keyword,
                        @Param("level") Integer level,
                        @Param("storeFilter") String storeFilter,
                        Pageable pageable);

    /** 按关键字(姓名/手机)取会员 id 列表, 供全局流水按会员筛选 */
    @Query("select m.id from Member m where m.tenantId = :tenantId and m.deleted = false " +
            "and (lower(m.name) like lower(concat('%', :keyword, '%')) or m.phone like concat('%', :keyword, '%'))")
    List<Long> findIdsByKeyword(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);

    @Query("select m.id from Member m where m.tenantId = :tenantId and m.deleted = false")
    List<Long> allMemberIds(@Param("tenantId") Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);

    @Query("select count(m) from Member m where m.tenantId = :tenantId and m.deleted = false and m.createdAt >= :start")
    long countNewAfter(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start);

    @Query("select count(m) from Member m where m.tenantId = :tenantId and m.deleted = false and m.lastConsumeAt >= :start")
    long countActiveAfter(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start);

    @Query("select count(m) from Member m where m.tenantId = :tenantId and m.deleted = false and m.lastConsumeAt is not null and m.lastConsumeAt < :dormantBefore")
    long countDormant(@Param("tenantId") Long tenantId, @Param("dormantBefore") LocalDateTime dormantBefore);
}
