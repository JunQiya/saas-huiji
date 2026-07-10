package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 会员标签关联(一个会员多个标签)。
 */
@Entity
@Table(name = "member_tag", indexes = @Index(name = "idx_member_tag_member", columnList = "member_id"))
@Getter
@Setter
public class MemberTag extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String tag;
}
