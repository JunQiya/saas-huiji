package com.huiji.repository;

import com.huiji.entity.MemberTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberTagRepository extends JpaRepository<MemberTag, Long> {

    List<MemberTag> findByTenantIdAndMemberId(Long tenantId, Long memberId);

    @Modifying
    @Query("delete from MemberTag t where t.tenantId = :tenantId and t.memberId = :memberId")
    void deleteByTenantIdAndMemberId(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    @Query("select t.memberId from MemberTag t where t.tenantId = :tenantId and t.tag = :tag")
    List<Long> findMemberIdsByTag(@Param("tenantId") Long tenantId, @Param("tag") String tag);

    @Query("select distinct t.tag from MemberTag t where t.tenantId = :tenantId order by t.tag")
    List<String> distinctTags(@Param("tenantId") Long tenantId);
}
