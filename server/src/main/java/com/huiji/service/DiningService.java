package com.huiji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.DiningDto;
import com.huiji.entity.DiningTable;
import com.huiji.entity.KitchenOrder;
import com.huiji.entity.MenuCategory;
import com.huiji.entity.Order;
import com.huiji.entity.Product;
import com.huiji.repository.DiningTableRepository;
import com.huiji.repository.KitchenOrderRepository;
import com.huiji.repository.MenuCategoryRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.ProductRepository;
import com.huiji.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 线下门店点餐服务: 桌台管理、菜单分类、厨房工单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiningService {

    private final DiningTableRepository tableRepository;
    private final MenuCategoryRepository categoryRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    @Value("${huiji.h5-domain}")
    private String h5Domain;

    // ============ 桌台 ============

    public List<DiningTable> tables(Long tenantId, Long storeId) {
        return tableRepository.findByTenantIdAndStoreIdAndDeletedFalseOrderBySortOrderAscIdAsc(tenantId, storeId);
    }

    @Transactional
    public DiningTable saveTable(Long tenantId, DiningDto.TableRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "桌名不能为空");
        }
        if (req.getStoreId() == null) {
            throw new BizException(ErrorCode.VALIDATION, "请选择门店");
        }
        DiningTable table;
        if (req.getId() != null) {
            table = tableRepository.findByIdAndTenantIdAndDeletedFalse(req.getId(), tenantId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "桌台不存在"));
        } else {
            table = new DiningTable();
            table.setTenantId(tenantId);
            table.setStatus("IDLE");
        }
        table.setStoreId(req.getStoreId());
        table.setName(req.getName());
        table.setArea(req.getArea());
        table.setSeats(req.getSeats());
        table.setSortOrder(req.getSortOrder());
        return tableRepository.save(table);
    }

    @Transactional
    public void removeTable(Long tenantId, Long id) {
        DiningTable table = tableRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "桌台不存在"));
        if ("OCCUPIED".equals(table.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "占用中的桌台不可删除");
        }
        table.setDeleted(true);
        tableRepository.save(table);
    }

    @Transactional
    public DiningTable occupyTable(Long tenantId, Long tableId) {
        DiningTable table = tableRepository.findByIdAndTenantIdAndDeletedFalse(tableId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "桌台不存在"));
        table.setStatus("OCCUPIED");
        return tableRepository.save(table);
    }

    @Transactional
    public DiningTable freeTable(Long tenantId, Long tableId) {
        DiningTable table = tableRepository.findByIdAndTenantIdAndDeletedFalse(tableId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "桌台不存在"));
        table.setStatus("IDLE");
        return tableRepository.save(table);
    }

    @Transactional
    public String generateQrcode(Long tenantId, Long tableId) {
        DiningTable table = tableRepository.findByIdAndTenantIdAndDeletedFalse(tableId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "桌台不存在"));
        String url = h5Domain + "/#/dining?tableId=" + tableId + "&storeId=" + table.getStoreId();
        table.setQrcode(url);
        tableRepository.save(table);
        return url;
    }

    /** H5 扫码获取桌台信息(公开, 无需登录) */
    public DiningTable getTable(Long tableId) {
        return tableRepository.findById(tableId)
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "桌台不存在"));
    }

    // ============ 菜单分类 ============

    public List<MenuCategory> categories(Long tenantId, Long storeId) {
        return categoryRepository.findByTenantIdAndStoreIdAndDeletedFalseOrderBySortOrderAscIdAsc(tenantId, storeId);
    }

    @Transactional
    public MenuCategory saveCategory(Long tenantId, DiningDto.CategoryRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "分类名称不能为空");
        }
        if (req.getStoreId() == null) {
            throw new BizException(ErrorCode.VALIDATION, "请选择门店");
        }
        MenuCategory cat;
        if (req.getId() != null) {
            cat = categoryRepository.findByIdAndTenantIdAndDeletedFalse(req.getId(), tenantId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "分类不存在"));
        } else {
            cat = new MenuCategory();
            cat.setTenantId(tenantId);
        }
        cat.setStoreId(req.getStoreId());
        cat.setName(req.getName());
        cat.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) {
            cat.setStatus(req.getStatus());
        }
        return categoryRepository.save(cat);
    }

    @Transactional
    public void removeCategory(Long tenantId, Long id) {
        MenuCategory cat = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "分类不存在"));
        cat.setDeleted(true);
        categoryRepository.save(cat);
    }

    @Transactional
    public void bindProductsToCategory(Long tenantId, Long categoryId, List<Long> productIds) {
        categoryRepository.findByIdAndTenantIdAndDeletedFalse(categoryId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "分类不存在"));
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Product> products = productRepository.findByIdInAndTenantIdAndDeletedFalse(productIds, tenantId);
        for (Product p : products) {
            p.setMenuCategoryId(categoryId);
        }
        productRepository.saveAll(products);
    }

    // ============ 厨房工单 ============

    @Transactional
    public KitchenOrder createKitchenOrder(Long tenantId, Long orderId, Long tableId,
                                           String orderType, List<DiningDto.KitchenOrderItem> items) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        KitchenOrder ko = new KitchenOrder();
        ko.setTenantId(tenantId);
        ko.setStoreId(order.getStoreId());
        ko.setOrderId(orderId);
        ko.setTableId(tableId);
        ko.setOrderType(orderType);
        ko.setStatus("PENDING");
        try {
            ko.setItems(objectMapper.writeValueAsString(items));
        } catch (Exception e) {
            log.error("序列化厨房工单明细失败", e);
            ko.setItems("[]");
        }
        return kitchenOrderRepository.save(ko);
    }

    public List<KitchenOrder> kitchenOrders(Long tenantId, Long storeId, String status) {
        if (status != null && !status.isBlank()) {
            return kitchenOrderRepository
                    .findByTenantIdAndStoreIdAndStatusAndDeletedFalseOrderByCreatedAtAsc(tenantId, storeId, status);
        }
        return kitchenOrderRepository
                .findByTenantIdAndStoreIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, storeId);
    }

    @Transactional
    public KitchenOrder updateKitchenStatus(Long tenantId, Long kitchenOrderId, String status) {
        KitchenOrder ko = kitchenOrderRepository.findByIdAndTenantIdAndDeletedFalse(kitchenOrderId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "工单不存在"));
        String cur = ko.getStatus();
        String target = status == null ? null : status.toUpperCase();
        if (!isValidTransition(cur, target)) {
            throw new BizException(ErrorCode.BIZ_ERROR, "工单状态不可从 " + cur + " 变更为 " + target);
        }
        ko.setStatus(target);
        if ("SERVED".equals(target)) {
            ko.setServedAt(LocalDateTime.now());
        }
        return kitchenOrderRepository.save(ko);
    }

    /** H5 会员的点餐工单 */
    public List<KitchenOrder> kitchenOrdersByMember(Long tenantId, Long memberId) {
        return kitchenOrderRepository.findByTenantIdAndMemberId(tenantId, memberId);
    }

    // ============ H5 菜单 ============

    /** H5 菜单(按分类分组) */
    public List<Map<String, Object>> menuGroupedByCategory(Long storeId) {
        Long tenantId = storeRepository.findById(storeId)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .map(s -> s.getTenantId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "门店不存在"));
        List<MenuCategory> cats = categoryRepository
                .findByTenantIdAndStoreIdAndDeletedFalseOrderBySortOrderAscIdAsc(tenantId, storeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MenuCategory cat : cats) {
            if (!"ENABLED".equals(cat.getStatus())) {
                continue;
            }
            List<Product> products = productRepository
                    .findByTenantIdAndMenuCategoryIdAndStatusAndDeletedFalseOrderByIdAsc(
                            tenantId, cat.getId(), "ACTIVE");
            List<Product> filtered = new ArrayList<>();
            for (Product p : products) {
                if (p.getStoreIds() == null || p.getStoreIds().isEmpty()
                        || p.getStoreIds().contains(storeId)) {
                    filtered.add(p);
                }
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("category", toCategoryVO(cat));
            group.put("products", filtered.stream().map(this::toProductVO).toList());
            result.add(group);
        }
        return result;
    }

    /** 工单转 VO(含解析后的 items) */
    public Map<String, Object> toKitchenVO(KitchenOrder ko) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", ko.getId());
        vo.put("storeId", ko.getStoreId());
        vo.put("orderId", ko.getOrderId());
        vo.put("tableId", ko.getTableId());
        vo.put("orderType", ko.getOrderType());
        vo.put("status", ko.getStatus());
        vo.put("items", parseItems(ko.getItems()));
        vo.put("createdAt", ko.getCreatedAt());
        vo.put("servedAt", ko.getServedAt());
        return vo;
    }

    // ============ 内部方法 ============

    private boolean isValidTransition(String cur, String target) {
        if (target == null) return false;
        return switch (cur) {
            case "PENDING" -> "COOKING".equals(target) || "CANCELLED".equals(target);
            case "COOKING" -> "SERVED".equals(target) || "CANCELLED".equals(target);
            default -> false;
        };
    }

    private List<DiningDto.KitchenOrderItem> parseItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析工单明细失败: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> toCategoryVO(MenuCategory cat) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", cat.getId());
        vo.put("name", cat.getName());
        vo.put("sortOrder", cat.getSortOrder());
        return vo;
    }

    private Map<String, Object> toProductVO(Product p) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", p.getId());
        vo.put("name", p.getName());
        vo.put("cover", p.getCover());
        vo.put("price", p.getPrice());
        vo.put("description", p.getDescription());
        vo.put("category", p.getCategory());
        return vo;
    }
}
