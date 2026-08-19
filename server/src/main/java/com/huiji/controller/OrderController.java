package com.huiji.controller;

import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.OrderDto;
import com.huiji.security.PreAllowed;
import com.huiji.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收银订单: 列表/详情/创建/支付/退款/作废。
 *  - 创建时可一次性结算(传 payMethod)
 *  - 也可创建为 PENDING 后再 pay
 *  - 已支付订单不可 void, 须 refund
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Result<PageData<Map<String, Object>>> list(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) Long storeId,
                                                      @RequestParam(required = false) Long memberId,
                                                      @RequestParam(required = false) String start,
                                                      @RequestParam(required = false) String end,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(orderService.list(status, storeId, memberId, start, end, page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody OrderDto.CreateOrderRequest req) {
        return Result.success(orderService.create(req));
    }

    @PostMapping("/{id}/pay")
    public Result<Map<String, Object>> pay(@PathVariable Long id, @Valid @RequestBody OrderDto.PayRequest req) {
        return Result.success(orderService.pay(id, req));
    }

    @PostMapping("/{id}/refund")
    public Result<Map<String, Object>> refund(@PathVariable Long id, @RequestBody(required = false) OrderDto.RefundRequest req) {
        return Result.success(orderService.refund(id, req));
    }

    @PostMapping("/{id}/void")
    public Result<Map<String, Object>> voidOrder(@PathVariable Long id, @RequestBody(required = false) OrderDto.RefundRequest req) {
        return Result.success(orderService.voidOrder(id, req));
    }
}
