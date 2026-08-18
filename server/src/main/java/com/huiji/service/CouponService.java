package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.CouponDto;
import com.huiji.entity.Coupon;
import com.huiji.entity.CouponRecord;
import com.huiji.entity.Member;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.CouponRepository;
import com.huiji.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 优惠券服务: CRUD、发放、核销、停用、发放记录。 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRecordRepository recordRepository;
    private final MemberRepository memberRepository;
    private final AuditHelper auditHelper;

    public List<Map<String, Object>> list(String status, String type) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        return couponRepository.listByTenant(tenantId, status, type).stream()
                .map(this::toVO).toList();
    }

    @Transactional
    public Map<String, Object> create(CouponDto.CouponRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Coupon c = new Coupon();
        c.setTenantId(tenantId);
        applyReq(c, req);
        couponRepository.save(c);
        auditHelper.record("新建优惠券", "coupon:" + c.getId(), c.getName());
        return toVO(c);
    }

    /** 批量导入优惠券(每条独立事务, 单条失败不阻塞其它) */
    @Transactional
    public Map<String, Object> importBatch(List<CouponDto.CouponRequest> reqs) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        int success = 0;
        List<String> errors = new ArrayList<>();
        if (reqs == null || reqs.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION, "导入列表不能为空");
        }
        for (int i = 0; i < reqs.size(); i++) {
            CouponDto.CouponRequest req = reqs.get(i);
            try {
                if (req.getName() == null || req.getName().isBlank()) {
                    throw new BizException(ErrorCode.VALIDATION, "券名称不能为空");
                }
                if (req.getType() == null || req.getType().isBlank()) {
                    throw new BizException(ErrorCode.VALIDATION, "券类型不能为空");
                }
                Coupon c = new Coupon();
                c.setTenantId(tenantId);
                applyReq(c, req);
                if (c.getValidType() == null) c.setValidType("DAYS");
                if (c.getValidDays() == null) c.setValidDays(30);
                if (c.getPerLimit() == null) c.setPerLimit(1);
                if (c.getScope() == null) c.setScope("ALL");
                couponRepository.save(c);
                success++;
            } catch (Exception ex) {
                errors.add("第" + (i + 1) + "条(" + (req == null ? "?" : req.getName() == null ? "?" : req.getName()) + "): " + ex.getMessage());
            }
        }
        auditHelper.record("导入优惠券", "coupons", "成功" + success + ",失败" + errors.size());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("failed", errors.size());
        resp.put("errors", errors.size() > 50 ? errors.subList(0, 50) : errors);
        return resp;
    }

    @Transactional
    public Map<String, Object> update(Long id, CouponDto.CouponRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Coupon c = couponRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        // 已发放不可改规则, 仅可改名称/状态无关字段
        if (c.getGrantedCount() != null && c.getGrantedCount() > 0) {
            if (req.getType() != null && !req.getType().equals(c.getType())) {
                throw new BizException(ErrorCode.BIZ_ERROR, "已发放的券不可修改规则");
            }
            if (req.getFaceValue() != null && !req.getFaceValue().equals(c.getFaceValue())) {
                throw new BizException(ErrorCode.BIZ_ERROR, "已发放的券不可修改面值");
            }
        }
        applyReq(c, req);
        couponRepository.save(c);
        auditHelper.record("编辑优惠券", "coupon:" + id, c.getName());
        return toVO(c);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Coupon c = couponRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        if (c.getGrantedCount() != null && c.getGrantedCount() > 0) {
            // 已发放仅停用
            c.setStatus("STOPPED");
            couponRepository.save(c);
            auditHelper.record("停用已发放券", "coupon:" + id, c.getName());
        } else {
            c.setDeleted(true);
            couponRepository.save(c);
            auditHelper.record("删除优惠券", "coupon:" + id, c.getName());
        }
    }

    /** 发放: 给指定会员批量发券 */
    @Transactional
    public Map<String, Object> grant(Long id, CouponDto.GrantRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Coupon c = couponRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        if (!"ACTIVE".equals(c.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "优惠券已停用, 无法发放");
        }
        if (req.getMemberIds() == null || req.getMemberIds().isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION, "发放会员不能为空");
        }
        int perLimit = c.getPerLimit() == null ? 1 : c.getPerLimit();
        int success = 0;
        List<Long> failed = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expire = computeExpire(c, now);
        for (Long memberId : req.getMemberIds()) {
            Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId).orElse(null);
            if (m == null) {
                failed.add(memberId);
                continue;
            }
            // 每人限领校验
            long held = recordRepository.countByTenantIdAndCouponIdAndMemberIdAndDeletedFalse(tenantId, id, memberId);
            if (held >= perLimit) {
                failed.add(memberId);
                continue;
            }
            // 总量校验
            if (c.getTotal() != null && c.getGrantedCount() >= c.getTotal()) {
                throw new BizException(ErrorCode.BIZ_ERROR, "已达发行总量, 停止发放");
            }
            CouponRecord r = new CouponRecord();
            r.setTenantId(tenantId);
            r.setCouponId(id);
            r.setCouponName(c.getName());
            r.setMemberId(memberId);
            r.setMemberName(m.getName());
            r.setCode(genCode());
            r.setStatus("UNUSED");
            r.setGrantedAt(now);
            r.setExpireAt(expire);
            r.setStoreId(req.getStoreId());
            recordRepository.save(r);
            c.setGrantedCount((c.getGrantedCount() == null ? 0 : c.getGrantedCount()) + 1);
            success++;
        }
        couponRepository.save(c);
        auditHelper.record("发放优惠券", "coupon:" + id, "成功" + success + "张");

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("failed", failed);
        return resp;
    }

    @Transactional
    public void stop(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Coupon c = couponRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        c.setStatus("STOPPED");
        couponRepository.save(c);
        auditHelper.record("停用优惠券", "coupon:" + id, c.getName());
    }


    /** 核销码展示: 只查不核销, 返回券码、券名、持券人、状态、有效期 */
    public Map<String, Object> display(String code) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        CouponRecord r = recordRepository.findByCodeAndTenantIdAndDeletedFalse(code, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "核销码无效"));
        // 自动校正过期状态
        if ("UNUSED".equals(r.getStatus()) && r.getExpireAt() != null && r.getExpireAt().isBefore(LocalDateTime.now())) {
            r.setStatus("EXPIRED");
            recordRepository.save(r);
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("code", r.getCode());
        vo.put("couponName", r.getCouponName());
        vo.put("memberName", r.getMemberName());
        vo.put("status", r.getStatus());
        vo.put("expireAt", r.getExpireAt());
        return vo;
    }

    public List<Map<String, Object>> records(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        return recordRepository.findByCoupon(tenantId, id).stream().map(this::recordVO).toList();
    }

    /** 核销: 按核销码使用 */
    @Transactional
    public Map<String, Object> verify(CouponDto.VerifyRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        CouponRecord r = recordRepository.findByCodeAndTenantIdAndDeletedFalse(req.getCode(), tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "核销码无效"));
        if (!"UNUSED".equals(r.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "该券不可核销(已用或已过期)");
        }
        if (r.getExpireAt() != null && r.getExpireAt().isBefore(LocalDateTime.now())) {
            r.setStatus("EXPIRED");
            recordRepository.save(r);
            throw new BizException(ErrorCode.BIZ_ERROR, "该券已过期");
        }
        r.setStatus("USED");
        r.setUsedAt(LocalDateTime.now());
        r.setUsedStoreId(req.getStoreId());
        recordRepository.save(r);
        // 券已用数 +1
        couponRepository.findById(r.getCouponId()).ifPresent(c -> {
            c.setUsedCount((c.getUsedCount() == null ? 0 : c.getUsedCount()) + 1);
            couponRepository.save(c);
        });
        auditHelper.record("核销优惠券", "record:" + r.getId(), req.getCode());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("recordId", r.getId());
        resp.put("memberName", r.getMemberName());
        resp.put("couponName", r.getCouponName());
        return resp;
    }

    /** H5 领券 */
    @Transactional
    public Map<String, Object> claim(Long memberId, Long couponId) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Coupon c = couponRepository.findByIdAndTenantIdAndDeletedFalse(couponId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        if (!"ACTIVE".equals(c.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "优惠券不可领取");
        }
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        int perLimit = c.getPerLimit() == null ? 1 : c.getPerLimit();
        long held = recordRepository.countByTenantIdAndCouponIdAndMemberIdAndDeletedFalse(tenantId, couponId, memberId);
        if (held >= perLimit) {
            throw new BizException(ErrorCode.BIZ_ERROR, "已达每人领取上限");
        }
        if (c.getTotal() != null && c.getGrantedCount() >= c.getTotal()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "券已领完");
        }
        LocalDateTime now = LocalDateTime.now();
        CouponRecord r = new CouponRecord();
        r.setTenantId(tenantId);
        r.setCouponId(couponId);
        r.setCouponName(c.getName());
        r.setMemberId(memberId);
        r.setMemberName(m.getName());
        r.setCode(genCode());
        r.setStatus("UNUSED");
        r.setGrantedAt(now);
        r.setExpireAt(computeExpire(c, now));
        recordRepository.save(r);
        c.setGrantedCount((c.getGrantedCount() == null ? 0 : c.getGrantedCount()) + 1);
        couponRepository.save(c);
        return recordVO(r);
    }

    // ---- 内部 ----

    private void applyReq(Coupon c, CouponDto.CouponRequest req) {
        if (req.getName() != null) c.setName(req.getName());
        if (req.getType() != null) c.setType(req.getType());
        if (req.getFaceValue() != null) c.setFaceValue(req.getFaceValue());
        if (req.getThreshold() != null) c.setThreshold(req.getThreshold());
        if (req.getValidType() != null) c.setValidType(req.getValidType());
        if (req.getValidDays() != null) c.setValidDays(req.getValidDays());
        if (req.getValidStart() != null) c.setValidStart(req.getValidStart());
        if (req.getValidEnd() != null) c.setValidEnd(req.getValidEnd());
        if (req.getTotal() != null) c.setTotal(req.getTotal());
        if (req.getPerLimit() != null) c.setPerLimit(req.getPerLimit());
        if (req.getScope() != null) c.setScope(req.getScope());
    }

    private LocalDateTime computeExpire(Coupon c, LocalDateTime now) {
        if ("DAYS".equals(c.getValidType()) && c.getValidDays() != null) {
            return now.plusDays(c.getValidDays());
        }
        if ("RANGE".equals(c.getValidType()) && c.getValidEnd() != null) {
            return c.getValidEnd().atTime(23, 59, 59);
        }
        // 默认 30 天有效
        return now.plusDays(30);
    }

    private String genCode() {
        return "CP" + System.currentTimeMillis() % 1000000
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public Map<String, Object> toVO(Coupon c) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("name", c.getName());
        vo.put("type", c.getType());
        vo.put("faceValue", c.getFaceValue());
        vo.put("threshold", c.getThreshold());
        vo.put("validType", c.getValidType());
        vo.put("validDays", c.getValidDays());
        vo.put("validStart", c.getValidStart());
        vo.put("validEnd", c.getValidEnd());
        vo.put("total", c.getTotal());
        vo.put("grantedCount", c.getGrantedCount());
        vo.put("usedCount", c.getUsedCount());
        vo.put("perLimit", c.getPerLimit());
        vo.put("scope", c.getScope());
        vo.put("status", c.getStatus());
        vo.put("createdAt", c.getCreatedAt());
        return vo;
    }

    private Map<String, Object> recordVO(CouponRecord r) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", r.getId());
        vo.put("memberId", r.getMemberId());
        vo.put("memberName", r.getMemberName());
        vo.put("couponName", r.getCouponName());
        vo.put("code", r.getCode());
        vo.put("status", r.getStatus());
        vo.put("grantedAt", r.getGrantedAt());
        vo.put("usedAt", r.getUsedAt());
        vo.put("expireAt", r.getExpireAt());
        // 附带券模板类型/面值/门槛
        if (r.getCouponId() != null) {
            couponRepository.findById(r.getCouponId()).ifPresent(c -> {
                vo.put("type", c.getType());
                vo.put("faceValue", c.getFaceValue());
                vo.put("threshold", c.getThreshold());
            });
        }
        return vo;
    }
}
