package com.eventhub.activity.api.merchant;

import com.eventhub.activity.application.activity.MerchantActivityService;
import com.eventhub.activity.domain.ActivityStatus;
import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivityRequest;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.dto.SessionRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/activities")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantActivityController {

    private final MerchantActivityService service;

    public MerchantActivityController(MerchantActivityService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResponse<ActivitySummaryView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.list(user, status, keyword, page, pageSize));
    }

    @GetMapping("/{activityId}")
    ApiResponse<ActivityDetailView> detail(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.detail(user, activityId));
    }

    @Operation(summary = "创建活动草稿")
    @PostMapping
    ApiResponse<ActivityDetailView> create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ActivityRequest request) {
        return ApiResponse.success(service.create(user, request));
    }

    @Operation(summary = "修改活动信息")
    @PutMapping("/{activityId}")
    ApiResponse<ActivityDetailView> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long activityId,
            @Valid @RequestBody ActivityRequest request) {
        return ApiResponse.success(service.update(user, activityId, request));
    }

    @Operation(summary = "新增活动场次和票档")
    @PostMapping("/{activityId}/sessions")
    ApiResponse<ActivityDetailView> createSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long activityId,
            @Valid @RequestBody SessionRequest request) {
        return ApiResponse.success(service.createSession(user, activityId, request));
    }

    @Operation(summary = "修改活动场次和票档")
    @PutMapping("/{activityId}/sessions/{sessionId}")
    ApiResponse<ActivityDetailView> updateSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long activityId,
            @PathVariable long sessionId,
            @Valid @RequestBody SessionRequest request) {
        return ApiResponse.success(service.updateSession(user, activityId, sessionId, request));
    }

    @DeleteMapping("/{activityId}/sessions/{sessionId}")
    ApiResponse<ActivityDetailView> deleteSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long activityId,
            @PathVariable long sessionId) {
        return ApiResponse.success(service.deleteSession(user, activityId, sessionId));
    }

    @Operation(summary = "提交活动审核")
    @PostMapping("/{activityId}/submit")
    ApiResponse<ActivityDetailView> submit(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.submit(user, activityId));
    }
}
