package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.repository.OrderItemRepository;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.OrderService;
import com.huiji.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 经营统计概览(看板/报表) */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) String range) {
        return Result.success(statsService.overview());
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) String range) {
        return Result.success(statsService.summary());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(required = false) String range,
                                                   @RequestParam(required = false) String metric) {
        return Result.success(statsService.trend(range == null ? "month" : range, metric));
    }

    @GetMapping("/member-growth")
    public Result<List<Map<String, Object>>> memberGrowth(@RequestParam(required = false) String range) {
        return Result.success(statsService.memberGrowth());
    }

    @GetMapping("/top-services")
    public Result<List<Map<String, Object>>> topServices(@RequestParam(required = false) String range,
                                                         @RequestParam(defaultValue = "10") int limit) {
        return Result.success(statsService.topServices());
    }

    @GetMapping("/rfm")
    public Result<Map<String, Object>> rfm() {
        return Result.success(statsService.rfm());
    }

    @GetMapping("/hour")
    public Result<List<Map<String, Object>>> hour() {
        return Result.success(statsService.hour());
    }

    // ============ 新增: 今日订单统计 ============

    @GetMapping("/orders/today")
    public Result<Map<String, Object>> ordersToday() {
        return Result.success(orderService.todayStats());
    }

    /** 商品 Top N(按销量排序, 限定时间范围) */
    @GetMapping("/products/top")
    public Result<List<Map<String, Object>>> topProducts(@RequestParam(defaultValue = "10") int limit,
                                                         @RequestParam(required = false) String start,
                                                         @RequestParam(required = false) String end) {
        Long tenantId = LoginUserHolder.currentTenantId();
        LocalDateTime s = parseTime(start, false);
        LocalDateTime e = parseTime(end, true);
        List<Object[]> rows = orderItemRepository.topProductsInRange(tenantId, s, e,
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("productId", r[0]);
            vo.put("productName", r[1]);
            vo.put("quantity", r[2] == null ? 0L : ((Number) r[2]).longValue());
            vo.put("subtotal", r[3] == null ? 0L : ((Number) r[3]).longValue());
            out.add(vo);
        }
        return Result.success(out);
    }

    private LocalDateTime parseTime(String s, boolean isEnd) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return isEnd ? d.plusDays(1).atStartOfDay() : d.atStartOfDay();
            }
            return LocalDateTime.parse(s);
        } catch (Exception ex) {
            return null;
        }
    }
}
