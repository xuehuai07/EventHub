package com.eventhub.favorite;

import com.eventhub.activity.domain.ActivityStatus;
import java.time.LocalDateTime;

public record ActivityFavoriteView(
        long activityId,
        String title,
        String summary,
        String coverUrl,
        String city,
        String categoryName,
        ActivityStatus status,
        LocalDateTime nextSessionAt,
        Long minimumPriceCents,
        LocalDateTime favoritedAt) {}
