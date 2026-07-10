package com.huiji.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 全局启用 Spring @Scheduled, 供消息中心/报表等定时任务使用。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
