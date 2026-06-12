package com.eventhub.review;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities/{activityId}")
public class ActivityReviewController {

    private final UserActivityReviewService service;

    public ActivityReviewController(UserActivityReviewService service) {
        this.service = service;
    }

    @Operation(operationId = "listPublicActivityReviews", summary = "查询活动公开评价")
    @GetMapping("/reviews")
    ApiResponse<PageResponse<ActivityReviewView>> reviews(
            @PathVariable long activityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(service.publicReviews(activityId, page, pageSize));
    }

    @Operation(operationId = "getActivityReviewSummary", summary = "查询活动评价摘要")
    @GetMapping("/review-summary")
    ApiResponse<ActivityReviewSummaryView> summary(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.summary(user, activityId));
    }

    @Operation(operationId = "getMyActivityReview", summary = "查询本人活动评价")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-review")
    ApiResponse<ActivityReviewView> mine(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.mine(user, activityId));
    }

    @Operation(operationId = "saveMyActivityReview", summary = "创建或更新本人活动评价")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/my-review")
    ApiResponse<ActivityReviewView> save(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long activityId,
            @Valid @RequestBody ActivityReviewRequest request) {
        return ApiResponse.success(service.save(user, activityId, request));
    }

    @Operation(operationId = "deleteMyActivityReview", summary = "删除本人活动评价")
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/my-review")
    ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        service.deleteMine(user, activityId);
        return ApiResponse.success(null);
    }
}
