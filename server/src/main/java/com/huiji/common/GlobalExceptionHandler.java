package com.huiji.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理, 统一输出 { ok:false, message, code } 并设置正确的 HTTP 状态码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e, HttpServletResponse resp) {
        resp.setStatus(e.getErrorCode().getHttpStatus());
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    /** 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + defaultMsg(fe))
                .collect(Collectors.joining("; "));
        return Result.fail(ErrorCode.VALIDATION, msg);
    }

    private String defaultMsg(FieldError fe) {
        return fe.getDefaultMessage() == null ? "无效" : fe.getDefaultMessage();
    }

    /** 鉴权失败 401 */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuth(AuthenticationException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        return Result.fail(ErrorCode.SESSION_EXPIRED);
    }

    /** 无权限 403 */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleForbidden(AccessDeniedException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.FORBIDDEN.value());
        return Result.fail(ErrorCode.FORBIDDEN, "无权限访问");
    }

    /** 404: 无对应处理器 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.NOT_FOUND.value());
        return Result.fail(ErrorCode.NOT_FOUND, "接口不存在");
    }

    /** 404: 静态资源/路由未找到(Spring 6), 不打印 ERROR 避免日志噪音 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.NOT_FOUND.value());
        log.debug("资源未找到: {}", e.getMessage());
        return Result.fail(ErrorCode.NOT_FOUND, "接口不存在");
    }

    /** 400: 路径变量类型不匹配(如 /{id} 收到非数字) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.BAD_REQUEST.value());
        log.warn("参数类型不匹配: {}", e.getMessage());
        return Result.fail(ErrorCode.VALIDATION, "参数格式错误: " + e.getName());
    }

    /** 400: 缺少必需的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.BAD_REQUEST.value());
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(ErrorCode.VALIDATION, "缺少必需参数: " + e.getParameterName());
    }

    /** 兜底 500 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        log.error("服务异常", e);
        return Result.fail(ErrorCode.SERVER_ERROR, "服务异常, 请稍后重试");
    }
}
