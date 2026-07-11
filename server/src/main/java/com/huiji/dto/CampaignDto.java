package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 营销活动相关 DTO */
public class CampaignDto {

    @Data
    public static class CampaignRequest {
        @NotBlank(message = "活动名称不能为空")
        private String name;
        @NotBlank(message = "活动类型不能为空")
        private String type;
        private String trigger;
        private String audience;
        @NotBlank(message = "触达渠道不能为空")
        private String channel;
        private String content;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private Boolean enabled;
        private List<SopStep> sopSteps;
    }

    /** SOP 步骤 */
    @Data
    public static class SopStep {
        private String type;
        private String text;
        private Integer delayMinutes;
    }

    @Data
    public static class ToggleRequest {
        private Boolean enabled;
    }
}
