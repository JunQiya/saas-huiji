package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.entity.RechargeOrder;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.RechargeOrderRepository;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 储值充值订单: 先落单(create) -> 支付(pay) -> 入账。
 * 演示环境(recharge-demo-pay=true)由 pay 直接模拟支付成功;
 * 生产环境需接入真实微信支付(JSAPI 下单 + 回调 markPaidByNotify)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeService {

    private final RechargeOrderRepository rechargeOrderRepository;
    private final MemberRepository memberRepository;
    private final SettingsService settingsService;
    private final MemberService memberService;

    @Value("${huiji.h5.recharge-demo-pay:false}")
    private boolean rechargeDemoPay;

    /** 创建充值单(PENDING), 返回订单信息供支付 */
    @Transactional
    public Map<String, Object> create(Long tenantId, Long memberId, Long amount, String payMethod) {
        if (amount == null || amount <= 0) {
            throw new BizException(ErrorCode.VALIDATION, "充值金额必须大于 0");
        }
        if (amount > 5_000_000L) {
            throw new BizException(ErrorCode.VALIDATION, "单次充值不能超过 50000 元");
        }
        memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));

        long gift = settingsService.matchGift(tenantId, amount);
        RechargeOrder order = new RechargeOrder();
        order.setTenantId(tenantId);
        order.setMemberId(memberId);
        order.setAmount(amount);
        order.setGift(gift);
        order.setPayMethod(payMethod == null ? "WECHAT" : payMethod);
        order.setStatus("PENDING");
        order.setOutTradeNo(generateOutTradeNo());
        // 注意: BaseEntity 的 @Version 初始化为 0L 使 isNew 判定为 false, save 走 merge 分支,
        // 原对象不回填 id/createdAt, 必须使用返回值
        order = rechargeOrderRepository.save(order);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("rechargeOrderId", order.getId());
        vo.put("outTradeNo", order.getOutTradeNo());
        vo.put("amount", order.getAmount());
        vo.put("gift", order.getGift());
        vo.put("status", order.getStatus());
        vo.put("createdAt", order.getCreatedAt());
        return vo;
    }

    /**
     * 支付充值单:
     * 演示环境直接模拟支付成功并入账; 生产环境抛"请通过微信支付"由前端走真实 JSAPI 支付。
     */
    @Transactional
    public Map<String, Object> pay(Long tenantId, Long memberId, Long rechargeOrderId) {
        RechargeOrder order = rechargeOrderRepository.findByIdAndTenantIdAndDeletedFalse(rechargeOrderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "充值单不存在"));
        if (order.getMemberId() != null && !order.getMemberId().equals(memberId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "充值单不属于该会员");
        }
        if ("SUCCESS".equals(order.getStatus())) {
            return vo(order);
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "充值单状态不可支付: " + order.getStatus());
        }
        if (!rechargeDemoPay) {
            // 真实接入点: 此处应创建微信 JSAPI 预支付单并返回 payParams 供 wx.chooseWXPay 拉起
            throw new BizException(ErrorCode.BIZ_ERROR, "请通过微信支付完成付款");
        }
        // 演示模拟: 模拟微信支付回调成功
        return markSuccess(order, "MOCK" + System.nanoTime() % 100000000);
    }

    /** 微信支付回调入账(幂等): 已到账直接返回, 避免重复加余额 */
    @Transactional
    public Map<String, Object> markPaidByNotify(String outTradeNo, String transactionId) {
        RechargeOrder order = rechargeOrderRepository.findByOutTradeNoAndDeletedFalse(outTradeNo)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "充值单不存在: " + outTradeNo));
        if ("SUCCESS".equals(order.getStatus())) {
            log.info("充值回调重复, 跳过: outTradeNo={}", outTradeNo);
            return vo(order);
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "充值单状态不可支付: " + order.getStatus());
        }
        return markSuccess(order, transactionId);
    }

    /** 入账并标记成功 */
    private Map<String, Object> markSuccess(RechargeOrder order, String transactionId) {
        long after = memberService.creditRecharge(
                order.getTenantId(), order.getMemberId(),
                order.getAmount(), order.getGift(),
                order.getPayMethod(), "线上充值", LoginUserHolder.get() == null ? null : LoginUserHolder.get().getUserId());
        order.setStatus("SUCCESS");
        order.setTransactionId(transactionId);
        order.setBalanceAfter(after);
        order.setPaidAt(LocalDateTime.now());
        rechargeOrderRepository.save(order);
        return vo(order);
    }

    /** 定时关单: 关闭超过 minutes 未支付的充值单 */
    @Transactional
    public int cancelExpired(int minutes) {
        List<RechargeOrder> expired = rechargeOrderRepository
                .findByStatusAndCreatedAtBefore("PENDING", LocalDateTime.now().minusMinutes(minutes));
        int n = 0;
        for (RechargeOrder o : expired) {
            o.setStatus("CANCELLED");
            rechargeOrderRepository.save(o);
            n++;
        }
        if (n > 0) log.info("定时关单: 关闭 {} 笔超时充值单", n);
        return n;
    }

    private Map<String, Object> vo(RechargeOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rechargeOrderId", o.getId());
        m.put("outTradeNo", o.getOutTradeNo());
        m.put("amount", o.getAmount());
        m.put("gift", o.getGift());
        m.put("status", o.getStatus());
        m.put("balanceAfter", o.getBalanceAfter());
        m.put("transactionId", o.getTransactionId());
        m.put("paidAt", o.getPaidAt());
        return m;
    }

    /** 商户订单号: RC + 时间 + 随机, 保证唯一且可区分充值单 */
    private String generateOutTradeNo() {
        String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "RC" + ts + rand;
    }
}
