package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.dto.ProductDto;
import com.huiji.entity.Product;
import com.huiji.entity.TenantSetting;
import com.huiji.repository.ProductRepository;
import com.huiji.repository.TenantSettingRepository;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.PlanLimitCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 商品/服务: 增删改查、上下架、库存调整。 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final AuditHelper auditHelper;

    /** 计费版商品上限: FREE=30, BASIC=100, GROWTH=500, FLAGSHIP=1000 */
    private static final java.util.Map<String, Integer> PLAN_PRODUCT_LIMITS = new java.util.HashMap<>();
    static {
        PLAN_PRODUCT_LIMITS.put("FREE", 30);
        PLAN_PRODUCT_LIMITS.put("BASIC", 100);
        PLAN_PRODUCT_LIMITS.put("GROWTH", 500);
        PLAN_PRODUCT_LIMITS.put("FLAGSHIP", 1000);
    }

    public PageData<Map<String, Object>> list(String keyword, String category, String storeId, String status,
                                              int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<Product> p = productRepository.search(tenantId, trim(keyword), trim(category), trim(status),
                pageable);
        List<Map<String, Object>> list = p.getContent().stream()
                .filter(prod -> storeId == null || storeId.isBlank() || storeIdMatch(prod, storeId))
                .map(this::toVO)
                .toList();
        return PageData.of(list, (long) list.size(), page, size);
    }

    /** storeIds 是 List<Long>, 在内存里判断是否包含 */
    private boolean storeIdMatch(Product p, String storeId) {
        if (p.getStoreIds() == null || p.getStoreIds().isEmpty()) {
            return true; // 空 = 全店可用
        }
        try {
            long sid = Long.parseLong(storeId);
            return p.getStoreIds().contains(sid);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public Map<String, Object> detail(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        return toVO(p);
    }

    @Transactional
    @PlanLimitCheck("products")
    public Map<String, Object> create(ProductDto.ProductRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "商品名称不能为空");
        }
        if (!"SERVICE".equals(req.getCategory()) && !"GOODS".equals(req.getCategory())) {
            throw new BizException(ErrorCode.VALIDATION, "商品类型必须为 SERVICE 或 GOODS");
        }
        // 计费版配额校验
        checkQuota(tenantId);
        Product p = new Product();
        p.setTenantId(tenantId);
        applyReq(p, req);
        // 商品: 库存默认 0; 服务: 库存留空
        if ("GOODS".equals(req.getCategory()) && p.getStock() == null) {
            p.setStock(0);
        }
        productRepository.save(p);
        auditHelper.record("新增商品", "product:" + p.getId(), p.getName());
        return toVO(p);
    }

    @Transactional
    public Map<String, Object> update(Long id, ProductDto.ProductRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (req.getCategory() != null && !req.getCategory().equals(p.getCategory())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "商品类型不可变更");
        }
        applyReq(p, req);
        productRepository.save(p);
        auditHelper.record("编辑商品", "product:" + id, p.getName());
        return toVO(p);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        p.setDeleted(true);
        productRepository.save(p);
        auditHelper.record("删除商品", "product:" + id, p.getName());
    }

    @Transactional
    public Map<String, Object> changeStatus(Long id, String status) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BizException(ErrorCode.VALIDATION, "状态值无效");
        }
        p.setStatus(status);
        productRepository.save(p);
        auditHelper.record(status.equals("ACTIVE") ? "上架商品" : "下架商品", "product:" + id, p.getName());
        return toVO(p);
    }

    /**
     * 库存调整: SET 直接覆盖 / INC 增量(可负, 不会让库存低于 0)。
     * 仅 GOODS 允许调整库存; SERVICE 抛业务错。
     */
    @Transactional
    public Map<String, Object> adjustStock(Long id, ProductDto.StockRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Product p = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!"GOODS".equals(p.getCategory())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "服务类商品没有库存");
        }
        if (req == null || req.getMode() == null) {
            throw new BizException(ErrorCode.VALIDATION, "调整方式不能为空");
        }
        if (!"SET".equalsIgnoreCase(req.getMode()) && !"INC".equalsIgnoreCase(req.getMode())) {
            throw new BizException(ErrorCode.VALIDATION, "调整方式必须为 SET 或 INC");
        }
        int current = p.getStock() == null ? 0 : p.getStock();
        int next;
        if ("SET".equalsIgnoreCase(req.getMode())) {
            next = req.getValue() == null ? 0 : req.getValue();
        } else {
            long after = (long) current + (req.getValue() == null ? 0 : req.getValue());
            if (after < 0) throw new BizException(ErrorCode.BIZ_ERROR, "库存调整后不能小于 0");
            next = (int) after;
        }
        p.setStock(next);
        productRepository.save(p);
        auditHelper.record("调整库存", "product:" + id,
                p.getName() + " " + req.getMode() + " " + req.getValue() + " -> " + next);
        return toVO(p);
    }

    /** 校验库存并扣减(订单创建时由 OrderService 调用, 失败抛错回滚) */
    @Transactional
    public void deductStockTx(Product p, int qty) {
        if (!"GOODS".equals(p.getCategory())) return;
        if (qty <= 0) throw new BizException(ErrorCode.VALIDATION, "数量必须为正");
        int current = p.getStock() == null ? 0 : p.getStock();
        if (current < qty) {
            throw new BizException(ErrorCode.BIZ_ERROR,
                    "商品「" + p.getName() + "」库存不足, 当前库存 " + current);
        }
        p.setStock(current - qty);
        p.setSoldCount((p.getSoldCount() == null ? 0 : p.getSoldCount()) + qty);
        productRepository.save(p);
    }

    /** 收银台取上架商品(SERVICE/GOODS) */
    public List<Map<String, Object>> listActive(String category) {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<Product> rows = productRepository.listActive(tenantId, category);
        return rows.stream().map(this::toVO).toList();
    }

    /** 内部计费版校验 */
    private void checkQuota(Long tenantId) {
        TenantSetting setting = tenantSettingRepository.findByTenantId(tenantId).orElse(null);
        String plan = setting == null ? "FREE" : planName(setting);
        Integer max = PLAN_PRODUCT_LIMITS.get(plan);
        if (max == null) return;
        long current = productRepository.countByTenantIdAndDeletedFalse(tenantId);
        if (current >= max) {
            throw new BizException(ErrorCode.PLAN_LIMIT,
                    "当前套餐下商品已达上限(" + max + "), 请升级套餐");
        }
    }

    /** 解析套餐名: 当前实现为 FREE, 未来可从 TenantSetting 读取 tenantPlan 字段 */
    private String planName(TenantSetting s) {
        return "FREE";
    }

    private void applyReq(Product p, ProductDto.ProductRequest req) {
        if (req.getName() != null) p.setName(req.getName());
        if (req.getCategory() != null) p.setCategory(req.getCategory());
        if (req.getCover() != null) p.setCover(req.getCover());
        if (req.getPrice() != null) p.setPrice(req.getPrice());
        if (req.getCostPrice() != null) p.setCostPrice(req.getCostPrice());
        if (req.getStock() != null) p.setStock(req.getStock());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getStoreIds() != null) p.setStoreIds(req.getStoreIds());
    }

    public Map<String, Object> toVO(Product p) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", p.getId());
        vo.put("name", p.getName());
        vo.put("category", p.getCategory());
        vo.put("cover", p.getCover());
        vo.put("price", p.getPrice());
        vo.put("costPrice", p.getCostPrice());
        vo.put("stock", p.getStock());
        vo.put("status", p.getStatus());
        vo.put("soldCount", p.getSoldCount());
        vo.put("description", p.getDescription());
        vo.put("storeIds", p.getStoreIds() == null ? new ArrayList<>() : p.getStoreIds());
        vo.put("createdAt", p.getCreatedAt());
        return vo;
    }

    private String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
