package com.eventhub.admin.activity;

import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.dto.ReviewRequest;
import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/activities")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final ActivityReviewService service;

    public AdminActivityController(ActivityReviewService service) {
        this.service = service;
    }

    @GetMapping("/reviews")
    ApiResponse<PageResponse<ActivitySummaryView>> pending(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.pending(keyword, page, pageSize));
    }

    @GetMapping("/{activityId}")
    ApiResponse<ActivityDetailView> detail(@PathVariable long activityId) {
        return ApiResponse.success(service.detail(activityId));
    }

    @PostMapping("/{activityId}/approve")
    ApiResponse<ActivityDetailView> approve(
            @AuthenticationPrincipal AuthenticatedUser reviewer, @PathVariable long activityId) {
        return ApiResponse.success(service.approve(reviewer, activityId));
    }

    @PostMapping("/{activityId}/reject")
    ApiResponse<ActivityDetailView> reject(
            @AuthenticationPrincipal AuthenticatedUser reviewer,
            @PathVariable long activityId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(service.reject(reviewer, activityId, request.reason()));
    }

    @PostMapping("/{activityId}/off-shelf")
    ApiResponse<ActivityDetailView> offShelf(
            @AuthenticationPrincipal AuthenticatedUser reviewer,
            @PathVariable long activityId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(service.offShelf(reviewer, activityId, request.reason()));
    }
}
