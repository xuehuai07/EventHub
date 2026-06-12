package com.eventhub.favorite;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-favorites")
@PreAuthorize("hasRole('USER')")
public class ActivityFavoriteController {

    private final ActivityFavoriteService service;

    public ActivityFavoriteController(ActivityFavoriteService service) {
        this.service = service;
    }

    @Operation(operationId = "listMyActivityFavorites", summary = "查询本人收藏活动")
    @GetMapping
    ApiResponse<PageResponse<ActivityFavoriteView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return ApiResponse.success(service.list(user, page, pageSize));
    }

    @Operation(operationId = "getMyActivityFavoriteStatus", summary = "查询本人活动收藏状态")
    @GetMapping("/{activityId}/status")
    ApiResponse<ActivityFavoriteStatusView> status(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.status(user, activityId));
    }

    @Operation(operationId = "favoriteActivity", summary = "收藏活动")
    @PutMapping("/{activityId}")
    ApiResponse<ActivityFavoriteStatusView> favorite(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.favorite(user, activityId));
    }

    @Operation(operationId = "unfavoriteActivity", summary = "取消收藏活动")
    @DeleteMapping("/{activityId}")
    ApiResponse<ActivityFavoriteStatusView> unfavorite(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long activityId) {
        return ApiResponse.success(service.unfavorite(user, activityId));
    }
}
