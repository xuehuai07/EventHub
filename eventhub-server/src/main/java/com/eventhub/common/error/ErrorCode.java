package com.eventhub.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败", HttpStatus.BAD_REQUEST),
    CONFLICT("CONFLICT", "数据已存在", HttpStatus.CONFLICT),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "账号或密码错误", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_DISABLED("AUTH_ACCOUNT_DISABLED", "账号不可用", HttpStatus.FORBIDDEN),
    AUTH_CLIENT_NOT_ALLOWED("AUTH_CLIENT_NOT_ALLOWED", "当前账号不能登录该客户端", HttpStatus.FORBIDDEN),
    AUTH_REFRESH_INVALID("AUTH_REFRESH_INVALID", "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    AUTH_CSRF_INVALID("AUTH_CSRF_INVALID", "安全校验失败，请重新登录", HttpStatus.FORBIDDEN),
    AUTH_LOGIN_LOCKED("AUTH_LOGIN_LOCKED", "登录失败次数过多，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", "无权访问该资源", HttpStatus.FORBIDDEN),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "不支持当前请求方法", HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_ERROR("INTERNAL_ERROR", "服务器发生异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
