package com.huiji.service;

import com.huiji.entity.Member;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.MemberRepository;
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

    /** 概览: 近 30 天 vs 上 30 天环比 */
    public Map<String, Object> overview() {
        Long tenantId = LoginUserHolder.currentTenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curStart = now.minusDays(30);
        LocalDateTime prevStart = now.minusDays(60);

        long curRevenue = nvl(walletRepository.sumConsume(tenantId, curStart, now));
        long prevRevenue = nvl(walletRepository.sumConsume(tenantId, prevStart, curStart));
        long curOrders = nvl(walletRepository.countConsume(tenantId, curStart, now));
        long prevOrders = nvl(walletRepository.countConsume(tenantId, prevStart, curStart));

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
                    value = nvl(walletRepository.countConsume(tenantId, dayStart, dayEnd));
                    break;
                case "members":
                    value = members.stream().filter(m -> m.getCreatedAt() != null
                            && m.getCreatedAt().isAfter(dayStart) && m.getCreatedAt().isBefore(dayEnd)).count();
                    break;
                default:
                    value = nvl(walletRepository.sumConsume(tenantId, dayStart, dayEnd));
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

    /** 热门服务 Top10: 按消费流水的服务项(remark)聚合 */
    public List<Map<String, Object>> topServices() {
        Long tenantId = LoginUserHolder.currentTenantId();
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        List<WalletTransaction> txs = walletRepository.consumeInRange(tenantId, start, LocalDateTime.now());
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
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = walletRepository.countByHour(tenantId, start);
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
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        // 今日
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = todayStart;
        long todayRevenue = nvl(walletRepository.sumConsume(tenantId, todayStart, todayEnd));
        long yesterdayRevenue = nvl(walletRepository.sumConsume(tenantId, yesterdayStart, yesterdayEnd));
        long todayOrders = nvl(walletRepository.countConsume(tenantId, todayStart, todayEnd));
        long newMembersToday = memberRepository.countNewAfter(tenantId, todayStart);
        long activeMembersToday = memberRepository.countActiveAfter(tenantId, todayStart);

        // 本周(周一为起点)
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt = today.plusDays(1).atStartOfDay();
        LocalDate lastWeekStart = weekStart.minusDays(7);
        LocalDateTime lastWeekStartDt = lastWeekStart.atStartOfDay();
        LocalDateTime lastWeekEndDt = weekStartDt;
        long weekRevenue = nvl(walletRepository.sumConsume(tenantId, weekStartDt, weekEndDt));
        long lastWeekRevenue = nvl(walletRepository.sumConsume(tenantId, lastWeekStartDt, lastWeekEndDt));
        long weekOrders = nvl(walletRepository.countConsume(tenantId, weekStartDt, weekEndDt));
        long newMembersWeek = memberRepository.countNewAfter(tenantId, weekStartDt) - newMembersToday;

        // 本月
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDt = monthStart.atStartOfDay();
        LocalDateTime monthEndDt = today.plusDays(1).atStartOfDay();
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);
        LocalDateTime lastMonthStartDt = lastMonthStart.atStartOfDay();
        LocalDateTime lastMonthEndDt = lastMonthEnd.plusDays(1).atStartOfDay();
        long monthRevenue = nvl(walletRepository.sumConsume(tenantId, monthStartDt, monthEndDt));
        long lastMonthRevenue = nvl(walletRepository.sumConsume(tenantId, lastMonthStartDt, lastMonthEndDt));
        long monthOrders = nvl(walletRepository.countConsume(tenantId, monthStartDt, monthEndDt));
        long newMembersMonth = memberRepository.countNewAfter(tenantId, monthStartDt);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("todayRevenue", todayRevenue);
        vo.put("todayDelta", deltaPct(todayRevenue, yesterdayRevenue));
        vo.put("weekRevenue", weekRevenue);
        vo.put("weekDelta", deltaPct(weekRevenue, lastWeekRevenue));
        vo.put("monthRevenue", monthRevenue);
        vo.put("monthDelta", deltaPct(monthRevenue, lastMonthRevenue));
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
