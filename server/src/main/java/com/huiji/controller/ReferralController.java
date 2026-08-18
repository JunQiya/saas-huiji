package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.service.ReferralService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 推荐裂变接口(后台) */
@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long memberId) {
        return Result.success(referralService.listByReferrer(memberId));
    }

    @GetMapping("/admin/all")
    public Result<PageData<Map<String, Object>>> adminAll(
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(referralService.adminAll(memberId, page, size));
    }

    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> stats(@RequestParam Long memberId) {
        return Result.success(referralService.stats(memberId));
    }

    /** 全局裂变汇总(全租户) */
    @GetMapping("/admin/summary")
    public Result<Map<String, Object>> globalSummary() {
        return Result.success(referralService.globalSummary());
    }

    @Data
    public static class BindRequest {
        private Long memberId;
        private String code;
    }

    /**
     * 后台手工代绑定: 由管理员指定要绑定的会员 ID 和推荐码。
     */
    @PostMapping("/admin/bind")
    public Result<Map<String, Object>> adminBind(@RequestBody BindRequest req) {
        if (req.getMemberId() == null) {
            throw new BizException(ErrorCode.VALIDATION, "请指定会员");
        }
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "请输入推荐码");
        }
        return Result.success(referralService.bind(req.getMemberId(), req.getCode()));
    }
}
