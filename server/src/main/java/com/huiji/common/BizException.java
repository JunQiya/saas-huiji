package com.huiji.common;

import lombok.Getter;

/**
 * 业务异常, 携带错误码与提示信息。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BizException(String message) {
        super(message);
        this.errorCode = ErrorCode.BIZ_ERROR;
    }
}
