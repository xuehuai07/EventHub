package com.eventhub.activity.api.publicview;

import com.eventhub.activity.application.activity.PublicActivityService;
import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.dto.CategoryView;
import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicActivityController {

    private final PublicActivityService service;

    public PublicActivityController(PublicActivityService service) {
        this.service = service;
    }

    @Operation(operationId = "listPublicActivityCategories", summary = "查询活动分类")
    @GetMapping("/api/activity-categories")
    ApiResponse<List<CategoryView>> categories() {
        return ApiResponse.success(service.categories());
    }

    @Operation(operationId = "listPublicActivities", summary = "查询已发布活动")
    @GetMapping("/api/activities")
    ApiResponse<PageResponse<ActivitySummaryView>> activities(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return ApiResponse.success(service.list(categoryId, city, keyword, date, page, pageSize));
    }

    @Operation(operationId = "getPublicActivityDetail", summary = "查询已发布活动详情")
    @GetMapping("/api/activities/{activityId}")
    ApiResponse<ActivityDetailView> detail(@PathVariable long activityId) {
        return ApiResponse.success(service.detail(activityId));
    }
}
