package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.H5Dto;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.H5Service;
import com.huiji.service.OrderService;
import com.huiji.service.ProductService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** H5 会员端接口(公开, 凭 memberToken) */
@RestController
@RequestMapping("/api/h5")
@RequiredArgsConstructor
public class H5Controller {

    private final H5Service h5Service;
    private final MemberTokenUtil memberTokenUtil;
    private final OrderService orderService;
    private final ProductService productService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody H5Dto.LoginRequest req) {
        return Result.success(h5Service.login(req));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.profile(ctx[0], ctx[1]));
    }

    @GetMapping("/balance")
    public Result<Map<String, Object>> balance(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.balance(ctx[0], ctx[1]));
    }

    @GetMapping("/coupons")
    public Result<List<Map<String, Object>>> coupons(HttpServletRequest req,
                                                     @RequestParam(required = false) String status) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.coupons(ctx[0], ctx[1], status));
    }

    @GetMapping("/coupons/available")
    public Result<List<Map<String, Object>>> available(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.available(ctx[0], ctx[1]));
    }

    @PostMapping("/coupons/{id}/claim")
    public Result<Map<String, Object>> claim(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.claim(ctx[0], ctx[1], id));
    }

    @GetMapping("/transactions")
    public Result<List<Map<String, Object>>> transactions(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.transactions(ctx[0], ctx[1]));
    }

    @GetMapping("/stores")
    public Result<List<Map<String, Object>>> stores(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.stores(ctx[1]));
    }

    // ============ 新增: 我的订单 ============

    @GetMapping("/orders")
    public Result<PageData<Map<String, Object>>> orders(HttpServletRequest req,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(orderService.listByMember(ctx[0], status, page, size));
        } finally {
            LoginUserHolder.clear();
        }
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> orderDetail(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(orderService.memberDetail(id, ctx[0]));
        } finally {
            LoginUserHolder.clear();
        }
    }

    /** 会员端浏览商品(无分页, 全部 ACTIVE) */
    @GetMapping("/products/active")
    public Result<List<Map<String, Object>>> products(HttpServletRequest req,
                                                     @RequestParam(required = false) String category) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(productService.listActive(category));
        } finally {
            LoginUserHolder.clear();
        }
    }

    private void bindAsMember(long memberId, long tenantId) {
        LoginUser lu = LoginUser.builder()
                .userId(memberId)
                .tenantId(tenantId)
                .username(String.valueOf(memberId))
                .role("MEMBER")
                .build();
        LoginUserHolder.set(lu);
    }

    /** 解析 memberToken, 返回 [memberId, tenantId] */
    private long[] currentMember(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "请先登录");
        }
        String token = header.substring(7);
        try {
            Claims claims = memberTokenUtil.parse(token);
            if (!"MEMBER".equals(claims.get("type", String.class))) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            Long memberId = claims.get("memberId", Long.class);
            Long tenantId = claims.get("tenantId", Long.class);
            if (memberId == null) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            return new long[]{memberId, tenantId == null ? 1L : tenantId};
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "登录已过期");
        }
    }
}
