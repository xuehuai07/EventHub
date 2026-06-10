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
    MERCHANT_NOT_BOUND("MERCHANT_NOT_BOUND", "当前账号未绑定商家", HttpStatus.FORBIDDEN),
    MERCHANT_INACTIVE("MERCHANT_INACTIVE", "商家或员工账号不可用", HttpStatus.FORBIDDEN),
    VENUE_NOT_FOUND("VENUE_NOT_FOUND", "场馆不存在", HttpStatus.NOT_FOUND),
    VENUE_ACCESS_DENIED("VENUE_ACCESS_DENIED", "无权操作该场馆", HttpStatus.FORBIDDEN),
    ACTIVITY_NOT_FOUND("ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND),
    ACTIVITY_ACCESS_DENIED("ACTIVITY_ACCESS_DENIED", "无权操作该活动", HttpStatus.FORBIDDEN),
    ACTIVITY_STATUS_INVALID("ACTIVITY_STATUS_INVALID", "当前活动状态不允许该操作", HttpStatus.CONFLICT),
    ACTIVITY_INCOMPLETE("ACTIVITY_INCOMPLETE", "活动信息不完整，暂不能提交审核", HttpStatus.BAD_REQUEST),
    ACTIVITY_VERSION_CONFLICT("ACTIVITY_VERSION_CONFLICT", "活动已被其他操作更新，请刷新后重试", HttpStatus.CONFLICT),
    ACTIVITY_NOT_AVAILABLE("ACTIVITY_NOT_AVAILABLE", "活动暂不可查看", HttpStatus.NOT_FOUND),
    SESSION_TIME_INVALID("SESSION_TIME_INVALID", "场次或售票时间设置不正确", HttpStatus.BAD_REQUEST),
    TICKET_TYPE_INVALID("TICKET_TYPE_INVALID", "票档设置不正确", HttpStatus.BAD_REQUEST),
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
