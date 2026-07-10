package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/** 全局储值流水接口 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * 全局流水查询: 返回分页 list 与汇总 summary。
     * start/end 格式 yyyy-MM-dd, end 按开区间处理(加一天)。
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDateTime startTime = parseDate(start, false);
        LocalDateTime endTime = parseDate(end, true);

        PageData<Map<String, Object>> pageData = walletService.transactions(
                keyword, type, storeId, memberId, startTime, endTime, page, size);
        Map<String, Object> summary = walletService.summary(
                keyword, type, storeId, memberId, startTime, endTime);

        // 组装 { list, total, page, size, summary }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", pageData.getList());
        data.put("total", pageData.getTotal());
        data.put("page", pageData.getPage());
        data.put("size", pageData.getSize());
        data.put("summary", summary);
        return Result.success(data);
    }

    /** 解析 yyyy-MM-dd; end 为 true 时加一天转为开区间上限 */
    private LocalDateTime parseDate(String s, boolean end) {
        if (s == null || s.isBlank()) return null;
        try {
            LocalDate d = LocalDate.parse(s.trim());
            return end ? d.plusDays(1).atStartOfDay() : d.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.VALIDATION, "日期格式错误");
        }
    }
}
