package com.eventhub.notification;

import java.time.LocalDateTime;

public record NotificationView(
        long id,
        String type,
        String title,
        String content,
        String resourceType,
        Long resourceId,
        LocalDateTime readAt,
        LocalDateTime createdAt) {}
