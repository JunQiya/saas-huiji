package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.entity.Coupon;
import com.huiji.entity.CouponRecord;
import com.huiji.entity.Member;
import com.huiji.entity.Referral;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.CouponRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.ReferralRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐裂变服务: 6 位推荐码 / 关系绑定 / 统计 / 奖励发放。
 * 不修改 Member 实体, code 冗余在 Referral 上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RAND = new SecureRandom();

    private final ReferralRepository referralRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final CouponRecordRepository couponRecordRepository;
    private final WalletTransactionRepository walletRepository;
    private final AuditHelper auditHelper;

    // ---- 推荐码生成 ----

    /** 生成 6 位大写字母数字(数字+字母, 去 0/1/O/I 等易混字符) */
    public String genCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(CHARS.charAt(RAND.nextInt(CHARS.length())));
        return sb.toString();
    }

    /** 取会员的推荐码(若没有则生成并创建一条"自我推荐"占位) */
    @Transactional
    public String myReferralCode(Long memberId) {
        // 看是否已存在该 referrer 的 code
        List<Referral> list = referralRepository.findByReferrerIdAndDeletedFalseOrderByIdDesc(memberId);
        for (Referral r : list) {
            if (r.getCode() != null) return r.getCode();
        }
        // 创建一个"自我占位"记录, code 唯一
        String code;
        int tries = 0;
        do {
            code = genCode();
            tries++;
            if (tries > 50) throw new BizException(ErrorCode.SERVER_ERROR, "推荐码生成失败");
        } while (referralRepository.findByCodeAndDeletedFalse(code).isPresent());
        Referral r = new Referral();
        r.setReferrerId(memberId);
        r.setRefereeId(memberId); // 自占
        r.setRefereeName("(self)");
        r.setRefereePhone("(self)");
        r.setCode(code);
        r.setStatus("REGISTERED");
        r.setRewardAmount(0L);
        referralRepository.save(r);
        return code;
    }

    // ---- 关系绑定 ----

    /**
     * 当前会员(caller) 填写了被推荐人手机号 + 推荐码, 创建关系。
     * 简化: 假定 caller 就是 referee, referrer 通过 code 反查。
     * 但任务说明: refereeId 来自 caller, code 反查 referrer。
     * 这里: caller (memberId) 是 referee, 持码人是 referrer。
     */
    @Transactional
    public Map<String, Object> bind(Long memberId, String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "请填写推荐码");
        }
        // 已绑定则不可重复
        if (referralRepository.findByRefereeIdAndDeletedFalse(memberId).isPresent()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "您已绑定过推荐人");
        }
        // 反查推荐人
        Referral referrerRecord = referralRepository.findByCodeAndDeletedFalse(code.trim().toUpperCase())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "推荐码无效"));
        Long referrerId = referrerRecord.getReferrerId();
        if (referrerId.equals(memberId)) {
            throw new BizException(ErrorCode.BIZ_ERROR, "不可绑定自己");
        }
        Member me = memberRepository.findById(memberId)
                .filter(m -> Boolean.FALSE.equals(m.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        Member referrer = memberRepository.findByIdAndTenantIdAndDeletedFalse(referrerId, me.getTenantId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "推荐人不存在或非同租户"));
        // 创建关系
        Referral r = new Referral();
        r.setTenantId(me.getTenantId());
        r.setReferrerId(referrerId);
        r.setRefereeId(memberId);
        r.setRefereeName(me.getName());
        r.setRefereePhone(me.getPhone());
        r.setCode(code.trim().toUpperCase());
        r.setStatus("REGISTERED");
        r.setRewardAmount(0L);
        referralRepository.save(r);
        auditHelper.record("绑定推荐人", "member:" + memberId,
                "推荐码=" + code + ", 推荐人=" + (referrer == null ? "?" : referrer.getName()));
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("referrerId", referrerId);
        vo.put("referrerName", referrer == null ? null : referrer.getName());
        vo.put("code", code.trim().toUpperCase());
        return vo;
    }

    // ---- H5 端 ----

    /** H5 端: 我的推荐码 + 统计 + 二维码占位 */
    public Map<String, Object> myReferralInfo(Long memberId) {
        String code = myReferralCode(memberId);
        long total = referralRepository.countByReferrerIdAndDeletedFalse(memberId);
        long active = referralRepository.countByReferrerIdAndStatusAndDeletedFalse(memberId, "ACTIVE");
        long rewarded = referralRepository.countByReferrerIdAndStatusAndDeletedFalse(memberId, "REWARDED");
        long totalReward = referralRepository.sumReward(memberId) == null ? 0L : referralRepository.sumReward(memberId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("rewarded", rewarded);
        stats.put("totalReward", totalReward);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("code", code);
        vo.put("stats", stats);
        vo.put("qrUrl", "/api/h5/referral/qrcode?code=" + code);
        return vo;
    }

    /** H5 端: 列表(我推荐的所有人) */
    public List<Map<String, Object>> myReferralList(Long memberId) {
        List<Referral> list = referralRepository.findByReferrerIdAndDeletedFalseOrderByIdDesc(memberId);
        // 过滤掉 self
        return list.stream()
                .filter(r -> !r.getRefereeId().equals(r.getReferrerId()))
                .map(this::toVO)
                .toList();
    }

    // ---- Admin 端 ----

    public PageData<Map<String, Object>> adminAll(Long memberId, int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<Referral> p = referralRepository.search(tenantId, memberId, pageable);
        // 关联会员名
        List<Map<String, Object>> rows = p.getContent().stream().map(r -> {
            Map<String, Object> vo = toVO(r);
            memberRepository.findById(r.getReferrerId())
                    .filter(m -> Boolean.FALSE.equals(m.getDeleted()))
                    .ifPresent(m -> vo.put("referrerName", m.getName()));
            return vo;
        }).toList();
        return PageData.of(rows, p.getTotalElements(), page, size);
    }

    public List<Map<String, Object>> listByReferrer(Long memberId) {
        List<Referral> list = referralRepository.findByReferrerIdAndDeletedFalseOrderByIdDesc(memberId);
        return list.stream().map(this::toVO).toList();
    }

    public Map<String, Object> stats(Long memberId) {
        long total = referralRepository.countByReferrerIdAndDeletedFalse(memberId);
        long active = referralRepository.countByReferrerIdAndStatusAndDeletedFalse(memberId, "ACTIVE");
        long rewarded = referralRepository.countByReferrerIdAndStatusAndDeletedFalse(memberId, "REWARDED");
        long totalReward = referralRepository.sumReward(memberId) == null ? 0L : referralRepository.sumReward(memberId);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("code", myReferralCode(memberId));
        vo.put("totalReferrals", total);
        vo.put("activeReferrals", active);
        vo.put("rewardedReferrals", rewarded);
        vo.put("totalReward", totalReward);
        return vo;
    }

    /**
     * 被推荐人首次消费后由 OrderService.consume 触发的奖励发放。
     * 当前简化: 任务说明允许"在 referral.create 时就发", 我们保留方法签名供未来接入。
     */
    @Transactional
    public void rewardOnFirstConsume(Long refereeId) {
        Referral r = referralRepository.findByRefereeIdAndDeletedFalse(refereeId).orElse(null);
        if (r == null) return;
        if ("REWARDED".equals(r.getStatus())) return; // 已发放
        Long referrerId = r.getReferrerId();
        if (referrerId == null || referrerId.equals(refereeId)) return;
        Member referrer = memberRepository.findById(referrerId)
                .filter(m -> Boolean.FALSE.equals(m.getDeleted())).orElse(null);
        if (referrer == null) return;
        // 找一个 BIRTHDAY/EXPERIENCE 类型的券, 不限租户
        List<Coupon> coupons = couponRepository.listByTenant(referrer.getTenantId(), "ACTIVE", null);
        Coupon rewardCoupon = null;
        for (Coupon c : coupons) {
            if ("EXPERIENCE".equalsIgnoreCase(c.getType()) || "BIRTHDAY".equalsIgnoreCase(c.getType())) {
                rewardCoupon = c; break;
            }
        }
        long rewardFen = 0L;
        Long rewardId = null;
        String rewardType = "BALANCE";
        if (rewardCoupon != null) {
            // 直接写一条 CouponRecord
            CouponRecord cr = new CouponRecord();
            cr.setTenantId(referrer.getTenantId());
            cr.setCouponId(rewardCoupon.getId());
            cr.setCouponName(rewardCoupon.getName());
            cr.setMemberId(referrerId);
            cr.setMemberName(referrer.getName());
            cr.setCode("RF" + System.currentTimeMillis() % 1000000
                    + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase());
            cr.setStatus("UNUSED");
            cr.setGrantedAt(LocalDateTime.now());
            cr.setExpireAt(LocalDateTime.now().plusDays(30));
            couponRecordRepository.save(cr);
            rewardFen = rewardCoupon.getFaceValue() == null ? 0L : rewardCoupon.getFaceValue();
            rewardId = cr.getId();
            rewardType = "COUPON";
        } else {
            // 没券就送余额
            long before = referrer.getBalance() == null ? 0L : referrer.getBalance();
            referrer.setBalance(before + 500L);
            memberRepository.save(referrer);
            WalletTransaction tx = new WalletTransaction();
            tx.setTenantId(referrer.getTenantId());
            tx.setMemberId(referrerId);
            tx.setType("GIFT");
            tx.setAmount(500L);
            tx.setBalanceAfter(referrer.getBalance());
            tx.setRemark("推荐奖励: 新用户首单");
            walletRepository.save(tx);
            rewardFen = 500L;
            rewardId = tx.getId();
        }
        r.setStatus("REWARDED");
        r.setRewardAmount(rewardFen);
        r.setRewardType(rewardType);
        r.setRewardId(rewardId);
        referralRepository.save(r);
        auditHelper.record("推荐奖励发放", "referral:" + r.getId(),
                "推荐人=" + referrerId + ", 奖励=" + rewardFen + "分, 类型=" + rewardType);
    }

    // ---- 内部 ----

    public Map<String, Object> toVO(Referral r) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", r.getId());
        vo.put("referrerId", r.getReferrerId());
        vo.put("refereeId", r.getRefereeId());
        vo.put("refereeName", r.getRefereeName());
        vo.put("refereePhone", r.getRefereePhone());
        vo.put("code", r.getCode());
        vo.put("status", r.getStatus());
        vo.put("rewardAmount", r.getRewardAmount());
        vo.put("rewardType", r.getRewardType());
        vo.put("rewardId", r.getRewardId());
        vo.put("createdAt", r.getCreatedAt());
        return vo;
    }
}
