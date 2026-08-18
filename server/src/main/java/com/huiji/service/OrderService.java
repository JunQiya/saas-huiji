package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.dto.CouponDto;
import com.huiji.dto.OrderDto;
import com.huiji.entity.Member;
import com.huiji.entity.Order;
import com.huiji.entity.OrderItem;
import com.huiji.entity.Product;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.OrderItemRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.ProductRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 收银订单服务。
 *  - create 流程: 校验 -> 扣库存 -> 计算金额 -> 核销券 -> 余额扣款 -> 持久化
 *  - pay: 标记支付(余额支付时再扣一次)
 *  - refund: 校验状态, 若原 payMethod 为 BALANCE/MIXED, 退款回余额并写流水
 *  - void: 未支付订单直接作废, 释放库存(已扣减的)
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final WalletTransactionRepository walletRepository;
    private final CouponRecordRepository couponRecordRepository;
    private final CouponService couponService;
    private final com.huiji.repository.StoreRepository storeRepository;
    private final AuditHelper auditHelper;

    @Transactional
    public Map<String, Object> create(OrderDto.CreateOrderRequest req) {
        LoginUser lu = LoginUserHolder.current();
        Long tenantId = lu.getTenantId();
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION, "请添加商品");
        }
        // 门店: 优先从 token 取, 其次从前端传入, 最后取租户第一个门店
        Long curStore = LoginUserHolder.requireStoreId();
        Long storeId = curStore != null ? curStore : req.getStoreId();
        if (storeId == null) {
            List<com.huiji.entity.Store> stores = storeRepository
                    .findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId);
            if (!stores.isEmpty()) {
                storeId = stores.get(0).getId();
            }
        }
        if (storeId == null) {
            throw new BizException(ErrorCode.VALIDATION, "请选择门店");
        }

        // 1) 加载商品 + 校验
        List<OrderItem> items = new ArrayList<>();
        long total = 0L;
        for (OrderDto.OrderItemRequest it : req.getItems()) {
            if (it.getProductId() == null || it.getQuantity() == null || it.getQuantity() <= 0) {
                throw new BizException(ErrorCode.VALIDATION, "商品数量必须为正");
            }
            Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(it.getProductId(), tenantId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在 ID=" + it.getProductId()));
            if (!"ACTIVE".equals(p.getStatus())) {
                throw new BizException(ErrorCode.BIZ_ERROR, "商品「" + p.getName() + "」已下架");
            }
            // 适用门店校验
            if (p.getStoreIds() != null && !p.getStoreIds().isEmpty()
                    && !p.getStoreIds().contains(storeId)) {
                throw new BizException(ErrorCode.BIZ_ERROR,
                        "商品「" + p.getName() + "」不在当前门店");
            }
            // 2) 扣减库存
            deductTx(p, it.getQuantity());

            long subtotal = (p.getPrice() == null ? 0L : p.getPrice()) * it.getQuantity();
            total += subtotal;
            OrderItem oi = new OrderItem();
            oi.setTenantId(tenantId);
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setUnitPrice(p.getPrice() == null ? 0L : p.getPrice());
            oi.setQuantity(it.getQuantity());
            oi.setSubtotal(subtotal);
            items.add(oi);
        }
        // 3) 优惠金额
        long discount = req.getDiscountAmount() == null ? 0L : Math.max(0L, req.getDiscountAmount());
        if (discount > total) {
            throw new BizException(ErrorCode.BIZ_ERROR, "优惠金额不能超过订单总额");
        }
        long payable = total - discount;

        // 4) 核销券(若传 couponCode, 通过 CouponService.verify 自动校验+核销)
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            CouponDto.VerifyRequest vr = new CouponDto.VerifyRequest();
            vr.setCode(req.getCouponCode().trim());
            vr.setStoreId(storeId);
            couponService.verify(vr);
        }

        // 5) 持久化订单
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNo(genOrderNo());
        order.setStoreId(storeId);
        order.setMemberId(req.getMemberId());
        order.setCashierId(lu.getUserId());
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setRemark(req.getRemark());

        String payMethod = req.getPayMethod() == null ? null : req.getPayMethod().toUpperCase();
        if (payMethod == null) {
            order.setPayMethod(null);
            order.setStatus("PENDING");
            order.setPaidAmount(0L);
        } else {
            order.setPayMethod(payMethod);
            if ("BALANCE".equals(payMethod)) {
                if (req.getMemberId() == null) {
                    throw new BizException(ErrorCode.BIZ_ERROR, "余额支付需绑定会员");
                }
                consumeBalance(req.getMemberId(), tenantId, storeId, payable, order.getOrderNo(), req.getRemark());
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                order.setPaidAmount(payable);
            } else if ("MIXED".equals(payMethod)) {
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                order.setPaidAmount(0L);
            } else if (List.of("CASH", "WECHAT", "ALIPAY").contains(payMethod)) {
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                order.setPaidAmount(payable);
            } else {
                throw new BizException(ErrorCode.VALIDATION, "支付方式不合法");
            }
        }
        // persist + flush: 立即回填主键, 供返回的 orderId 使用(MySQL/H2 均可靠)
        entityManager.persist(order);
        entityManager.flush();
        for (OrderItem oi : items) {
            oi.setOrderId(order.getId());
        }
        orderItemRepository.saveAll(items);
        // 已支付订单为会员累加积分
        if ("PAID".equals(order.getStatus()) && order.getMemberId() != null) {
            awardPoints(order.getMemberId(), order.getStoreId(), payable, order.getOrderNo());
        }
        auditHelper.record("创建订单", "order:" + order.getOrderNo(), "金额 " + total);
        return detail(order.getId());
    }

    /** 微信支付回调标记已支付, 无需登录上下文 */
    @Transactional
    public void markPaidByWxNotify(String orderNo, String transactionId) {
        Order order = orderRepository.findByOrderNoAndDeletedFalse(orderNo)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在: " + orderNo));
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "订单状态不可支付: " + order.getStatus());
        }
        order.setPayMethod("WECHAT");
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        long payable = order.getTotalAmount() - (order.getDiscountAmount() == null ? 0L : order.getDiscountAmount());
        order.setPaidAmount(payable);
        orderRepository.save(order);
        auditHelper.record("微信支付回调", "order:" + order.getOrderNo(), transactionId);
    }

    @Transactional
    public Map<String, Object> pay(Long id, OrderDto.PayRequest req) {
        LoginUser lu = LoginUserHolder.current();
        Long tenantId = lu.getTenantId();
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前订单状态不可支付: " + order.getStatus());
        }
        if (req == null || req.getPayMethod() == null || req.getPayMethod().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "请选择支付方式");
        }
        String method = req.getPayMethod().toUpperCase();
        if (!List.of("CASH", "WECHAT", "ALIPAY", "BALANCE", "MIXED").contains(method)) {
            throw new BizException(ErrorCode.VALIDATION, "支付方式不合法");
        }
        long payable = order.getTotalAmount() - order.getDiscountAmount();
        if ("BALANCE".equals(method)) {
            if (order.getMemberId() == null) {
                throw new BizException(ErrorCode.BIZ_ERROR, "余额支付需绑定会员");
            }
            consumeBalance(order.getMemberId(), tenantId, order.getStoreId(), payable, order.getOrderNo(), null);
        }
        order.setPayMethod(method);
        order.setPaidAmount(req.getPaidAmount() == null ? payable : req.getPaidAmount());
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        // 已支付订单为会员累加积分
        if (order.getMemberId() != null) {
            awardPoints(order.getMemberId(), order.getStoreId(), payable, order.getOrderNo());
        }
        auditHelper.record("订单支付", "order:" + order.getOrderNo(), method);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> refund(Long id, OrderDto.RefundRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!"PAID".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "只有已支付订单可退款");
        }
        long amt = order.getPaidAmount() == null
                ? (order.getTotalAmount() - order.getDiscountAmount()) : order.getPaidAmount();
        if (order.getMemberId() != null
                && ("BALANCE".equalsIgnoreCase(order.getPayMethod())
                    || "MIXED".equalsIgnoreCase(order.getPayMethod()))
                && amt > 0) {
            refundBalance(order.getMemberId(), tenantId, order.getStoreId(), amt, order.getOrderNo(),
                    req == null ? null : req.getReason());
        }
        order.setStatus("REFUNDED");
        order.setRefundedAt(LocalDateTime.now());
        order.setRefundReason(req == null ? null : req.getReason());
        orderRepository.save(order);
        auditHelper.record("订单退款", "order:" + order.getOrderNo(), req == null ? "" : req.getReason());
        return detail(id);
    }

    @Transactional
    public Map<String, Object> voidOrder(Long id, OrderDto.RefundRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if ("PAID".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "已支付订单请走退款流程");
        }
        if ("VOID".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "订单已是作废状态");
        }
        for (OrderItem oi : orderItemRepository.findByOrderIdOrderByIdAsc(id)) {
            if (oi.getProductId() == null) continue;
            productRepository.findByIdAndTenantIdAndDeletedFalse(oi.getProductId(), tenantId)
                    .ifPresent(p -> {
                        if ("GOODS".equals(p.getCategory())) {
                            p.setStock((p.getStock() == null ? 0 : p.getStock()) + oi.getQuantity());
                            p.setSoldCount(Math.max(0, (p.getSoldCount() == null ? 0 : p.getSoldCount()) - oi.getQuantity()));
                            productRepository.save(p);
                        }
                    });
        }
        order.setStatus("VOID");
        order.setRefundedAt(LocalDateTime.now());
        order.setRefundReason(req == null ? null : req.getReason());
        orderRepository.save(order);
        auditHelper.record("订单作废", "order:" + order.getOrderNo(), req == null ? "" : req.getReason());
        return detail(id);
    }

    public Map<String, Object> detail(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        Map<String, Object> vo = toVO(order);
        vo.put("items", orderItemRepository.findByOrderIdOrderByIdAsc(id).stream().map(this::toItemVO).toList());
        return vo;
    }

    public PageData<Map<String, Object>> list(String status, Long storeId, Long memberId,
                                              String start, String end, int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Long curStore = LoginUserHolder.requireStoreId();
        Long sid = curStore != null ? curStore : storeId;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<Order> p = orderRepository.search(tenantId, blank(status), sid, memberId,
                parseTime(start, false), parseTime(end, true), pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::toVO).toList();
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    public PageData<Map<String, Object>> listByMember(Long memberId, String status, String keyword, int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<Order> p = orderRepository.listByMember(tenantId, memberId, blank(status), keyword, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::toVO).toList();
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    public Map<String, Object> memberDetail(Long id, Long memberId) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getMemberId() == null || !order.getMemberId().equals(memberId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "订单不属于该会员");
        }
        return detail(id);
    }

    public Map<String, Object> todayStats() {
        Long tenantId = LoginUserHolder.currentTenantId();
        Long storeId = LoginUserHolder.requireStoreId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<Object[]> rows = orderRepository.todayStats(tenantId, start, end, storeId);
        long count = 0L;
        long amount = 0L;
        if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
            Object[] r = rows.get(0);
            if (r[0] != null) count = ((Number) r[0]).longValue();
            if (r[1] != null) amount = ((Number) r[1]).longValue();
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("count", count);
        vo.put("amount", amount);
        vo.put("date", today.toString());
        return vo;
    }

    // ---- 内部方法 ----

    private void consumeBalance(Long memberId, Long tenantId, Long storeId, long amount,
                                String orderNo, String remark) {
        if (amount <= 0) return;
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        long balance = m.getBalance() == null ? 0L : m.getBalance();
        if (balance < amount) {
            throw new BizException(ErrorCode.BIZ_ERROR, "储值余额不足, 当前余额 " + balance + " 分");
        }
        long after = balance - amount;
        m.setBalance(after);
        m.setConsumeCount((m.getConsumeCount() == null ? 0 : m.getConsumeCount()) + 1);
        m.setTotalAmount((m.getTotalAmount() == null ? 0L : m.getTotalAmount()) + amount);
        m.setLastConsumeAt(LocalDateTime.now());
        memberRepository.save(m);

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setMemberId(memberId);
        tx.setType("CONSUME");
        tx.setAmount(-amount);
        tx.setBalanceAfter(after);
        tx.setStoreId(storeId);
        tx.setOrderNo(orderNo);
        tx.setPayMethod("BALANCE");
        tx.setRemark(remark == null ? "订单消费" : remark);
        tx.setOperatorId(LoginUserHolder.currentUserId());
        walletRepository.save(tx);
    }

    private void refundBalance(Long memberId, Long tenantId, Long storeId, long amount,
                               String orderNo, String reason) {
        if (amount <= 0) return;
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        long balance = m.getBalance() == null ? 0L : m.getBalance();
        long after = balance + amount;
        m.setBalance(after);
        memberRepository.save(m);

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setMemberId(memberId);
        tx.setType("REFUND");
        tx.setAmount(amount);
        tx.setBalanceAfter(after);
        tx.setStoreId(storeId);
        tx.setOrderNo(orderNo);
        tx.setPayMethod("BALANCE");
        tx.setRemark(reason == null ? "订单退款" : reason);
        tx.setOperatorId(LoginUserHolder.currentUserId());
        walletRepository.save(tx);
    }

    /**
     * 消费赠送积分: 1 元 = 1 积分 (amount 单位为分, 转为元即为积分值)。
     * 累加到 member.points, 并写入一条 WalletTransaction(type=POINT)流水。
     */
    @Transactional
    public void awardPoints(Long memberId, Long storeId, Long amount, String orderNo) {
        if (memberId == null) {
            return;
        }
        if (amount == null || amount <= 0) {
            return;
        }
        long points = amount / 100L;
        if (points <= 0) {
            return;
        }
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        long currentPoints = m.getPoints() == null ? 0L : m.getPoints();
        m.setPoints(currentPoints + points);
        memberRepository.save(m);

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(m.getTenantId());
        tx.setMemberId(memberId);
        tx.setType("POINT");
        tx.setAmount(points);
        tx.setBalanceAfter(m.getPoints());
        tx.setStoreId(storeId);
        tx.setOrderNo(orderNo);
        tx.setRemark("消费赠送积分");
        walletRepository.save(tx);
    }

    private void deductTx(Product p, int qty) {
        if (!"GOODS".equals(p.getCategory())) return;
        int current = p.getStock() == null ? 0 : p.getStock();
        if (current < qty) {
            throw new BizException(ErrorCode.BIZ_ERROR,
                    "商品「" + p.getName() + "」库存不足, 当前库存 " + current);
        }
        p.setStock(current - qty);
        p.setSoldCount((p.getSoldCount() == null ? 0 : p.getSoldCount()) + qty);
        productRepository.save(p);
    }

    private String genOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "OD" + ts + rand;
    }

    private LocalDateTime parseTime(String s, boolean isEnd) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return isEnd ? d.plusDays(1).atStartOfDay() : d.atStartOfDay();
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private Map<String, Object> toVO(Order o) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", o.getId());
        vo.put("orderNo", o.getOrderNo());
        vo.put("storeId", o.getStoreId());
        vo.put("memberId", o.getMemberId());
        vo.put("cashierId", o.getCashierId());
        vo.put("totalAmount", o.getTotalAmount());
        vo.put("discountAmount", o.getDiscountAmount());
        vo.put("paidAmount", o.getPaidAmount());
        vo.put("payMethod", o.getPayMethod());
        vo.put("status", o.getStatus());
        vo.put("paidAt", o.getPaidAt());
        vo.put("refundedAt", o.getRefundedAt());
        vo.put("refundReason", o.getRefundReason());
        vo.put("remark", o.getRemark());
        vo.put("createdAt", o.getCreatedAt());
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
}
