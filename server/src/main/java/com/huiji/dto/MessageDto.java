package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 消息中心 DTO */
public class MessageDto {

    /** 创建消息任务请求 */
    @Data
    public static class CreateRequest {
        @NotBlank(message = "请选择渠道")
        private String channel;             // SMS / WECHAT / IN_APP

        @NotBlank(message = "请选择模板类型")
        private String templateType;        // BIRTHDAY / COUPON_EXPIRE / CAMPAIGN / MANUAL

        private String subject;

        @NotBlank(message = "请输入消息内容")
        private String content;

        @NotNull(message = "请选择目标会员")
        private List<Long> memberIds;

        /** 定时发送(可选, 早于当前时间则立即发送) */
        private LocalDateTime scheduledAt;
    }
}
