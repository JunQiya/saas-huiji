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

    @Data
    public static class FeatureFlags {
        /** 积分系统 */
        private Boolean pointsEnabled = true;
        /** 储值功能 */
        private Boolean rechargeEnabled = true;
        /** 优惠券 */
        private Boolean couponsEnabled = true;
        /** 营销活动 */
        private Boolean campaignsEnabled = true;
        /** 推荐裂变 */
        private Boolean referralEnabled = true;
        /** 生日营销 */
        private Boolean birthdayMarketingEnabled = true;
        /** 短信通知 */
        private Boolean smsEnabled = false;
        /** 会员自助注册 */
        private Boolean selfRegisterEnabled = true;
        /** 消费自动升级等级 */
        private Boolean autoUpgradeEnabled = true;
        /** 小票打印 */
        private Boolean receiptPrintEnabled = true;
    }
}
