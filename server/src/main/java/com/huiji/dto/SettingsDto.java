package com.huiji.dto;

import lombok.Data;

import java.util.List;

/** 租户设置 DTO */
public class SettingsDto {

    @Data
    public static class SettingsRequest {
        private String tenantName;
        private String brandColor;
        private String smsSign;
        private List<LevelRule> levelRules;
        private List<RechargeRule> rechargeRules;
    }

    @Data
    public static class LevelRule {
        private Integer level;
        private String name;
        private Long threshold;
    }

    @Data
    public static class RechargeRule {
        private Long recharge;
        private Long gift;
    }
}
