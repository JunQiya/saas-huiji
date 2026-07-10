package com.huiji.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 启用 @Async: 消息发送模拟器异步执行 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
