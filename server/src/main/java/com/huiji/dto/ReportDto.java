package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 报表订阅 DTO */
public class ReportDto {

    @Data
    public static class ReportRequest {
        @NotBlank(message = "报表名称不能为空")
        private String name;

        @NotBlank(message = "报表类型不能为空")
        private String type;             // DASHBOARD / REVENUE / MEMBER / COUPON / ORDER

        @NotBlank(message = "调度方式不能为空")
        private String schedule;          // DAILY / WEEKLY / MONTHLY / ONCE

        /** 接收人邮箱列表(后端 join 为逗号串存储) */
        private List<String> recipients;
    }
}
