package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.StoreDto;
import com.huiji.entity.Member;
import com.huiji.entity.Store;
import com.huiji.entity.User;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 门店服务: CRUD, 删除前校验会员/员工。 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuditHelper auditHelper;

    public List<Map<String, Object>> list() {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        return storeRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId).stream()
                .map(this::toVO).toList();
    }

    @Transactional
    public Map<String, Object> create(StoreDto.StoreRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Store s = new Store();
        s.setTenantId(tenantId);
        applyReq(s, req);
        storeRepository.save(s);
        auditHelper.record("新建门店", "store:" + s.getId(), s.getName());
        return toVO(s);
    }

    @Transactional
    public Map<String, Object> update(Long id, StoreDto.StoreRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Store s = storeRepository.findById(id)
                .filter(st -> st.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(st.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "门店不存在"));
        applyReq(s, req);
        storeRepository.save(s);
        auditHelper.record("编辑门店", "store:" + id, s.getName());
        return toVO(s);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Store s = storeRepository.findById(id)
                .filter(st -> st.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(st.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "门店不存在"));
        // 校验: 有会员或员工归属则禁用删除
        boolean hasMember = memberRepository.findAll().stream()
                .anyMatch(m -> m.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(m.getDeleted())
                        && m.getStoreIds() != null && m.getStoreIds().contains(id));
        boolean hasUser = userRepository.findAll().stream()
                .anyMatch(u -> u.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(u.getDeleted())
                        && u.getStoreIds() != null && u.getStoreIds().contains(id));
        if (hasMember || hasUser) {
            throw new BizException(ErrorCode.BIZ_ERROR, "该门店下仍有会员或员工, 无法删除");
        }
        s.setDeleted(true);
        storeRepository.save(s);
        auditHelper.record("删除门店", "store:" + id, s.getName());
    }

    private void applyReq(Store s, StoreDto.StoreRequest req) {
        if (req.getName() != null) s.setName(req.getName());
        if (req.getAddress() != null) s.setAddress(req.getAddress());
        if (req.getPhone() != null) s.setPhone(req.getPhone());
        if (req.getBusinessHours() != null) s.setBusinessHours(req.getBusinessHours());
        if (req.getStatus() != null) s.setStatus(req.getStatus());
    }

    public Map<String, Object> toVO(Store s) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", s.getId());
        vo.put("name", s.getName());
        vo.put("address", s.getAddress());
        vo.put("phone", s.getPhone());
        vo.put("businessHours", s.getBusinessHours());
        vo.put("status", s.getStatus());
        vo.put("createdAt", s.getCreatedAt());
        return vo;
    }
}
