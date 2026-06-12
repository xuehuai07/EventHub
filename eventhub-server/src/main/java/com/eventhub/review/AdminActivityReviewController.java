package com.eventhub.review;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/admin/activity-reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityReviewController {

    private final UserActivityReviewService service;

    public AdminActivityReviewController(UserActivityReviewService service) {
        this.service = service;
    }

    @Operation(operationId = "listAdminActivityReviews", summary = "查询平台活动评价")
    @GetMapping
    ApiResponse<PageResponse<ActivityReviewView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.adminList(status, keyword, page, pageSize));
    }

    @Operation(operationId = "hideActivityReview", summary = "隐藏活动评价")
    @PostMapping("/{reviewId}/hide")
    ApiResponse<ActivityReviewView> hide(
            @AuthenticationPrincipal AuthenticatedUser moderator,
            @PathVariable long reviewId,
            @Valid @RequestBody ReviewModerationRequest request) {
        return ApiResponse.success(service.hide(moderator, reviewId, request.reason()));
    }

    @Operation(operationId = "restoreActivityReview", summary = "恢复活动评价")
    @PostMapping("/{reviewId}/restore")
    ApiResponse<ActivityReviewView> restore(
            @AuthenticationPrincipal AuthenticatedUser moderator, @PathVariable long reviewId) {
        return ApiResponse.success(service.restore(moderator, reviewId));
    }
}
