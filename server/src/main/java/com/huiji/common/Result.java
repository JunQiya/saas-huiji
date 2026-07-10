package com.huiji.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应包装。
 * 成功: { "ok": true, "data": ... }
 * 失败: { "ok": false, "message": "...", "code": "..." }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private boolean ok;
    private T data;
    private String message;
    private String code;

    private Result(boolean ok, T data, String message, String code) {
        this.ok = ok;
        this.data = data;
        this.message = message;
        this.code = code;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null, null);
    }

    public static <T> Result<T> success() {
        return new Result<>(true, null, null, null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(false, null, message, code);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(false, null, errorCode.getMessage(), errorCode.getCode());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(false, null, message, errorCode.getCode());
    }
}
