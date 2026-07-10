package com.huiji.config;

import com.huiji.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 报表定时调度: 每天 02:00 扫描到期任务 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportSchedulerHook {

    private final ReportService reportService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDueReports() {
        try {
            reportService.runDue();
        } catch (Exception e) {
            log.error("ReportSchedulerHook 异常", e);
        }
    }
}
