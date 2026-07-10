package com.huiji.service;

import com.huiji.common.PageData;
import com.huiji.entity.Member;
import com.huiji.entity.Store;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 储值流水服务: 全局流水查询与汇总, 支持按会员关键字/类型/门店/时间筛选。 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletTransactionRepository walletRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    /**
     * 全局流水分页查询。
     * keyword 非空时先匹配会员姓名/手机, 转为 memberIds; 无匹配则返回空分页。
     */
    public PageData<Map<String, Object>> transactions(String keyword, String type, Long storeId, Long memberId,
                                                       LocalDateTime start, LocalDateTime end, int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<Long> memberIds = resolveMemberIds(tenantId, keyword);
        // keyword 非空但无匹配会员, 直接返回空分页
        if (keyword != null && keyword.isBlank() == false && (memberIds == null || memberIds.isEmpty())) {
            return PageData.of(Collections.emptyList(), 0, page, size);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<WalletTransaction> p = walletRepository.searchGlobal(
                tenantId, trim(type), storeId, memberId, start, end, memberIds, pageable);

        List<WalletTransaction> txs = p.getContent();
        // 批量 join 会员名与门店名
        Map<Long, String> memberNameMap = loadNames(txs, WalletTransaction::getMemberId,
                ids -> memberRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Member::getId, Member::getName)));
        Map<Long, String> storeNameMap = loadNames(txs, WalletTransaction::getStoreId,
                ids -> storeRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Store::getId, Store::getName)));

        List<Map<String, Object>> list = txs.stream().map(t -> txVO(t, memberNameMap, storeNameMap))
                .collect(Collectors.toList());
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    /** 全局流水汇总: { recharge, consume, gift, refund, total } */
    public Map<String, Object> summary(String keyword, String type, Long storeId, Long memberId,
                                       LocalDateTime start, LocalDateTime end) {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<Long> memberIds = resolveMemberIds(tenantId, keyword);
        if (keyword != null && keyword.isBlank() == false && (memberIds == null || memberIds.isEmpty())) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("recharge", 0L);
            empty.put("consume", 0L);
            empty.put("gift", 0L);
            empty.put("refund", 0L);
            empty.put("total", 0L);
            return empty;
        }
        List<Object[]> rows = walletRepository.summaryGlobal(
                tenantId, trim(type), storeId, memberId, start, end, memberIds);

        long recharge = 0, consume = 0, gift = 0, refund = 0, total = 0;
        for (Object[] row : rows) {
            String t = String.valueOf(row[0]);
            long sum = row[1] == null ? 0L : ((Number) row[1]).longValue();
            long cnt = row[2] == null ? 0L : ((Number) row[2]).longValue();
            switch (t) {
                case "RECHARGE" -> recharge += sum;
                case "CONSUME" -> consume += sum;
                case "GIFT" -> gift += sum;
                case "REFUND" -> refund += sum;
                default -> { /* 忽略未知类型 */ }
            }
            total += cnt;
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("recharge", recharge);
        vo.put("consume", consume);
        vo.put("gift", gift);
        vo.put("refund", refund);
        vo.put("total", total);
        return vo;
    }

    // ---- 内部方法 ----

    /** keyword 非空时按姓名/手机匹配会员 id; keyword 为空返回 null(不限制) */
    private List<Long> resolveMemberIds(Long tenantId, String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return memberRepository.findIdsByKeyword(tenantId, keyword.trim());
    }

    /** 收集非空 id 并批量查询, 返回 id→name 映射 */
    private Map<Long, String> loadNames(List<WalletTransaction> txs, Function<WalletTransaction, Long> idGetter,
                                        Function<java.util.Set<Long>, Map<Long, String>> loader) {
        Set<Long> ids = txs.stream().map(idGetter).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return new HashMap<>();
        return loader.apply(ids);
    }

    private Map<String, Object> txVO(WalletTransaction t, Map<Long, String> memberNameMap, Map<Long, String> storeNameMap) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", t.getId());
        vo.put("type", t.getType());
        vo.put("amount", t.getAmount());
        vo.put("gift", t.getGift());
        vo.put("balanceAfter", t.getBalanceAfter());
        vo.put("storeId", t.getStoreId());
        vo.put("storeName", t.getStoreId() == null ? null : storeNameMap.get(t.getStoreId()));
        vo.put("memberId", t.getMemberId());
        vo.put("memberName", memberNameMap.get(t.getMemberId()));
        vo.put("payMethod", t.getPayMethod());
        vo.put("orderNo", t.getOrderNo());
        vo.put("remark", t.getRemark());
        vo.put("createdAt", t.getCreatedAt());
        return vo;
    }

    private String trim(String s) {
        return s == null ? null : s.trim().isEmpty() ? null : s.trim();
    }
}
