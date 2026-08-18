package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.dto.MallDto;
import com.huiji.entity.Cart;
import com.huiji.entity.Member;
import com.huiji.entity.MallCategory;
import com.huiji.entity.Product;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.ProductRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberContext;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.MallService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * H5 商城会员端:
 * 公开: 分类、商品列表、商品详情
 * 需 member token: 购物车、结算、我的订单
 */
@RestController
@RequestMapping("/api/h5/mall")
@RequiredArgsConstructor
public class H5MallController {

    private final MallService mallService;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final MemberTokenUtil memberTokenUtil;

    // ============ 公开接口 ============

    /** 商城分类(公开) */
    @GetMapping("/categories")
    public Result<List<MallCategory>> categories(@RequestParam(required = false) Long tenantId,
                                                   HttpServletRequest req) {
        return Result.success(mallService.categories(resolveTenantId(tenantId, req)));
    }

    /** 商城商品列表(公开) */
    @GetMapping("/products")
    public Result<Map<String, Object>> products(@RequestParam(required = false) Long tenantId,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  HttpServletRequest req) {
        Long tid = resolveTenantId(tenantId, req);
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<Product> p = productRepository.searchMall(tid, categoryId, keyword, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::toProductVO).toList();
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("list", list);
        vo.put("total", p.getTotalElements());
        vo.put("page", page);
        vo.put("size", size);
        return Result.success(vo);
    }

    /** 商品详情(公开) */
    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> productDetail(@PathVariable Long id,
                                                      @RequestParam(required = false) Long tenantId,
                                                      HttpServletRequest req) {
        Long tid = resolveTenantId(tenantId, req);
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        return Result.success(toProductVO(p));
    }

    // ============ 购物车(member token) ============

    /** 购物车列表 */
    @GetMapping("/cart")
    public Result<List<Cart>> cartList(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.cartList(ctx[1], ctx[0]));
    }

    /** 加购 */
    @PostMapping("/cart")
    public Result<Cart> addToCart(HttpServletRequest req, @RequestBody MallDto.CartAddRequest body) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.addToCart(ctx[1], ctx[0], body));
    }

    /** 更新数量/选中 */
    @PutMapping("/cart/{cartId}")
    public Result<Cart> updateCart(HttpServletRequest req,
                                    @PathVariable Long cartId,
                                    @RequestBody MallDto.CartUpdateRequest body) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.updateCart(ctx[1], ctx[0], cartId, body));
    }

    /** 移除购物车项 */
    @DeleteMapping("/cart/{productId}")
    public Result<Void> removeFromCart(HttpServletRequest req, @PathVariable Long productId) {
        long[] ctx = currentMember(req);
        mallService.removeFromCart(ctx[1], ctx[0], productId);
        return Result.success();
    }

    /** 清空购物车 */
    @PostMapping("/cart/clear")
    public Result<Void> clearCart(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        mallService.clearCart(ctx[1], ctx[0]);
        return Result.success();
    }

    /** 购物车汇总 */
    @GetMapping("/cart/summary")
    public Result<Map<String, Object>> cartSummary(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.cartSummary(ctx[1], ctx[0]));
    }

    // ============ 商城订单(member token) ============

    /** 结算下单 */
    @PostMapping("/checkout")
    public Result<Map<String, Object>> checkout(HttpServletRequest req,
                                                 @RequestBody MallDto.CheckoutRequest body) {
        long[] ctx = currentMember(req);
        long memberId = ctx[0];
        long tenantId = ctx[1];
        bindAsMember(memberId, tenantId);
        try {
            return Result.success(mallService.checkout(tenantId, memberId, body));
        } finally {
            LoginUserHolder.clear();
        }
    }

    /** 我的商城订单 */
    @GetMapping("/my-orders")
    public Result<Map<String, Object>> myOrders(HttpServletRequest req,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.myOrders(ctx[1], ctx[0], status, keyword, page, size));
    }

    /** 订单详情 */
    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> orderDetail(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.orderDetail(ctx[1], ctx[0], id));
    }

    /** 订单支付(演示环境直接成功) */
    @PostMapping("/orders/{id}/pay")
    public Result<Map<String, Object>> payOrder(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.payOrder(ctx[1], ctx[0], id));
    }

    /** 取消订单 */
    @PostMapping("/orders/{id}/cancel")
    public Result<Map<String, Object>> cancelOrder(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.cancelOrder(ctx[1], ctx[0], id));
    }

    /** 确认收货 */
    @PostMapping("/orders/{id}/confirm")
    public Result<Map<String, Object>> confirmOrder(HttpServletRequest req, @PathVariable Long id) {
        long[] ctx = currentMember(req);
        return Result.success(mallService.confirmOrder(ctx[1], ctx[0], id));
    }

    // ============ 内部方法 ============

    /** 从 query 参数或 member token 推断 tenantId */
    private Long resolveTenantId(Long tenantId, HttpServletRequest req) {
        if (tenantId != null) return tenantId;
        return MemberContext.tryTenantId(req, memberTokenUtil);
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
        return MemberContext.require(req, memberTokenUtil, memberId ->
                memberRepository.findById(memberId)
                        .filter(x -> !Boolean.TRUE.equals(x.getDeleted()))
                        .map(Member::getTenantId)
                        .orElse(null));
    }

    private Map<String, Object> toProductVO(Product p) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", p.getId());
        vo.put("name", p.getName());
        vo.put("category", p.getCategory());
        vo.put("cover", p.getCover());
        vo.put("price", p.getPrice());
        vo.put("stock", p.getStock());
        vo.put("soldCount", p.getSoldCount());
        vo.put("description", p.getDescription());
        vo.put("mallCategoryId", p.getMallCategoryId());
        return vo;
    }
}
