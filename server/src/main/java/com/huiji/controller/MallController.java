package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.MallDto;
import com.huiji.entity.MallCategory;
import com.huiji.entity.OrderExtend;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.MallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商城管理(admin token):
 *  GET    /api/mall/categories            分类列表
 *  POST   /api/mall/categories            创建/更新分类
 *  DELETE /api/mall/categories/{id}       删除分类
 *  POST   /api/mall/categories/{id}/products  绑定商品到分类
 *  GET    /api/mall/orders                商城订单列表
 *  PUT    /api/mall/orders/{orderId}/tracking  更新物流
 */
@RestController
@RequestMapping("/api/mall")
@RequiredArgsConstructor
public class MallController {

    private final MallService mallService;

    /** 分类列表 */
    @GetMapping("/categories")
    public Result<List<MallCategory>> categories() {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(mallService.categories(tenantId));
    }

    /** 创建/更新分类 */
    @PostMapping("/categories")
    public Result<MallCategory> saveCategory(@RequestBody MallDto.CategoryRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(mallService.saveCategory(tenantId, req));
    }

    /** 删除分类 */
    @DeleteMapping("/categories/{id}")
    public Result<Void> removeCategory(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        mallService.removeCategory(tenantId, id);
        return Result.success();
    }

    /** 绑定商品到分类 */
    @PostMapping("/categories/{id}/products")
    public Result<Void> bindProducts(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<Long> productIds = body == null ? null : body.get("productIds");
        mallService.bindProducts(tenantId, id, productIds);
        return Result.success();
    }

    /** 商城订单列表 */
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders(@RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(mallService.mallOrders(tenantId, status, page, size));
    }

    /** 更新物流 */
    @PutMapping("/orders/{orderId}/tracking")
    public Result<OrderExtend> updateTracking(@PathVariable Long orderId,
                                               @RequestBody Map<String, String> body) {
        Long tenantId = LoginUserHolder.currentTenantId();
        String trackingNo = body == null ? null : body.get("trackingNo");
        String trackingCompany = body == null ? null : body.get("trackingCompany");
        return Result.success(mallService.updateTracking(tenantId, orderId, trackingNo, trackingCompany));
    }
}
