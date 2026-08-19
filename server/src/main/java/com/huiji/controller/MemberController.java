package com.huiji.controller;

import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.MemberDto;
import com.huiji.security.PreAllowed;
import com.huiji.service.MemberService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 会员接口 (敏感操作: 储值/消费/积分/等级/批量/删除/导入 仅超管与店长) */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@PreAllowed({"TENANT_ADMIN", "STORE_MANAGER"})
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<PageData<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) List<Long> storeIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(memberService.list(keyword, level, tag, storeIds, page, size));
    }

    @PostMapping
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> create(@Valid @RequestBody MemberDto.MemberRequest req) {
        return Result.success(memberService.create(req));
    }

    @GetMapping("/{id}")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(memberService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody MemberDto.MemberRequest req) {
        return Result.success(memberService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/transactions")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<PageData<Map<String, Object>>> transactions(
            @PathVariable Long id,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(memberService.transactions(id, type, page, size));
    }

    @PostMapping("/{id}/recharge")
    public Result<Map<String, Object>> recharge(@PathVariable Long id, @RequestBody MemberDto.RechargeRequest req) {
        return Result.success(memberService.recharge(id, req));
    }

    /** 储值退款(运营): 扣回余额并记 REFUND 流水 */
    @PostMapping("/{id}/refund")
    public Result<Map<String, Object>> refund(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        long amount = body == null || body.get("amount") == null ? 0 : Long.parseLong(String.valueOf(body.get("amount")));
        String reason = body == null ? null : (String) body.get("reason");
        return Result.success(memberService.refundBalance(id, amount, reason));
    }

    @PostMapping("/{id}/consume")
    public Result<Map<String, Object>> consume(@PathVariable Long id, @RequestBody MemberDto.ConsumeRequest req) {
        return Result.success(memberService.consume(id, req));
    }

    @PostMapping("/{id}/tags")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Void> tags(@PathVariable Long id, @RequestBody MemberDto.TagsRequest req) {
        memberService.updateTags(id, req.getTags());
        return Result.success();
    }

    @GetMapping("/{id}/coupons")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<List<Map<String, Object>>> coupons(@PathVariable Long id) {
        return Result.success(memberService.memberCoupons(id));
    }

    /** 批量设置标签 */
    @PostMapping("/batch/tags")
    public Result<Void> batchTags(@RequestBody MemberDto.BatchTagsRequest req) {
        memberService.batchSetTags(req.getMemberIds(), req.getTags());
        return Result.success();
    }

    /** 批量调整等级 */
    @PostMapping("/batch/level")
    public Result<Void> batchLevel(@RequestBody MemberDto.BatchLevelRequest req) {
        memberService.batchSetLevel(req.getMemberIds(), req.getLevel());
        return Result.success();
    }

    /** 调整单个会员积分 */
    @PostMapping("/{id}/points")
    public Result<Map<String, Object>> adjustPoints(@PathVariable Long id, @RequestBody MemberDto.PointsAdjustRequest req) {
        return Result.success(memberService.adjustPoints(id, req));
    }

    /** 修改单个会员等级 */
    @PutMapping("/{id}/level")
    public Result<Map<String, Object>> setLevel(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer level;
        try {
            level = body.get("level") == null ? null : Integer.parseInt(body.get("level").toString());
        } catch (NumberFormatException e) {
            throw new com.huiji.common.BizException(com.huiji.common.ErrorCode.VALIDATION, "等级格式不正确");
        }
        return Result.success(memberService.setLevel(id, level));
    }

    /** CSV 导入 */
    @PostMapping("/import")
    public Result<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.success(memberService.importCsv(file.getInputStream()));
    }

    /** CSV 导出 */
    @GetMapping("/export")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String tag) {
        byte[] data = memberService.exportCsv(keyword, level, tag);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "members-" + System.currentTimeMillis() + ".csv");
        return new ResponseEntity<>(data, headers, 200);
    }

    /** 会员画像 */
    @GetMapping("/{id}/profile")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> profile(@PathVariable Long id) {
        return Result.success(memberService.profile(id));
    }
}
