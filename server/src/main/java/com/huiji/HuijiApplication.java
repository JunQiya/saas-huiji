package com.huiji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 星河·会记 SaaS 后端启动入口。
 */
@SpringBootApplication
@EnableScheduling
public class HuijiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuijiApplication.class, args);
    }
}
