package com.huiji.controller;

import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.ProductDto;
import com.huiji.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品/服务: CRUD + 上下架 + 库存调整。
 *  GET  /api/products                列表(分页)
 *  GET  /api/products/active         收银台可选商品(ACTIVE)
 *  GET  /api/products/{id}           详情
 *  POST /api/products               新建
 *  PUT  /api/products/{id}           更新
 *  DELETE /api/products/{id}        逻辑删除
 *  PUT  /api/products/{id}/status    上下架 {status}
 *  PUT  /api/products/{id}/stock     库存调整 {mode, value}
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Result<PageData<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) Long storeId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(productService.list(keyword, category,
                storeId == null ? null : String.valueOf(storeId), status, page, size));
    }

    @GetMapping("/active")
    public Result<List<Map<String, Object>>> active(@RequestParam(required = false) String category,
                                                     @RequestParam(required = false) Long storeId) {
        return Result.success(productService.listActive(category, storeId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody ProductDto.ProductRequest req) {
        return Result.success(productService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody ProductDto.ProductRequest req) {
        return Result.success(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Map<String, Object>> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(productService.changeStatus(id, body == null ? null : body.get("status")));
    }

    /** 仅更新适用门店(门店管理-商品配置) */
    @PutMapping("/{id}/stores")
    public Result<Map<String, Object>> updateStores(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object ids = body == null ? null : body.get("storeIds");
        List<Long> storeIds = new java.util.ArrayList<>();
        if (ids instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) storeIds.add(n.longValue());
                else if (o != null) storeIds.add(Long.parseLong(o.toString()));
            }
        }
        return Result.success(productService.updateStoreIds(id, storeIds));
    }

    @PutMapping("/{id}/stock")
    public Result<Map<String, Object>> stock(@PathVariable Long id, @Valid @RequestBody ProductDto.StockRequest req) {
        return Result.success(productService.adjustStock(id, req));
    }
}
