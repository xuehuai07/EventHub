package com.eventhub.activity.infrastructure.persistence;

import com.eventhub.activity.domain.ActivityStatus;

public record ActivityDetailRow(
        long id,
        long merchantId,
        long categoryId,
        String categoryName,
        String merchantName,
        String title,
        String summary,
        String description,
        String coverUrl,
        String city,
        ActivityStatus status,
        String reviewReason,
        int version,
        long favoriteCount,
        long reviewCount,
        Double averageRating) {}
