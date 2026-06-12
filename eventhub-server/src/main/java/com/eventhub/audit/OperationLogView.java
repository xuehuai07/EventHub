package com.eventhub.audit;

import java.time.LocalDateTime;

public record OperationLogView(
        long id,
        long operatorUserId,
        String operatorName,
        String operatorRole,
        Long merchantId,
        String action,
        String resourceType,
        long resourceId,
        String summary,
        String requestId,
        LocalDateTime createdAt) {}
