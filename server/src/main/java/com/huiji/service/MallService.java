package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.MallDto;
import com.huiji.dto.OrderDto;
import com.huiji.entity.Cart;
import com.huiji.entity.MallCategory;
import com.huiji.entity.Order;
import com.huiji.entity.OrderExtend;
import com.huiji.entity.OrderItem;
import com.huiji.entity.Product;
import com.huiji.repository.CartRepository;
import com.huiji.repository.MallCategoryRepository;
import com.huiji.repository.OrderExtendRepository;
import com.huiji.repository.OrderItemRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商城服务: 分类管理、购物车、商城订单结算。
 * 结算时复用 OrderService.create 创建订单, 再补充 OrderExtend 物流信息。
 */
@Service
@RequiredArgsConstructor
public class MallService {

    private final MallCategoryRepository categoryRepository;
    private final CartRepository cartRepository;
    private final OrderExtendRepository orderExtendRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    /** 商城"免支付直接成功"开关(仅演示环境; 生产 false 时禁止免支付) */
    @Value("${huiji.h5.mall-demo-pay:false}")
    private boolean mallDemoPay;

    // ============ 分类管理 ============

    /** 分类列表 */
    public List<MallCategory> categories(Long tenantId) {
        return categoryRepository.findByTenantIdOrderBySortOrderAsc(tenantId);
    }

    /** 创建/更新分类 */
    @Transactional
    public MallCategory saveCategory(Long tenantId, MallDto.CategoryRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "分类名称不能为空");
        }
        MallCategory cat;
        if (req.getId() != null) {
            cat = categoryRepository.findById(req.getId())
                    .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "分类不存在"));
        } else {
            cat = new MallCategory();
            cat.setTenantId(tenantId);
        }
        cat.setName(req.getName().trim());
        cat.setIcon(req.getIcon());
        cat.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        String status = req.getStatus();
        if (status != null && !status.isBlank()) {
            cat.setStatus(status.toUpperCase());
        }
        return categoryRepository.save(cat);
    }

    /** 删除分类 */
    @Transactional
    public void removeCategory(Long tenantId, Long id) {
        MallCategory cat = categoryRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "分类不存在"));
        cat.setDeleted(true);
        categoryRepository.save(cat);
    }

    /** 绑定商品到分类(更新 Product 的 mallCategoryId) */
    @Transactional
    public void bindProducts(Long tenantId, Long categoryId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        MallCategory cat = categoryRepository.findById(categoryId)
                .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "分类不存在"));
        List<Product> products = productRepository.findByIdInAndTenantIdAndDeletedFalse(productIds, tenantId);
        for (Product p : products) {
            p.setMallCategoryId(categoryId);
            // 绑定分类时默认上架到商城
            if (p.getMallVisible() == null) {
                p.setMallVisible(true);
            }
            productRepository.save(p);
        }
        // 更新分类商品数冗余字段
        cat.setProductCount(products.size());
        categoryRepository.save(cat);
    }

    // ============ 购物车 ============

    /** 购物车列表 */
    public List<Cart> cartList(Long tenantId, Long memberId) {
        return cartRepository.findByMemberIdAndTenantIdAndDeletedFalseOrderByIdDesc(memberId, tenantId);
    }

    /** 加购(已存在则加数量) */
    @Transactional
    public Cart addToCart(Long tenantId, Long memberId, MallDto.CartAddRequest req) {
        if (req.getProductId() == null) {
            throw new BizException(ErrorCode.VALIDATION, "商品 ID 不能为空");
        }
        int qty = req.getQuantity() == null ? 1 : req.getQuantity();
        if (qty <= 0) {
            throw new BizException(ErrorCode.VALIDATION, "数量必须为正");
        }
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(req.getProductId(), tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!"ACTIVE".equals(p.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "商品已下架");
        }
        Cart cart = cartRepository
                .findByMemberIdAndTenantIdAndProductIdAndDeletedFalse(memberId, tenantId, req.getProductId())
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setTenantId(tenantId);
                    c.setMemberId(memberId);
                    c.setProductId(p.getId());
                    c.setProductName(p.getName());
                    c.setProductImage(p.getCover());
                    c.setPrice(p.getPrice() == null ? 0L : p.getPrice());
                    c.setQuantity(0);
                    c.setSelected(true);
                    return c;
                });
        cart.setQuantity(cart.getQuantity() + qty);
        // 同步最新价格和名称
        cart.setProductName(p.getName());
        cart.setProductImage(p.getCover());
        cart.setPrice(p.getPrice() == null ? 0L : p.getPrice());
        return cartRepository.save(cart);
    }

    /** 更新数量/选中状态 */
    @Transactional
    public Cart updateCart(Long tenantId, Long memberId, Long cartId, MallDto.CartUpdateRequest req) {
        Cart cart = cartRepository.findById(cartId)
                .filter(c -> tenantId.equals(c.getTenantId())
                        && memberId.equals(c.getMemberId())
                        && !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "购物车项不存在"));
        if (req.getQuantity() != null) {
            if (req.getQuantity() <= 0) {
                throw new BizException(ErrorCode.VALIDATION, "数量必须为正");
            }
            cart.setQuantity(req.getQuantity());
        }
        if (req.getSelected() != null) {
            cart.setSelected(req.getSelected());
        }
        return cartRepository.save(cart);
    }

    /** 移除购物车项 */
    @Transactional
    public void removeFromCart(Long tenantId, Long memberId, Long productId) {
        cartRepository.deleteByMemberIdAndTenantIdAndProductId(memberId, tenantId, productId);
    }

    /** 清空购物车 */
    @Transactional
    public void clearCart(Long tenantId, Long memberId) {
        cartRepository.deleteByMemberIdAndTenantId(memberId, tenantId);
    }

    /** 购物车汇总: 总件数/选中件数/总价 */
    public Map<String, Object> cartSummary(Long tenantId, Long memberId) {
        List<Cart> list = cartRepository.findByMemberIdAndTenantIdAndDeletedFalseOrderByIdDesc(memberId, tenantId);
        int totalCount = 0;
        int selectedCount = 0;
        long totalPrice = 0L;
        for (Cart c : list) {
            int qty = c.getQuantity() == null ? 0 : c.getQuantity();
            totalCount += qty;
            if (Boolean.TRUE.equals(c.getSelected())) {
                selectedCount += qty;
                totalPrice += (c.getPrice() == null ? 0L : c.getPrice()) * qty;
            }
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("totalCount", totalCount);
        vo.put("selectedCount", selectedCount);
        vo.put("totalPrice", totalPrice);
        return vo;
    }

    // ============ 商城订单 ============

    /**
     * 结算下单:
     * 1. 查购物车选中项(或指定 itemIds)
     * 2. 构建 OrderService 下单请求
     * 3. 创建 OrderExtend 物流信息
     * 4. 清空已购购物车项
     */
    @Transactional
    public Map<String, Object> checkout(Long tenantId, Long memberId, MallDto.CheckoutRequest req) {
        // 1) 确定结算的购物车项
        List<Cart> allCart = cartRepository
                .findByMemberIdAndTenantIdAndDeletedFalseOrderByIdDesc(memberId, tenantId);
        List<Cart> toCheckout;
        if (req.getItemIds() != null && req.getItemIds().length > 0) {
            Set<Long> idSet = new HashSet<>(Arrays.asList(req.getItemIds()));
            toCheckout = allCart.stream().filter(c -> idSet.contains(c.getId())).collect(Collectors.toList());
        } else {
            toCheckout = allCart.stream().filter(c -> Boolean.TRUE.equals(c.getSelected())).collect(Collectors.toList());
        }
        if (toCheckout.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "没有可结算的商品");
        }

        // 2) 构建下单请求
        OrderDto.CreateOrderRequest orderReq = new OrderDto.CreateOrderRequest();
        // 商城订单门店: 优先用请求中的 storeId; 未指定时从购物车商品适用门店推导,
        // 避免 storeId=0 触发商品门店校验失败(商品 storeIds 非空时)
        Long checkoutStoreId = req.getStoreId();
        if (checkoutStoreId == null || checkoutStoreId <= 0) {
            checkoutStoreId = inferStoreId(tenantId, toCheckout);
        }
        orderReq.setStoreId(checkoutStoreId);
        orderReq.setMemberId(memberId);
        orderReq.setRemark(req.getRemark());
        List<OrderDto.OrderItemRequest> items = new ArrayList<>();
        for (Cart c : toCheckout) {
            OrderDto.OrderItemRequest oi = new OrderDto.OrderItemRequest();
            oi.setProductId(c.getProductId());
            oi.setQuantity(c.getQuantity());
            items.add(oi);
        }
        orderReq.setItems(items);
        // 创建为 PENDING 订单, 支付由后续步骤处理
        orderReq.setPayMethod(null);

        // 3) 调用 OrderService 创建订单(需登录上下文, 由 Controller 层 bindAsMember)
        Map<String, Object> orderResult = orderService.create(orderReq);
        Long orderId = ((Number) orderResult.get("id")).longValue();

        // 4) 创建 OrderExtend 物流信息
        OrderExtend ext = new OrderExtend();
        ext.setTenantId(tenantId);
        ext.setOrderId(orderId);
        String deliveryType = req.getDeliveryType() == null ? "DELIVERY" : req.getDeliveryType().toUpperCase();
        ext.setDeliveryType(deliveryType);
        ext.setReceiverName(req.getReceiverName());
        ext.setReceiverPhone(req.getReceiverPhone());
        ext.setReceiverAddress(req.getReceiverAddress());
        ext.setReceiverProvince(req.getReceiverProvince());
        ext.setReceiverCity(req.getReceiverCity());
        ext.setReceiverDistrict(req.getReceiverDistrict());
        ext.setStoreId(req.getStoreId());
        // 运费简化处理: 暂为 0
        ext.setFreight(0L);
        orderExtendRepository.save(ext);

        // 5) 清空已购购物车项
        for (Cart c : toCheckout) {
            cartRepository.deleteByMemberIdAndTenantIdAndProductId(memberId, tenantId, c.getProductId());
        }

        // 6) 返回订单信息和支付参数
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", orderResult);
        result.put("orderId", orderId);
        result.put("orderNo", orderResult.get("orderNo"));
        result.put("totalAmount", orderResult.get("totalAmount"));
        result.put("status", orderResult.get("status"));
        // 支付参数占位: 实际由支付模块生成
        Map<String, Object> payParams = new LinkedHashMap<>();
        payParams.put("payMethod", req.getPayMethod());
        payParams.put("orderId", orderId);
        payParams.put("orderNo", orderResult.get("orderNo"));
        result.put("payParams", payParams);
        return result;
    }

    /**
     * 从购物车商品推导结算门店: 取所有商品适用门店的交集; 无交集则取第一个商品的首个门店。
     * 避免 storeId 缺省时商品门店校验失败。
     */
    private Long inferStoreId(Long tenantId, List<Cart> carts) {
        Set<Long> common = null;
        for (Cart c : carts) {
            Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(c.getProductId(), tenantId).orElse(null);
            if (p == null || p.getStoreIds() == null || p.getStoreIds().isEmpty()) {
                continue;
            }
            Set<Long> stores = new HashSet<>(p.getStoreIds());
            if (common == null) {
                common = new HashSet<>(stores);
            } else {
                common.retainAll(stores);
            }
        }
        if (common != null && !common.isEmpty()) {
            return common.iterator().next();
        }
        // 兜底: 第一个商品的第一个门店
        for (Cart c : carts) {
            Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(c.getProductId(), tenantId).orElse(null);
            if (p != null && p.getStoreIds() != null && !p.getStoreIds().isEmpty()) {
                return p.getStoreIds().get(0);
            }
        }
        return null;
    }

    /** 我的商城订单列表(含 OrderExtend)。keyword 支持按订单号模糊搜索 */
    public Map<String, Object> myOrders(Long tenantId, Long memberId, String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        String st = (status == null || status.isBlank()) ? null : status.trim();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Order> p = orderRepository.listByMember(tenantId, memberId, st, kw, pageable);
        List<Map<String, Object>> list = p.getContent().stream()
                .map(o -> toOrderVO(o, true))
                .collect(Collectors.toList());
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("list", list);
        vo.put("total", p.getTotalElements());
        vo.put("page", page);
        vo.put("size", size);
        return vo;
    }

    /** 订单详情 */
    public Map<String, Object> orderDetail(Long tenantId, Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (memberId != null && (order.getMemberId() == null || !order.getMemberId().equals(memberId))) {
            throw new BizException(ErrorCode.FORBIDDEN, "订单不属于该会员");
        }
        return toOrderVO(order, true);
    }

    /** 商城订单支付: 演示环境可直接成功; 生产环境必须走真实微信支付(/api/wxpay/order/{id}) */
    @Transactional
    public Map<String, Object> payOrder(Long tenantId, Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getMemberId() == null || !order.getMemberId().equals(memberId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "订单不属于该会员");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前订单状态不可支付: " + order.getStatus());
        }
        if (!mallDemoPay) {
            throw new BizException(ErrorCode.BIZ_ERROR, "请通过微信支付完成付款");
        }
        long payable = order.getTotalAmount()
                - (order.getDiscountAmount() == null ? 0L : order.getDiscountAmount());
        order.setPayMethod("WECHAT");
        order.setPaidAmount(payable);
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        return orderDetail(tenantId, memberId, orderId);
    }

    /** 取消订单(仅 PENDING 可取消) */
    @Transactional
    public Map<String, Object> cancelOrder(Long tenantId, Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getMemberId() == null || !order.getMemberId().equals(memberId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "订单不属于该会员");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前订单状态不可取消: " + order.getStatus());
        }
        // 归还下单时占用的库存
        orderService.restoreStock(order);
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        return orderDetail(tenantId, memberId, orderId);
    }

    /** 确认收货(仅 SHIPPED 可确认, 确认后 -> COMPLETED) */
    @Transactional
    public Map<String, Object> confirmOrder(Long tenantId, Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getMemberId() == null || !order.getMemberId().equals(memberId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "订单不属于该会员");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前订单状态不可确认收货: " + order.getStatus());
        }
        order.setStatus("COMPLETED");
        orderRepository.save(order);
        return orderDetail(tenantId, memberId, orderId);
    }

    /** 更新物流信息(admin) */
    @Transactional
    public OrderExtend updateTracking(Long tenantId, Long orderId, String trackingNo, String trackingCompany) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        OrderExtend ext = orderExtendRepository.findByOrderIdAndDeletedFalse(order.getId())
                .orElseGet(() -> {
                    OrderExtend e = new OrderExtend();
                    e.setTenantId(tenantId);
                    e.setOrderId(orderId);
                    return e;
                });
        ext.setTrackingNo(trackingNo);
        ext.setTrackingCompany(trackingCompany);
        return orderExtendRepository.save(ext);
    }

    /** admin 商城订单列表 */
    public List<Map<String, Object>> mallOrders(Long tenantId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        String st = (status == null || status.isBlank()) ? null : status.trim();
        Page<Order> p = orderRepository.searchMallOrders(tenantId, st, pageable);
        return p.getContent().stream()
                .map(o -> toOrderVO(o, true))
                .collect(Collectors.toList());
    }

    // ============ 内部方法 ============

    private Map<String, Object> toOrderVO(Order o, boolean withExtend) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", o.getId());
        vo.put("orderNo", o.getOrderNo());
        vo.put("storeId", o.getStoreId());
        vo.put("memberId", o.getMemberId());
        vo.put("totalAmount", o.getTotalAmount());
        vo.put("discountAmount", o.getDiscountAmount());
        vo.put("paidAmount", o.getPaidAmount());
        vo.put("payMethod", o.getPayMethod());
        vo.put("status", o.getStatus());
        vo.put("paidAt", o.getPaidAt());
        vo.put("remark", o.getRemark());
        vo.put("createdAt", o.getCreatedAt());
        // 订单明细
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(o.getId());
        vo.put("items", items.stream().map(this::toItemVO).collect(Collectors.toList()));
        // 扩展信息
        if (withExtend) {
            orderExtendRepository.findByOrderIdAndDeletedFalse(o.getId())
                    .ifPresent(ext -> vo.put("extend", toExtendVO(ext)));
        }
        return vo;
    }

    private Map<String, Object> toItemVO(OrderItem oi) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", oi.getId());
        vo.put("productId", oi.getProductId());
        vo.put("productName", oi.getProductName());
        vo.put("unitPrice", oi.getUnitPrice());
        vo.put("quantity", oi.getQuantity());
        vo.put("subtotal", oi.getSubtotal());
        return vo;
    }

    private Map<String, Object> toExtendVO(OrderExtend ext) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("deliveryType", ext.getDeliveryType());
        vo.put("receiverName", ext.getReceiverName());
        vo.put("receiverPhone", ext.getReceiverPhone());
        vo.put("receiverAddress", ext.getReceiverAddress());
        vo.put("receiverProvince", ext.getReceiverProvince());
        vo.put("receiverCity", ext.getReceiverCity());
        vo.put("receiverDistrict", ext.getReceiverDistrict());
        vo.put("storeId", ext.getStoreId());
        vo.put("freight", ext.getFreight());
        vo.put("trackingNo", ext.getTrackingNo());
        vo.put("trackingCompany", ext.getTrackingCompany());
        return vo;
    }
}
