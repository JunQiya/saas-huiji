package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.entity.Agent;
import com.huiji.entity.WxAccount;
import com.huiji.repository.AgentRepository;
import com.huiji.repository.OrderRepository;
import com.huiji.repository.WxAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 代理商管理 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRepository agentRepository;
    private final WxAccountRepository wxAccountRepository;
    private final OrderRepository orderRepository;

    /** 列表 */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Agent> agents = agentRepository.findAll();
        return Result.success(agents.stream().map(this::toView).collect(Collectors.toList()));
    }

    /** 创建 */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Agent agent = new Agent();
        applyBody(agent, body);
        agentRepository.save(agent);
        return Result.success(toView(agent));
    }

    /** 更新 */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("代理商不存在"));
        applyBody(agent, body);
        agentRepository.save(agent);
        return Result.success(toView(agent));
    }

    /** 删除(软删除) */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentRepository.findById(id).ifPresent(agent -> {
            agent.setDeleted(true);
            agentRepository.save(agent);
        });
        return Result.success();
    }

    /** 代理商业绩统计: 挂靠商家数、总交易额、抽佣金额 */
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("代理商不存在"));
        List<WxAccount> accounts = wxAccountRepository.findByAgentIdAndStatus(id, "ENABLED");
        List<Long> tenantIds = accounts.stream()
                .map(WxAccount::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long tenantCount = tenantIds.size();
        long totalAmount = tenantIds.isEmpty() ? 0L : orderRepository.sumPaidByTenantIds(tenantIds);
        int rate = agent.getCommissionRate() == null ? 0 : agent.getCommissionRate();
        long commission = totalAmount * rate / 1000;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tenantCount", tenantCount);
        stats.put("totalAmount", totalAmount);
        stats.put("commissionRate", rate);
        stats.put("commission", commission);
        return Result.success(stats);
    }

    private void applyBody(Agent agent, Map<String, Object> body) {
        if (body == null) return;
        if (body.containsKey("name")) agent.setName(str(body.get("name")));
        if (body.containsKey("contactName")) agent.setContactName(str(body.get("contactName")));
        if (body.containsKey("contactPhone")) agent.setContactPhone(str(body.get("contactPhone")));
        if (body.containsKey("appId")) agent.setAppId(str(body.get("appId")));
        if (body.containsKey("appSecret")) agent.setAppSecret(str(body.get("appSecret")));
        if (body.containsKey("mchId")) agent.setMchId(str(body.get("mchId")));
        if (body.containsKey("mchKey")) agent.setMchKey(str(body.get("mchKey")));
        if (body.containsKey("commissionRate")) agent.setCommissionRate(toInt(body.get("commissionRate")));
        if (body.containsKey("status")) agent.setStatus(str(body.get("status")));
    }

    private Map<String, Object> toView(Agent agent) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", agent.getId());
        vo.put("name", agent.getName());
        vo.put("contactName", agent.getContactName());
        vo.put("contactPhone", agent.getContactPhone());
        vo.put("appId", agent.getAppId());
        vo.put("appSecret", mask(agent.getAppSecret()));
        vo.put("mchId", agent.getMchId());
        vo.put("mchKey", mask(agent.getMchKey()));
        vo.put("commissionRate", agent.getCommissionRate());
        vo.put("status", agent.getStatus());
        vo.put("createdAt", agent.getCreatedAt());
        vo.put("updatedAt", agent.getUpdatedAt());
        return vo;
    }

    private static String mask(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() <= 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
