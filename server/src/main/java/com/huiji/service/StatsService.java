package com.huiji.service;

import com.huiji.entity.Member;
import com.huiji.entity.Store;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** 数据看板服务: 概览(含环比)、趋势、会员增长、热门服务、RFM 分层、小时分布。 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final MemberRepository memberRepository;
    private final WalletTransactionRepository walletRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;

    /** 当前门店 ID(null 表示不按门店过滤, 返回全租户数据) */
    private Long currentStoreId() {
        return LoginUserHolder.requireStoreId();
    }

    /** 已支付订单统计 [笔数, 金额分]: 营业额口径为已支付订单, 而非储值消费流水 */
    private long[] paidStats(Long tenantId, LocalDateTime start, LocalDateTime end, Long storeId) {
        List<Object[]> rows = orderRepository.todayStats(tenantId, start, end, storeId);
        if (rows == null || rows.isEmpty() || rows.get(0) == null) return new long[]{0, 0};
        Object[] r = rows.get(0);
        long count = r[0] == null ? 0 : ((Number) r[0]).longValue();
        long sum = r[1] == null ? 0 : ((Number) r[1]).longValue();
        return new long[]{count, sum};
    }

    /** 概览: 近 30 天 vs 上 30 天环比 */
    public Map<String, Object> overview() {
        Long tenantId = LoginUserHolder.currentTenantId();
        Long storeId = currentStoreId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curStart = now.minusDays(30);
        LocalDateTime prevStart = now.minusDays(60);

        long curRevenue = paidStats(tenantId, curStart, now, storeId)[1];
        long prevRevenue = paidStats(tenantId, prevStart, curStart, storeId)[1];
        long curOrders = paidStats(tenantId, curStart, now, storeId)[0];
        long prevOrders = paidStats(tenantId, prevStart, curStart, storeId)[0];

        long memberCount = memberRepository.countByTenantIdAndDeletedFalse(tenantId);
        long newMembers = memberRepository.countNewAfter(tenantId, curStart);
        long prevNewMembers = memberRepository.countNewAfter(tenantId, prevStart) - newMembers;

        long avgPrice = curOrders > 0 ? curRevenue / curOrders : 0;
        long prevAvgPrice = prevOrders > 0 ? prevRevenue / prevOrders : 0;

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("revenue", curRevenue);
        vo.put("revenueDelta", deltaPct(curRevenue, prevRevenue));
        vo.put("memberCount", memberCount);
        vo.put("memberDelta", deltaPct(newMembers, Math.max(0, prevNewMembers)));
        vo.put("orderCount", curOrders);
        vo.put("orderDelta", deltaPct(curOrders, prevOrders));
        vo.put("avgPrice", avgPrice);
        vo.put("avgPriceDelta", deltaPct(avgPrice, prevAvgPrice));
        return vo;
    }

    /** 趋势: 7d/30d/90d, metric revenue/orders/members */
    public List<Map<String, Object>> trend(String range, String metric) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Long storeId = currentStoreId();
        int days = "7d".equals(range) ? 7 : "90d".equals(range) ? 90 : 30;
        LocalDate today = LocalDate.now();
        List<Member> members = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            long value;
            switch (metric == null ? "revenue" : metric) {
                case "orders":
                    value = paidStats(tenantId, dayStart, dayEnd, storeId)[0];
                    break;
                case "members":
                    value = members.stream().filter(m -> m.getCreatedAt() != null
                            && m.getCreatedAt().isAfter(dayStart) && m.getCreatedAt().isBefore(dayEnd)).count();
                    break;
                default:
                    value = paidStats(tenantId, dayStart, dayEnd, storeId)[1];
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("value", value);
            result.add(row);
        }
        return result;
    }

    /** 会员增长: 近 30 天每日新增与活跃 */
    public List<Map<String, Object>> memberGrowth() {
        Long tenantId = LoginUserHolder.currentTenantId();
        LocalDate today = LocalDate.now();
        List<Member> members = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            long newCount = members.stream().filter(m -> m.getCreatedAt() != null
                    && m.getCreatedAt().isAfter(dayStart) && m.getCreatedAt().isBefore(dayEnd)).count();
            long activeCount = members.stream().filter(m -> m.getLastConsumeAt() != null
                    && m.getLastConsumeAt().isAfter(dayStart) && m.getLastConsumeAt().isBefore(dayEnd)).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("newCount", newCount);
            row.put("activeCount", activeCount);
            result.add(row);
        }
        return result;
    }

    /** 门店营收排行: 按近 N 天消费聚合 [storeId, amount, count], 门店名关联后返回 */
    public List<Map<String, Object>> storeRanking(int days) {
        Long tenantId = LoginUserHolder.currentTenantId();
        LocalDateTime start = LocalDateTime.now().minusDays(Math.max(1, days));
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<Object[]> rows = orderRepository.paidSumByStore(tenantId, start, end);
        Map<Long, String> storeNames = storeRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName, (a, b) -> a));
        return rows.stream().limit(10).map(r -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            Long sid = ((Number) r[0]).longValue();
            vo.put("storeId", sid);
            vo.put("storeName", storeNames.getOrDefault(sid, "门店#" + sid));
            vo.put("amount", r[1] == null ? 0L : ((Number) r[1]).longValue());
            vo.put("count", r[2] == null ? 0L : ((Number) r[2]).longValue());
            return vo;
        }).collect(Collectors.toList());
    }

    /** 热门服务 Top10: 按消费流水的服务项(remark)聚合 */
    public List<Map<String, Object>> topServices() {        Long tenantId = LoginUserHolder.currentTenantId();
        Long storeId = currentStoreId();
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        List<WalletTransaction> txs = walletRepository.consumeInRange(tenantId, start, LocalDateTime.now(), storeId);
        // 按服务项聚合
        Map<String, long[]> agg = new LinkedHashMap<>();
        for (WalletTransaction t : txs) {
            String name = (t.getRemark() == null || t.getRemark().isBlank()) ? "其他" : t.getRemark();
            long[] cur = agg.computeIfAbsent(name, k -> new long[2]);
            cur[0] += 1;                 // count
            cur[1] += Math.abs(t.getAmount()); // amount
        }
        return agg.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]))
                .limit(10)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", e.getKey());
                    row.put("count", e.getValue()[0]);
                    row.put("amount", e.getValue()[1]);
                    return row;
                })
                .collect(Collectors.toList());
    }

    /** RFM 分层: high 近30天有消费且累计高 / mid 近90天有消费 / low 90天以上 / dormant 无消费 */
    public Map<String, Object> rfm() {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<Member> members = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime d30 = now.minusDays(30);
        LocalDateTime d90 = now.minusDays(90);
        long high = 0, mid = 0, low = 0, dormant = 0;
        for (Member m : members) {
            long total = m.getTotalAmount() == null ? 0 : m.getTotalAmount();
            LocalDateTime last = m.getLastConsumeAt();
            if (last == null) {
                dormant++;
            } else if (last.isAfter(d30) && total >= 200000) {
                high++;
            } else if (last.isAfter(d90)) {
                mid++;
            } else {
                low++;
            }
        }
        long total = members.size();
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("high", layer(high, total));
        vo.put("mid", layer(mid, total));
        vo.put("low", layer(low, total));
        vo.put("dormant", layer(dormant, total));
        return vo;
    }

    private Map<String, Object> layer(long count, long total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", count);
        m.put("ratio", total > 0 ? Math.round(count * 10000.0 / total) / 100.0 : 0);
        return m;
    }

    /** 24 小时下单分布 */
    public List<Map<String, Object>> hour() {
        Long tenantId = LoginUserHolder.currentTenantId();
        Long storeId = currentStoreId();
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = orderRepository.paidCountByHour(tenantId, start, storeId);
        Map<Integer, Long> map = new TreeMap<>();
        for (Object[] row : rows) {
            int h = ((Number) row[0]).intValue();
            long c = ((Number) row[1]).longValue();
            map.put(h, c);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", h);
            row.put("count", map.getOrDefault(h, 0L));
            result.add(row);
        }
        return result;
    }


    /** 经营摘要: 今日/本周/本月营业额 + 环比 + 订单/会员/活跃 */
    public Map<String, Object> summary() {
        Long tenantId = LoginUserHolder.currentTenantId();
        Long storeId = currentStoreId();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        // 今日
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = todayStart;
        long[] t = paidStats(tenantId, todayStart, todayEnd, storeId);
        long[] y = paidStats(tenantId, yesterdayStart, yesterdayEnd, storeId);
        long todayRevenue = t[1];
        long yesterdayRevenue = y[1];
        long todayOrders = t[0];
        long newMembersToday = memberRepository.countNewAfter(tenantId, todayStart);
        long activeMembersToday = memberRepository.countActiveAfter(tenantId, todayStart);

        // 本周(周一为起点)
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt = today.plusDays(1).atStartOfDay();
        LocalDate lastWeekStart = weekStart.minusDays(7);
        LocalDateTime lastWeekStartDt = lastWeekStart.atStartOfDay();
        LocalDateTime lastWeekEndDt = weekStartDt;
        long weekRevenue = paidStats(tenantId, weekStartDt, weekEndDt, storeId)[1];
        long lastWeekRevenue = paidStats(tenantId, lastWeekStartDt, lastWeekEndDt, storeId)[1];
        long weekOrders = paidStats(tenantId, weekStartDt, weekEndDt, storeId)[0];
        long newMembersWeek = memberRepository.countNewAfter(tenantId, weekStartDt) - newMembersToday;

        // 本月
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDt = monthStart.atStartOfDay();
        LocalDateTime monthEndDt = today.plusDays(1).atStartOfDay();
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);
        LocalDateTime lastMonthStartDt = lastMonthStart.atStartOfDay();
        LocalDateTime lastMonthEndDt = lastMonthEnd.plusDays(1).atStartOfDay();
        long monthRevenue = paidStats(tenantId, monthStartDt, monthEndDt, storeId)[1];
        long lastMonthRevenue = paidStats(tenantId, lastMonthStartDt, lastMonthEndDt, storeId)[1];
        long monthOrders = paidStats(tenantId, monthStartDt, monthEndDt, storeId)[0];
        long newMembersMonth = memberRepository.countNewAfter(tenantId, monthStartDt);
        long monthRecharge = nvl(walletRepository.sumRecharge(tenantId, monthStartDt, monthEndDt));
        long lastMonthRecharge = nvl(walletRepository.sumRecharge(tenantId, lastMonthStartDt, lastMonthEndDt));

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("todayRevenue", todayRevenue);
        vo.put("todayDelta", deltaPct(todayRevenue, yesterdayRevenue));
        vo.put("weekRevenue", weekRevenue);
        vo.put("weekDelta", deltaPct(weekRevenue, lastWeekRevenue));
        vo.put("monthRevenue", monthRevenue);
        vo.put("monthDelta", deltaPct(monthRevenue, lastMonthRevenue));
        vo.put("monthRecharge", monthRecharge);
        vo.put("monthRechargeDelta", deltaPct(monthRecharge, lastMonthRecharge));
        vo.put("todayOrders", todayOrders);
        vo.put("weekOrders", weekOrders);
        vo.put("monthOrders", monthOrders);
        vo.put("newMembersToday", newMembersToday);
        vo.put("newMembersWeek", newMembersWeek);
        vo.put("newMembersMonth", newMembersMonth);
        vo.put("consumeMembersToday", activeMembersToday);
        return vo;
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    /** 环比百分比(保留 1 位) */
    private double deltaPct(long cur, long prev) {
        if (prev == 0) return cur == 0 ? 0 : 100.0;
        return Math.round((cur - prev) * 1000.0 / prev) / 10.0;
    }
}
