package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.EmployeeDto;
import com.huiji.entity.User;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.UserRepository;
import com.huiji.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 员工服务: CRUD、重置密码、业绩统计。 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final String PWD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RAND = new SecureRandom();

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditHelper auditHelper;

    public List<Map<String, Object>> list(String storeId, String role) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        String sid = (storeId == null || storeId.isBlank()) ? null : storeId;
        return userRepository.listByTenant(tenantId, sid, role).stream()
                .map(this::toVO).toList();
    }

    @Transactional
    public Map<String, Object> create(EmployeeDto.EmployeeRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        // 用户名租户内唯一(数据库层另有全局唯一约束, 单租户部署可忽略)
        if (userRepository.existsByUsernameAndTenantIdAndDeletedFalse(req.getUsername(), tenantId)) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User u = new User();
        u.setTenantId(tenantId);
        u.setUsername(req.getUsername());
        // 未提供密码时生成随机临时密码, 禁止默认弱口令
        String rawPwd = (req.getPassword() == null || req.getPassword().isBlank())
                ? randomPassword() : req.getPassword();
        u.setPassword(passwordEncoder.encode(rawPwd));
        u.setName(req.getName());
        u.setPhone(req.getPhone());
        u.setRole(req.getRole());
        u.setStoreIds(req.getStoreIds() == null ? new ArrayList<>() : req.getStoreIds());
        userRepository.save(u);
        auditHelper.record("新增员工", "user:" + u.getId(), u.getUsername());
        Map<String, Object> vo = toVO(u);
        vo.put("initialPassword", rawPwd);
        return vo;
    }

    @Transactional
    public Map<String, Object> update(Long id, EmployeeDto.EmployeeUpdate req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        User u = userRepository.findById(id)
                .filter(x -> x.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "员工不存在"));
        if (req.getName() != null) u.setName(req.getName());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getStoreIds() != null) u.setStoreIds(req.getStoreIds());
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        userRepository.save(u);
        auditHelper.record("编辑员工", "user:" + id, u.getUsername());
        return toVO(u);
    }

    @Transactional
    public String resetPassword(Long id, EmployeeDto.PasswordReset req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        User u = userRepository.findById(id)
                .filter(x -> x.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "员工不存在"));
        // 未提供新密码时生成随机临时密码, 返回给管理员转交员工, 禁止默认弱口令
        String pwd = (req == null || req.getPassword() == null || req.getPassword().isBlank())
                ? randomPassword() : req.getPassword();
        u.setPassword(passwordEncoder.encode(pwd));
        userRepository.save(u);
        auditHelper.record("重置员工密码", "user:" + id, u.getUsername());
        return pwd;
    }

    @Transactional
    public void disable(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        User u = userRepository.findById(id)
                .filter(x -> x.getTenantId().equals(tenantId) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "员工不存在"));
        u.setStatus("DISABLED");
        userRepository.save(u);
        auditHelper.record("禁用员工", "user:" + id, u.getUsername());
    }

    /** 员工业绩: 近 6 个月消费金额与笔数(以其为操作人) */
    public List<Map<String, Object>> performance(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        userRepository.findById(id)
                .filter(x -> x.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "员工不存在"));
        List<Map<String, Object>> result = new ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            java.time.LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            java.time.LocalDateTime end = start.plusMonths(1);
            Long amount = walletRepository.sumConsume(tenantId, start, end, null);
            Long count = walletRepository.countConsume(tenantId, start, end, null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", start.getYear() + "-" + String.format("%02d", start.getMonthValue()));
            row.put("amount", amount == null ? 0L : amount);
            row.put("count", count == null ? 0L : count);
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> toVO(User u) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", u.getId());
        vo.put("username", u.getUsername());
        vo.put("name", u.getName());
        vo.put("phone", u.getPhone());
        vo.put("role", u.getRole());
        vo.put("storeIds", u.getStoreIds());
        vo.put("status", u.getStatus());
        vo.put("createdAt", u.getCreatedAt());
        return vo;
    }

    /** 生成 8 位随机密码(去易混字符) */
    private static String randomPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(PWD_CHARS.charAt(RAND.nextInt(PWD_CHARS.length())));
        }
        return sb.toString();
    }
}
