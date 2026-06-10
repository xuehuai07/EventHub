package com.eventhub.activity.dto;

import com.eventhub.activity.domain.ActivityStatus;
import java.util.List;

public record ActivityDetailView(
        long id,
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
        List<SessionView> sessions) {}
