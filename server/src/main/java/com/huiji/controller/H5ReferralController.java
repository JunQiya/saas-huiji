package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.dto.H5Dto;
import com.huiji.security.MemberHolder;
import com.huiji.service.ReferralService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * H5 端推荐裂变接口(独立成 controller, 避免修改 H5Controller)。
 */
@RestController
@RequestMapping("/api/h5/referral")
@RequiredArgsConstructor
public class H5ReferralController {

    private final ReferralService referralService;

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Long memberId = requireMember();
        return Result.success(referralService.myReferralInfo(memberId));
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long memberId = requireMember();
        return Result.success(referralService.myReferralList(memberId));
    }

    @Data
    public static class BindReq {
        private String code;
    }

    @PostMapping("/bind")
    public Result<Map<String, Object>> bind(@RequestBody BindReq req) {
        Long memberId = requireMember();
        return Result.success(referralService.bind(memberId, req == null ? null : req.getCode()));
    }

    private Long requireMember() {
        Long memberId = MemberHolder.getOrNull();
        if (memberId == null) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "请先登录");
        }
        return memberId;
    }
}
