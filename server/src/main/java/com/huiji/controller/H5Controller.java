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
import com.huiji.security.MemberContext;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.H5Service;
import com.huiji.service.MallService;
import com.huiji.service.MemberService;
import com.huiji.service.OrderService;
import com.huiji.service.ProductService;
import com.huiji.service.SmsCodeService;
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
import java.util.stream.Collectors;

/** H5 会员端接口(公开, 凭 memberToken) */
@RestController
@RequestMapping("/api/h5")
@RequiredArgsConstructor
public class H5Controller {

    private final H5Service h5Service;
    private final MemberTokenUtil memberTokenUtil;
    private final OrderService orderService;
    private final ProductService productService;
    private final MallService mallService;
    private final MemberService memberService;
    private final CampaignRepository campaignRepository;
    private final SmsCodeService smsCodeService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody H5Dto.LoginRequest req) {
        return Result.success(h5Service.login(req));
    }

    /**
     * 发送登录短信验证码（公开接口）。
     * dev 模式下响应中回显验证码，方便前端调试；生产模式只返回成功标志。
     */
    @PostMapping("/sms/send")
    public Result<Map<String, Object>> sendSmsCode(@RequestBody Map<String, String> body) {
        String phone = body == null ? null : body.get("phone");
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BizException(ErrorCode.VALIDATION, "请输入正确的手机号");
        }
        String code = smsCodeService.send(phone);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("phone", phone);
        resp.put("expireSeconds", 300);
        // dev 模式回显码（仅开发/演示）；生产环境对接短信网关后此字段为 null
        resp.put("devCode", code);
        return Result.success(resp);
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
        // 绑定会员上下文: CouponService.claim 依赖 LoginUserHolder 取租户
        bindAsMember(ctx[0], ctx[1]);
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

    // ============ 储值钱包(H5 会员端) ============

    /** 充值规则(充 X 送 Y) */
    @GetMapping("/wallet/rules")
    public Result<List<Map<String, Object>>> walletRules(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(h5Service.rechargeRules(ctx[1]));
    }

    /** 会员充值(演示环境直接到账) */
    @PostMapping("/wallet/recharge")
    public Result<Map<String, Object>> recharge(HttpServletRequest req,
                                                @Valid @RequestBody H5Dto.RechargeRequest body) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(memberService.recharge(ctx[0], body.toMemberRecharge()));
        } finally {
            LoginUserHolder.clear();
        }
    }

    // ============ 新增: 我的订单 ============

    @GetMapping("/orders")
    public Result<PageData<Map<String, Object>>> orders(HttpServletRequest req,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(orderService.listByMember(ctx[0], status, keyword, page, size));
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

    /** 会员端订单支付(PENDING -> PAID) */
    @PostMapping("/orders/{id}/pay")
    public Result<Map<String, Object>> payOrder(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(mallService.payOrder(ctx[1], ctx[0], id));
        } finally {
            LoginUserHolder.clear();
        }
    }

    /** 会员端订单取消(仅 PENDING 可取消) */
    @PostMapping("/orders/{id}/cancel")
    public Result<Map<String, Object>> cancelOrder(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        bindAsMember(ctx[0], ctx[1]);
        try {
            return Result.success(mallService.cancelOrder(ctx[1], ctx[0], id));
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
            return Result.success(productService.listActive(category, null));
        } finally {
            LoginUserHolder.clear();
        }
    }

    /** 活动列表(公开, 按租户隔离): 仅返回启用且在有效期内的活动 */
    @GetMapping("/campaigns")
    public Result<List<Map<String, Object>>> campaigns(HttpServletRequest req) {
        Long tenantId = tryTenantId(req);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> list = campaignRepository.listByTenant(tenantId, "ENABLED").stream()
                .filter(c -> (c.getStartAt() == null || !c.getStartAt().isAfter(now))
                        && (c.getEndAt() == null || !c.getEndAt().isBefore(now)))
                .map(this::campaignVO)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    /** 活动详情(公开, 无需 member token, 按租户隔离) */
    @GetMapping("/campaigns/{id}")
    public Result<Map<String, Object>> campaignDetail(@PathVariable Long id, HttpServletRequest req) {
        Long tenantId = tryTenantId(req);
        Campaign c = campaignRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getDeleted()))
                .filter(x -> tenantId.equals(x.getTenantId()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        return Result.success(campaignVO(c));
    }

    /** 活动视图对象(与 H5 前端契约一致) */
    private Map<String, Object> campaignVO(Campaign c) {
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
        return vo;
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
        return MemberContext.require(req, memberTokenUtil);
    }

    /** 尝试从 memberToken 解析 tenantId，失败则返回默认租户（用于公开接口） */
    private Long tryTenantId(HttpServletRequest req) {
        return MemberContext.tryTenantId(req, memberTokenUtil);
    }
}
