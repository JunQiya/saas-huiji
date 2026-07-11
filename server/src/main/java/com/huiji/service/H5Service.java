package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.H5Dto;
import com.huiji.entity.Coupon;
import com.huiji.entity.CouponRecord;
import com.huiji.entity.Member;
import com.huiji.entity.Store;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.CouponRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.MemberTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** H5 会员端服务: 手机号+验证码登录、个人资料、余额、券、领券、消费记录、门店。 */
@Service
@RequiredArgsConstructor
public class H5Service {

    @Value("${huiji.h5.sms-code:8888}")
    private String smsCode;

    private final MemberRepository memberRepository;
    private final WalletTransactionRepository walletRepository;
    private final CouponRepository couponRepository;
    private final CouponRecordRepository couponRecordRepository;
    private final StoreRepository storeRepository;
    private final CouponService couponService;
    private final MemberService memberService;
    private final SettingsService settingsService;
    private final MemberTokenUtil memberTokenUtil;
    private final SmsCodeService smsCodeService;

    /** 登录: 校验验证码, 返回 memberToken 与会员资料 */
    @Transactional
    public Map<String, Object> login(H5Dto.LoginRequest req) {
        String input = req.getCode();
        boolean ok = false;
        // 优先走真实验证码通道
        if (smsCodeService.verifyAndConsume(req.getPhone(), input)) {
            ok = true;
        } else if (smsCode != null && !smsCode.isBlank() && smsCode.equals(input)) {
            // 兜底: 配置文件中的固定码 (仅 dev 演示)
            ok = true;
        }
        if (!ok) {
            throw new BizException(ErrorCode.BIZ_ERROR, "验证码错误或已过期");
        }
        // 按手机号查找会员(演示单租户, 取首个匹配)
        Member m = memberRepository.findFirstByPhoneAndDeletedFalseOrderByIdAsc(req.getPhone())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在, 请先到门店登记"));
        m.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(m);
        String token = memberTokenUtil.generate(m.getId(), m.getTenantId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("memberToken", token);
        resp.put("member", memberService.toVO(m, true));
        return resp;
    }

    /** 会员卡面: 含等级/余额/积分 */
    public Map<String, Object> profile(Long memberId, Long tenantId) {
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        Map<String, Object> vo = memberService.toVO(m, true);
        vo.put("levelName", settingsService.levelName(tenantId, m.getLevel()));
        return vo;
    }

    /** 储值余额 + 近 5 笔流水 */
    public Map<String, Object> balance(Long memberId, Long tenantId) {
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        List<WalletTransaction> recent = walletRepository.findTop5ByTenantIdAndMemberIdOrderByIdDesc(tenantId, memberId);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("balance", m.getBalance());
        vo.put("points", m.getPoints());
        vo.put("recent", recent.stream().map(this::txVO).collect(Collectors.toList()));
        return vo;
    }

    /** 我的券 */
    public List<Map<String, Object>> coupons(Long memberId, Long tenantId, String status) {
        List<CouponRecord> records = couponRecordRepository.findByMember(tenantId, memberId, status);
        return records.stream().map(this::recordVO).collect(Collectors.toList());
    }

    /** 可领取的券 */
    public List<Map<String, Object>> available(Long memberId, Long tenantId) {
        return couponRepository.listClaimable(tenantId).stream()
                .map(couponService::toVO).collect(Collectors.toList());
    }

    /** 领券 */
    @Transactional
    public Map<String, Object> claim(Long memberId, Long tenantId, Long couponId) {
        return couponService.claim(memberId, couponId);
    }

    /** 消费记录 */
    public List<Map<String, Object>> transactions(Long memberId, Long tenantId, String type) {
        List<WalletTransaction> all = walletRepository.findByMember(tenantId, memberId, type,
                PageRequest.of(0, 50)).getContent();
        return all.stream().map(this::txVO).collect(Collectors.toList());
    }

    /** 附近门店 */
    public List<Map<String, Object>> stores(Long tenantId) {
        return storeRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId).stream()
                .map(this::storeVO).collect(Collectors.toList());
    }

    private Map<String, Object> txVO(WalletTransaction t) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", t.getId());
        vo.put("type", t.getType());
        vo.put("amount", t.getAmount());
        vo.put("balanceAfter", t.getBalanceAfter());
        vo.put("remark", t.getRemark());
        vo.put("createdAt", t.getCreatedAt());
        return vo;
    }

    private Map<String, Object> recordVO(CouponRecord r) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", r.getId());
        vo.put("couponName", r.getCouponName());
        vo.put("code", r.getCode());
        vo.put("status", r.getStatus());
        vo.put("grantedAt", r.getGrantedAt());
        vo.put("usedAt", r.getUsedAt());
        vo.put("expireAt", r.getExpireAt());
        return vo;
    }

    private Map<String, Object> storeVO(Store s) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", s.getId());
        vo.put("name", s.getName());
        vo.put("address", s.getAddress());
        vo.put("phone", s.getPhone());
        vo.put("businessHours", s.getBusinessHours());
        vo.put("status", s.getStatus());
        return vo;
    }
}
