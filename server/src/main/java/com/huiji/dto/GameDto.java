package com.huiji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 赢奖小游戏相关 DTO */
public class GameDto {

    @Data
    public static class GameRequest {
        private Long id;
        @NotBlank(message = "游戏名称不能为空")
        private String name;
        @NotBlank(message = "游戏类型不能为空")
        private String type;
        private String subtitle;
        private String coverImage;
        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;
        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;
        private Integer dailyLimit;
        private Integer totalLimit;
        private Integer pointsCost;
        private String status;
        private String rules;
        private String bgImage;
        private Long storeId;
    }

    @Data
    public static class PrizeRequest {
        private Long id;
        @NotBlank(message = "奖品名称不能为空")
        private String name;
        @NotBlank(message = "奖品类型不能为空")
        private String type;
        private Long refId;
        private String refName;
        private Integer amount;
        private Integer probability;
        private String imageUrl;
        private Integer sortOrder;
    }

    @Data
    public static class PlayRequest {
        @NotNull(message = "gameId 不能为空")
        private Long gameId;
    }

    @Data
    public static class PlayResult {
        private Boolean isWin;
        private String prizeName;
        private String prizeType;
        private String prizeImage;
    }
}
