package com.eventhub.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败", HttpStatus.BAD_REQUEST),
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
