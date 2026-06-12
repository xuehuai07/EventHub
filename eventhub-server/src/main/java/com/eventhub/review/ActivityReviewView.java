package com.eventhub.review;

import java.time.LocalDateTime;

public record ActivityReviewView(
        long id,
        long activityId,
        String activityTitle,
        long userId,
        String userDisplayName,
        int rating,
        String content,
        String status,
        String hiddenReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
