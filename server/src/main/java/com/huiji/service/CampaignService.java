package com.huiji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.CampaignDto;
import com.huiji.entity.Campaign;
import com.huiji.entity.Member;
import com.huiji.repository.CampaignRepository;
import com.huiji.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 营销活动服务: CRUD、启停、预览命中、统计。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final MemberRepository memberRepository;
    private final AuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> list(String status) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        return campaignRepository.listByTenant(tenantId, status).stream()
                .map(this::toVO).toList();
    }

    @Transactional
    public Map<String, Object> create(CampaignDto.CampaignRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Campaign c = new Campaign();
        c.setTenantId(tenantId);
        applyReq(c, req);
        campaignRepository.save(c);
        auditHelper.record("新建活动", "campaign:" + c.getId(), c.getName());
        return toVO(c);
    }

    @Transactional
    public Map<String, Object> update(Long id, CampaignDto.CampaignRequest req) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Campaign c = campaignRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        applyReq(c, req);
        campaignRepository.save(c);
        auditHelper.record("编辑活动", "campaign:" + id, c.getName());
        return toVO(c);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Campaign c = campaignRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        c.setDeleted(true);
        campaignRepository.save(c);
        auditHelper.record("删除活动", "campaign:" + id, c.getName());
    }

    @Transactional
    public Map<String, Object> toggle(Long id, Boolean enabled) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Campaign c = campaignRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        c.setEnabled(enabled);
        campaignRepository.save(c);
        auditHelper.record("活动启停", "campaign:" + id, enabled ? "启用" : "停用");
        return toVO(c);
    }

    /** SOP 可视化预览: 触发/条件/动作 + 人群分布 + 渠道 + 预计触达/成本 */
    public Map<String, Object> preview(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Campaign c = campaignRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        List<Member> members = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));
        List<Member> hits = new java.util.ArrayList<>();
        for (Member m : members) {
            if (match(c, m)) hits.add(m);
        }
        // 人群分布(按性别/等级简单聚合)
        Map<String, Integer> bd = new LinkedHashMap<>();
        for (Member m : hits) {
            String key = m.getGender() == null ? "未知" : ("M".equals(m.getGender()) ? "男性" : ("F".equals(m.getGender()) ? "女性" : "未知"));
            bd.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> breakdown = new java.util.ArrayList<>();
        for (var e : bd.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", e.getKey());
            row.put("count", e.getValue());
            breakdown.add(row);
        }
        Map<String, Object> audience = new LinkedHashMap<>();
        audience.put("count", hits.size());
        audience.put("breakdown", breakdown);
        // SOP 步骤
        List<Map<String, Object>> sop = buildSop(c);
        // 渠道
        List<String> channels = new java.util.ArrayList<>();
        if (c.getChannel() != null) channels.add(c.getChannel());
        // 预计成本: 短信 0.05 元/条
        double cost = "SMS".equals(c.getChannel()) ? hits.size() * 0.05 : 0;
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("audience", audience);
        vo.put("channels", channels);
        vo.put("estimatedCost", Math.round(cost * 100.0) / 100.0);
        vo.put("estimatedReach", (long) (hits.size() * 0.9));
        vo.put("sop", sop);
        return vo;
    }

    /** 根据活动类型判断会员是否命中 */
    private boolean match(Campaign c, Member m) {
        return switch (c.getType()) {
            case "BIRTHDAY" -> m.getBirthday() != null;
            case "DORMANT" -> {
                java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(90);
                yield m.getLastConsumeAt() == null || m.getLastConsumeAt().isBefore(cutoff);
            }
            case "REPURCHASE" -> m.getConsumeCount() != null && m.getConsumeCount() >= 1;
            default -> true;
        };
    }

    /** 构造 SOP 步骤 */
    private List<Map<String, Object>> buildSop(Campaign c) {
        List<Map<String, Object>> steps = new java.util.ArrayList<>();
        String trigger = switch (c.getType()) {
            case "BIRTHDAY" -> "生日当天";
            case "DORMANT" -> "客户超过 90 天未消费";
            case "REPURCHASE" -> "客户消费后 7 天";
            default -> c.getTrigger() == null ? "手动触发" : c.getTrigger();
        };
        String filter = c.getAudience() == null || c.getAudience().isBlank() ? "全部会员" : c.getAudience();
        String action = switch (c.getType()) {
            case "BIRTHDAY" -> "发送生日专属礼券";
            case "DORMANT" -> "发送唤醒代金券";
            case "REPURCHASE" -> "发送复购优惠券";
            default -> c.getContent() == null || c.getContent().isBlank() ? "发送触达消息" : c.getContent();
        };
        addStep(steps, "trigger", trigger);
        addStep(steps, "filter", "筛选: " + filter);
        addStep(steps, "action", action);
        return steps;
    }

    private void addStep(List<Map<String, Object>> list, String type, String text) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", type);
        s.put("text", text);
        list.add(s);
    }

    /** 统计: 触发/触达/转化 */
    public Map<String, Object> stats(Long id) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Campaign c = campaignRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("campaignId", id);
        vo.put("triggered", c.getStatTriggered());
        vo.put("reached", c.getStatReached());
        vo.put("converted", c.getStatConverted());
        vo.put("enabled", c.getEnabled());
        return vo;
    }

    /** 按活动类型估算命中会员数 */
    private long estimateHit(Campaign c, Long tenantId) {
        List<Member> members = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));
        return switch (c.getType()) {
            case "BIRTHDAY" -> members.stream().filter(m -> m.getBirthday() != null).count();
            case "DORMANT" -> {
                java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(90);
                yield members.stream().filter(m -> m.getLastConsumeAt() == null || m.getLastConsumeAt().isBefore(cutoff)).count();
            }
            case "REPURCHASE" -> members.stream().filter(m -> m.getConsumeCount() != null && m.getConsumeCount() >= 1).count();
            default -> members.size();
        };
    }

    private void applyReq(Campaign c, CampaignDto.CampaignRequest req) {
        if (req.getName() != null) c.setName(req.getName());
        if (req.getType() != null) c.setType(req.getType());
        if (req.getTrigger() != null) c.setTrigger(req.getTrigger());
        if (req.getAudience() != null) c.setAudience(req.getAudience());
        if (req.getChannel() != null) c.setChannel(req.getChannel());
        if (req.getContent() != null) c.setContent(req.getContent());
        if (req.getStartAt() != null) c.setStartAt(req.getStartAt());
        if (req.getEndAt() != null) c.setEndAt(req.getEndAt());
        if (req.getEnabled() != null) c.setEnabled(req.getEnabled());
        if (req.getSopSteps() != null) {
            try {
                c.setSopSteps(objectMapper.writeValueAsString(req.getSopSteps()));
            } catch (Exception e) {
                log.warn("序列化 SOP 步骤失败: {}", e.getMessage());
                c.setSopSteps("[]");
            }
        }
    }

    public Map<String, Object> toVO(Campaign c) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("name", c.getName());
        vo.put("type", c.getType());
        vo.put("trigger", c.getTrigger());
        vo.put("audience", c.getAudience());
        vo.put("channel", c.getChannel());
        vo.put("content", c.getContent());
        vo.put("startAt", c.getStartAt());
        vo.put("endAt", c.getEndAt());
        vo.put("enabled", c.getEnabled());
        vo.put("statTriggered", c.getStatTriggered());
        vo.put("statReached", c.getStatReached());
        vo.put("statConverted", c.getStatConverted());
        vo.put("sopSteps", parseSopSteps(c.getSopSteps()));
        vo.put("createdAt", c.getCreatedAt());
        return vo;
    }

    /** 反序列化 SOP 步骤 */
    private List<CampaignDto.SopStep> parseSopSteps(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CampaignDto.SopStep>>() {});
        } catch (Exception e) {
            log.warn("解析 SOP 步骤失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
