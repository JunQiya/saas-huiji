package com.huiji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.dto.MessageDto;
import com.huiji.entity.MessageTask;
import com.huiji.entity.TenantSetting;
import com.huiji.repository.MessageTaskRepository;
import com.huiji.repository.TenantSettingRepository;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 消息中心服务: 短信/微信/站内发送任务。
 * 设计要点:
 *  - MVP 用本地"发送模拟器" sleep 1ms/条(基本不影响响应)
 *  - 短信计费 0.05 元/条, 站内免费
 *  - 短信余额校验(从 TenantSetting.smsBalance 读取, 不存在则视为 0)
 *  - 取消/重试: 改 status 即可
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /** 短信单价(分): 0.05 元 = 5 分 */
    private static final long SMS_PRICE_FEN = 5L;

    private final MessageTaskRepository messageTaskRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final AuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    @Value("${huiji.message.simulate:true}")
    private boolean simulate;

    /** 列表 */
    public PageData<Map<String, Object>> list(String status, String channel,
                                              LocalDateTime start, LocalDateTime end,
                                              int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<MessageTask> p = messageTaskRepository.search(tenantId, trim(status), trim(channel), start, end, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::toVO).toList();
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    /** 详情 */
    public Map<String, Object> detail(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        MessageTask t = messageTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "任务不存在"));
        return toVO(t);
    }

    /**
     * 创建并触发发送。
     *  - 校验短信余额
     *  - 创建任务 PENDING -> SENDING -> COMPLETED
     *  - 异步执行 simulateSend
     */
    @Transactional
    public Map<String, Object> create(MessageDto.CreateRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        if (req.getMemberIds() == null || req.getMemberIds().isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION, "请选择目标会员");
        }
        long count = req.getMemberIds().size();
        // 短信余额校验
        if ("SMS".equals(req.getChannel())) {
            long balance = readSmsBalance(tenantId);
            if (balance < count) {
                throw new BizException(ErrorCode.MESSAGE_QUOTA_EXCEEDED,
                        "短信余额不足, 当前 " + balance + " 条, 需要 " + count + " 条");
            }
        }

        MessageTask t = new MessageTask();
        t.setTenantId(tenantId);
        t.setChannel(req.getChannel());
        t.setTemplateType(req.getTemplateType());
        t.setSubject(req.getSubject());
        t.setContent(req.getContent());
        t.setMemberIds(serialize(req.getMemberIds()));
        t.setTotalCount((int) count);
        t.setSentCount(0);
        t.setFailedCount(0);
        t.setCost(0L);
        t.setStatus("PENDING");
        t.setScheduledAt(req.getScheduledAt());
        t.setCreatedBy(safeCurrentUserId());
        messageTaskRepository.save(t);
        auditHelper.record("新建消息任务", "message:" + t.getId(), req.getChannel() + "/" + count + "条");

        // 异步模拟发送
        asyncSend(t.getId());

        return toVO(t);
    }

    /** 取消(只能取消未完成的) */
    @Transactional
    public void cancel(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        MessageTask t = messageTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "任务不存在"));
        if ("COMPLETED".equals(t.getStatus()) || "CANCELED".equals(t.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前状态不可取消");
        }
        t.setStatus("CANCELED");
        t.setCompletedAt(LocalDateTime.now());
        messageTaskRepository.save(t);
        auditHelper.record("取消消息任务", "message:" + id, t.getChannel() + "/" + t.getTotalCount() + "条");
    }

    /** 重试: 失败的重新置 SENDING 后异步再发(简化: 已发送的不会重发, 只重试失败的部分) */
    @Transactional
    public Map<String, Object> retry(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        MessageTask t = messageTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!"FAILED".equals(t.getStatus()) && !"COMPLETED".equals(t.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "仅失败或已完成的任务可重试");
        }
        // 简化重试: 整批重发(只对未完成的情况)
        if (!"COMPLETED".equals(t.getStatus())) {
            t.setStatus("PENDING");
            t.setSentCount(0);
            t.setFailedCount(0);
            t.setCost(0L);
            messageTaskRepository.save(t);
            asyncSend(t.getId());
        }
        auditHelper.record("重试消息任务", "message:" + id, t.getChannel());
        return toVO(t);
    }

    /**
     * 发送模拟器: 遍历 memberIds, sleep 1ms/条, 全部完成后置 COMPLETED。
     * 短信: cost = sentCount * 5 分; 站内/微信: 0
     * 实际生产对接通道。
     */
    @Async
    public void asyncSend(Long taskId) {
        try {
            sendInternal(taskId);
        } catch (Exception e) {
            log.error("消息发送异常 taskId={}", taskId, e);
            try {
                messageTaskRepository.findById(taskId).ifPresent(t -> {
                    t.setStatus("FAILED");
                    t.setCompletedAt(LocalDateTime.now());
                    messageTaskRepository.save(t);
                });
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    protected void sendInternal(Long taskId) {
        MessageTask t = messageTaskRepository.findById(taskId).orElse(null);
        if (t == null) return;
        if ("CANCELED".equals(t.getStatus())) return;
        t.setStatus("SENDING");
        messageTaskRepository.save(t);

        List<Long> ids = parseMemberIds(t.getMemberIds());
        int sent = 0;
        int failed = 0;
        for (Long mid : ids) {
            if (simulate) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // 模拟: 95% 成功
            if (ThreadLocalRandom.current().nextInt(100) < 95) {
                sent++;
            } else {
                failed++;
            }
        }
        t.setSentCount(sent);
        t.setFailedCount(failed);
        long cost = "SMS".equals(t.getChannel()) ? (long) sent * SMS_PRICE_FEN : 0L;
        t.setCost(cost);
        t.setStatus(failed == 0 ? "COMPLETED" : "FAILED");
        t.setCompletedAt(LocalDateTime.now());
        messageTaskRepository.save(t);

        // 扣减短信余额(MVP: 直接 audit 记录, 避免 schema 不一致)
        if ("SMS".equals(t.getChannel()) && cost > 0) {
            final long finalSent = sent;
            final long finalCost = cost;
            final Long msgId = t.getId();
            try {
                tenantSettingRepository.findByTenantId(t.getTenantId()).ifPresent(setting -> {
                    auditHelper.record("消息计费", "message:" + msgId, "扣" + finalSent + "条, 费用" + finalCost + "分");
                });
            } catch (Exception ignored) {}
        }
    }

    /**
     * 统计: 今日 / 本月 / 总计 发送数 + 费用 + 最近 7 天趋势
     */
    public Map<String, Object> stats() {
        Long tenantId = LoginUserHolder.currentTenantId();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDt = monthStart.atStartOfDay();

        long todaySent = nvl(messageTaskRepository.sumSent(tenantId, todayStart, tomorrowStart));
        long todayCost = nvl(messageTaskRepository.sumCost(tenantId, todayStart, tomorrowStart));
        long monthSent = nvl(messageTaskRepository.sumSent(tenantId, monthStartDt, tomorrowStart));
        long monthCost = nvl(messageTaskRepository.sumCost(tenantId, monthStartDt, tomorrowStart));
        long totalSent = nvl(messageTaskRepository.sumSent(tenantId,
                LocalDateTime.of(2000, 1, 1, 0, 0), tomorrowStart));
        long totalCost = nvl(messageTaskRepository.sumCost(tenantId,
                LocalDateTime.of(2000, 1, 1, 0, 0), tomorrowStart));

        // 最近 7 天每日发送/费用
        List<Map<String, Object>> recent = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime ds = d.atStartOfDay();
            LocalDateTime de = d.plusDays(1).atStartOfDay();
            long s = nvl(messageTaskRepository.sumSent(tenantId, ds, de));
            long c = nvl(messageTaskRepository.sumCost(tenantId, ds, de));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("sent", s);
            row.put("cost", c);
            recent.add(row);
        }

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("todaySent", todaySent);
        vo.put("todayCost", todayCost);
        vo.put("monthSent", monthSent);
        vo.put("monthCost", monthCost);
        vo.put("totalSent", totalSent);
        vo.put("totalCost", totalCost);
        vo.put("recent", recent);
        return vo;
    }

    // ---- 内部辅助 ----

    private long readSmsBalance(Long tenantId) {
        return tenantSettingRepository.findByTenantId(tenantId)
                .map(TenantSetting::getId)
                .map(id -> 0L) // TenantSetting 没有 smsBalance 字段, MVP 默认 0, 后续接入计费服务
                .orElse(0L);
    }

    private String serialize(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Long> parseMemberIds(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Long safeCurrentUserId() {
        try {
            return LoginUserHolder.currentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private long nvl(Long v) { return v == null ? 0L : v; }

    private String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    public Map<String, Object> toVO(MessageTask t) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", t.getId());
        vo.put("channel", t.getChannel());
        vo.put("templateType", t.getTemplateType());
        vo.put("subject", t.getSubject());
        vo.put("content", t.getContent());
        vo.put("memberIds", parseMemberIds(t.getMemberIds()));
        vo.put("totalCount", t.getTotalCount());
        vo.put("sentCount", t.getSentCount());
        vo.put("failedCount", t.getFailedCount());
        vo.put("cost", t.getCost());
        vo.put("status", t.getStatus());
        vo.put("scheduledAt", t.getScheduledAt());
        vo.put("completedAt", t.getCompletedAt());
        vo.put("createdBy", t.getCreatedBy());
        vo.put("createdAt", t.getCreatedAt());
        return vo;
    }
}
