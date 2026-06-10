package com.eventhub.activity.application.activity;

import com.eventhub.activity.domain.ActivityStatus;
import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.dto.CategoryView;
import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.activity.infrastructure.persistence.ActivityDetailRow;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicActivityService {

    private final ActivityQueryMapper mapper;
    private final ActivityViewAssembler assembler;
    private final ActivityDetailCache cache;

    public PublicActivityService(
            ActivityQueryMapper mapper, ActivityViewAssembler assembler, ActivityDetailCache cache) {
        this.mapper = mapper;
        this.assembler = assembler;
        this.cache = cache;
    }

    public List<CategoryView> categories() {
        return mapper.findCategories();
    }

    public PageResponse<ActivitySummaryView> list(
            Long categoryId, String city, String keyword, LocalDate date, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        String normalizedCity = normalize(city);
        String normalizedKeyword = normalize(keyword);
        List<ActivitySummaryView> items = mapper.findPublishedActivities(
                categoryId, normalizedCity, normalizedKeyword, date, (safePage - 1) * safeSize, safeSize);
        long total = mapper.countPublishedActivities(categoryId, normalizedCity, normalizedKeyword, date);
        return PageResponse.of(items, safePage, safeSize, total);
    }

    public ActivityDetailView detail(long activityId) {
        ActivityDetailView cached = cache.get(activityId);
        if (cached != null) {
            return cached;
        }
        ActivityDetailRow row = mapper.findDetail(activityId);
        if (row == null || row.status() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_AVAILABLE);
        }
        ActivityDetailView detail = assembler.detail(row);
        cache.put(activityId, detail);
        return detail;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
