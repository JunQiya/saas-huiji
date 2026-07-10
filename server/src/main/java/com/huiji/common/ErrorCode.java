package com.huiji.common;

import lombok.Getter;

/**
 * 错误码枚举, 对应 API.md 第 12 节。
 */
@Getter
public enum ErrorCode {

    SESSION_EXPIRED(401, "SESSION_EXPIRED", "登录已过期"),
    FORBIDDEN(403, "FORBIDDEN", "无权限"),
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    VALIDATION(422, "VALIDATION", "参数校验失败"),
    CONFLICT(409, "CONFLICT", "冲突"),
    BIZ_ERROR(400, "BIZ_ERROR", "业务异常"),
    SERVER_ERROR(500, "SERVER_ERROR", "服务异常"),
    PLAN_LIMIT(403, "PLAN_LIMIT", "已达套餐上限"),
    MESSAGE_QUOTA_EXCEEDED(400, "MESSAGE_QUOTA", "短信余额不足");

    private final int httpStatus;
    private final String code;
    private final String message;

    ErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
