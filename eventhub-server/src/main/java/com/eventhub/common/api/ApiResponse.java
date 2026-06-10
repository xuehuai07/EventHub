package com.eventhub.common.api;

import com.eventhub.common.error.ErrorCode;
import com.eventhub.common.request.RequestIdFilter;
import java.time.Instant;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public record ApiResponse<T>(String code, String message, T data, String requestId, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "成功", data, currentRequestId(), Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.message());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return error(errorCode, message, currentRequestId());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message, String requestId) {
        return new ApiResponse<>(errorCode.code(), message, null, requestId, Instant.now());
    }

    private static String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object requestId =
                attributes.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return requestId == null ? null : requestId.toString();
    }
}
