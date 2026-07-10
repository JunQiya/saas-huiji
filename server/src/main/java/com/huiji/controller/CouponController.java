package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.CouponDto;
import com.huiji.service.CouponService;
import jakarta.validation.Valid;
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

/** 优惠券接口 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return Result.success(couponService.list(status, type));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody CouponDto.CouponRequest req) {
        return Result.success(couponService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody CouponDto.CouponRequest req) {
        return Result.success(couponService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/grant")
    public Result<Map<String, Object>> grant(@PathVariable Long id, @RequestBody CouponDto.GrantRequest req) {
        return Result.success(couponService.grant(id, req));
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        couponService.stop(id);
        return Result.success();
    }

    @GetMapping("/{id}/records")
    public Result<List<Map<String, Object>>> records(@PathVariable Long id) {
        return Result.success(couponService.records(id));
    }

    @PostMapping("/verify")
    public Result<Map<String, Object>> verify(@Valid @RequestBody CouponDto.VerifyRequest req) {
        return Result.success(couponService.verify(req));
    }

    /** 核销码展示(只查不核销) */
    @GetMapping("/records/{code}/qrcode")
    public Result<Map<String, Object>> display(@PathVariable String code) {
        return Result.success(couponService.display(code));
    }
}