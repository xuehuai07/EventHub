package com.eventhub.activity.dto;

import com.eventhub.activity.domain.ActivityStatus;
import java.time.LocalDateTime;

public record ActivitySummaryView(
        long id,
        String title,
        String summary,
        String coverUrl,
        String city,
        String categoryName,
        String merchantName,
        ActivityStatus status,
        LocalDateTime nextSessionAt,
        Long minimumPriceCents,
        int version) {}
