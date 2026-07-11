package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.H5Dto;
import com.huiji.entity.Campaign;
import com.huiji.repository.CampaignRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
    private final CampaignRepository campaignRepository;

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
    public Result<List<Map<String, Object>>> transactions(HttpServletRequest req,
                                                         @RequestParam(required = false) String type) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.transactions(ctx[0], ctx[1], type));
    }

    @GetMapping("/stores")
    public Result<List<Map<String, Object>>> stores(HttpServletRequest req) {
        Long tenantId = tryTenantId(req);
        return Result.success(h5Service.stores(tenantId));
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

    /** 活动详情(公开, 无需 member token) */
    @GetMapping("/campaigns/{id}")
    public Result<Map<String, Object>> campaignDetail(@PathVariable Long id) {
        Campaign c = campaignRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("name", c.getName());
        vo.put("tag", c.getType());
        vo.put("subtitle", c.getTrigger());
        vo.put("rules", c.getContent());
        vo.put("startTime", c.getStartAt());
        vo.put("endTime", c.getEndAt());
        vo.put("timeText", buildCampaignTimeText(c.getStartAt(), c.getEndAt()));
        vo.put("couponId", null);
        vo.put("link", null);
        vo.put("status", Boolean.TRUE.equals(c.getEnabled()) ? "ENABLED" : "DISABLED");
        return Result.success(vo);
    }

    private String buildCampaignTimeText(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String s = start == null ? "" : start.format(fmt);
        String e = end == null ? "" : end.format(fmt);
        if (s.isEmpty() && e.isEmpty()) return "";
        if (e.isEmpty()) return s + " 起";
        if (s.isEmpty()) return "截至 " + e;
        return s + " ~ " + e;
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

    /** 尝试从 memberToken 解析 tenantId，失败则返回默认 1L（用于公开接口） */
    private Long tryTenantId(HttpServletRequest req) {
        try {
            long[] ctx = currentMember(req);
            return ctx[1];
        } catch (Exception e) {
            return 1L;
        }
    }
}
