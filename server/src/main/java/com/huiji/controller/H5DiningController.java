package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.dto.DiningDto;
import com.huiji.dto.OrderDto;
import com.huiji.entity.DiningTable;
import com.huiji.entity.KitchenOrder;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.DiningService;
import com.huiji.service.OrderService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * H5 扫码点餐接口。
 * 公开: 桌台信息、菜单; 需 member token: 提交订单、我的订单。
 */
@RestController
@RequestMapping("/api/h5/dining")
@RequiredArgsConstructor
public class H5DiningController {

    private final DiningService diningService;
    private final OrderService orderService;
    private final MemberTokenUtil memberTokenUtil;

    /** 扫码后获取桌台信息(公开) */
    @GetMapping("/table/{tableId}")
    public Result<DiningTable> table(@PathVariable Long tableId) {
        return Result.success(diningService.getTable(tableId));
    }

    /** 获取菜单按分类分组(公开) */
    @GetMapping("/menu")
    public Result<List<Map<String, Object>>> menu(@RequestParam Long storeId) {
        return Result.success(diningService.menuGroupedByCategory(storeId));
    }

    /** 提交点餐订单(需 member token) */
    @PostMapping("/order")
    public Result<Map<String, Object>> order(HttpServletRequest req,
                                             @RequestBody DiningDto.H5OrderRequest body) {
        long[] ctx = currentMember(req);
        long memberId = ctx[0];
        long tenantId = ctx[1];
        bindAsMember(memberId, tenantId);
        try {
            if (body.getItems() == null || body.getItems().isEmpty()) {
                throw new BizException(ErrorCode.VALIDATION, "请添加菜品");
            }
            if (body.getStoreId() == null) {
                throw new BizException(ErrorCode.VALIDATION, "请选择门店");
            }
            String orderType = body.getOrderType() == null ? "DINE_IN" : body.getOrderType().toUpperCase();

            // 构建下单请求
            OrderDto.CreateOrderRequest orderReq = new OrderDto.CreateOrderRequest();
            orderReq.setStoreId(body.getStoreId());
            orderReq.setMemberId(memberId);
            orderReq.setRemark(body.getRemark());
            List<OrderDto.OrderItemRequest> itemList = new ArrayList<>();
            Map<Long, String> remarkMap = new HashMap<>();
            for (DiningDto.H5OrderItem it : body.getItems()) {
                OrderDto.OrderItemRequest oi = new OrderDto.OrderItemRequest();
                oi.setProductId(it.getProductId());
                oi.setQuantity(it.getQuantity());
                itemList.add(oi);
                if (it.getRemark() != null && !it.getRemark().isBlank()) {
                    remarkMap.put(it.getProductId(), it.getRemark());
                }
            }
            orderReq.setItems(itemList);
            orderReq.setPayMethod(null);

            // 创建订单
            Map<String, Object> orderResult = orderService.create(orderReq);
            Long orderId = ((Number) orderResult.get("id")).longValue();

            // 构建厨房工单明细
            List<DiningDto.KitchenOrderItem> kitchenItems = new ArrayList<>();
            Object itemsObj = orderResult.get("items");
            if (itemsObj instanceof List<?> rawList) {
                for (Object o : rawList) {
                    if (o instanceof Map<?, ?> im) {
                        DiningDto.KitchenOrderItem ki = new DiningDto.KitchenOrderItem();
                        Object pid = im.get("productId");
                        ki.setProductId(pid == null ? null : ((Number) pid).longValue());
                        ki.setName((String) im.get("productName"));
                        Object qty = im.get("quantity");
                        ki.setQuantity(qty == null ? null : ((Number) qty).intValue());
                        ki.setRemark(remarkMap.get(ki.getProductId()));
                        kitchenItems.add(ki);
                    }
                }
            }

            // 创建厨房工单
            KitchenOrder ko = diningService.createKitchenOrder(tenantId, orderId,
                    body.getTableId(), orderType, kitchenItems);

            // 堂食占用桌台
            if ("DINE_IN".equals(orderType) && body.getTableId() != null) {
                diningService.occupyTable(tenantId, body.getTableId());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("order", orderResult);
            result.put("kitchenOrderId", ko.getId());
            return Result.success(result);
        } finally {
            LoginUserHolder.clear();
        }
    }

    /** 我的点餐订单(需 member token) */
    @GetMapping("/my-orders")
    public Result<List<Map<String, Object>>> myOrders(HttpServletRequest req) {
        long[] ctx = currentMember(req);
        long memberId = ctx[0];
        long tenantId = ctx[1];
        List<KitchenOrder> list = diningService.kitchenOrdersByMember(tenantId, memberId);
        return Result.success(list.stream().map(diningService::toKitchenVO).toList());
    }

    // ============ 内部方法 ============

    private void bindAsMember(long memberId, long tenantId) {
        LoginUser lu = LoginUser.builder()
                .userId(memberId)
                .tenantId(tenantId)
                .username(String.valueOf(memberId))
                .role("MEMBER")
                .build();
        LoginUserHolder.set(lu);
    }

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
