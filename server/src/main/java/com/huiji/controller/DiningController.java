package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.DiningDto;
import com.huiji.entity.DiningTable;
import com.huiji.entity.KitchenOrder;
import com.huiji.entity.MenuCategory;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.DiningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 线下门店点餐管理端接口(需 admin token)。
 */
@RestController
@RequestMapping("/api/dining")
@RequiredArgsConstructor
public class DiningController {

    private final DiningService diningService;

    // ============ 桌台 ============

    @GetMapping("/tables")
    public Result<List<DiningTable>> tables(@RequestParam Long storeId) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.tables(tenantId, storeId));
    }

    @PostMapping("/tables")
    public Result<DiningTable> saveTable(@RequestBody DiningDto.TableRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.saveTable(tenantId, req));
    }

    @DeleteMapping("/tables/{id}")
    public Result<Void> removeTable(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        diningService.removeTable(tenantId, id);
        return Result.success();
    }

    @PostMapping("/tables/{id}/occupy")
    public Result<DiningTable> occupy(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.occupyTable(tenantId, id));
    }

    @PostMapping("/tables/{id}/free")
    public Result<DiningTable> free(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.freeTable(tenantId, id));
    }

    @PostMapping("/tables/{id}/qrcode")
    public Result<String> qrcode(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.generateQrcode(tenantId, id));
    }

    // ============ 菜单分类 ============

    @GetMapping("/categories")
    public Result<List<MenuCategory>> categories(@RequestParam Long storeId) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.categories(tenantId, storeId));
    }

    @PostMapping("/categories")
    public Result<MenuCategory> saveCategory(@RequestBody DiningDto.CategoryRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(diningService.saveCategory(tenantId, req));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> removeCategory(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        diningService.removeCategory(tenantId, id);
        return Result.success();
    }

    @PostMapping("/categories/{id}/products")
    public Result<Void> bindProducts(@PathVariable Long id,
                                     @RequestBody DiningDto.BindProductsRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        diningService.bindProductsToCategory(tenantId, id, req.getProductIds());
        return Result.success();
    }

    // ============ 厨房工单 ============

    @GetMapping("/kitchen-orders")
    public Result<List<Map<String, Object>>> kitchenOrders(@RequestParam Long storeId,
                                                            @RequestParam(required = false) String status) {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<KitchenOrder> list = diningService.kitchenOrders(tenantId, storeId, status);
        return Result.success(list.stream().map(diningService::toKitchenVO).toList());
    }

    @PostMapping("/kitchen-orders/{id}/status")
    public Result<Map<String, Object>> updateKitchenStatus(@PathVariable Long id,
                                                           @RequestBody DiningDto.KitchenStatusRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        KitchenOrder ko = diningService.updateKitchenStatus(tenantId, id, req.getStatus());
        return Result.success(diningService.toKitchenVO(ko));
    }
}
