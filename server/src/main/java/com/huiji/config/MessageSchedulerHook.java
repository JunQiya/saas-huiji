package com.huiji.config;

import com.huiji.entity.MessageTask;
import com.huiji.repository.MessageTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息任务自动钩子: 每天 02:00 扫描"待发送"任务并触发。
 * 由于会员/优惠券触达由活动服务承担, 此钩子更像是兜底: 重新触发因异常被搁置的 PENDING 任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSchedulerHook {

    private final MessageTaskRepository messageTaskRepository;
    // 注入消息服务会循环依赖, 通过 ApplicationContext 解析
    private final org.springframework.context.ApplicationContext ctx;

    /** 每天 02:00 执行 */
    @Scheduled(cron = "0 0 2 * * *")
    public void scanPendingTasks() {
        try {
            List<MessageTask> pending = messageTaskRepository.findAll().stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                    .filter(t -> "PENDING".equals(t.getStatus()))
                    .filter(t -> t.getScheduledAt() == null || t.getScheduledAt().isBefore(LocalDateTime.now()))
                    .toList();
            if (pending.isEmpty()) {
                log.info("MessageSchedulerHook: 暂无待发送任务");
                return;
            }
            log.info("MessageSchedulerHook: 发现 {} 个待发送任务, 正在触发", pending.size());
            com.huiji.service.MessageService svc = ctx.getBean(com.huiji.service.MessageService.class);
            for (MessageTask t : pending) {
                svc.asyncSend(t.getId());
            }
        } catch (Exception e) {
            log.error("MessageSchedulerHook 异常", e);
        }
    }
}
