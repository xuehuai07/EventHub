package com.eventhub.notification;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('USER')")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResponse<NotificationView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.list(user, page, pageSize));
    }

    @GetMapping("/unread-count")
    ApiResponse<UnreadCountView> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.unreadCount(user));
    }

    @PostMapping("/{notificationId}/read")
    ApiResponse<Void> markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long notificationId) {
        service.markRead(user, notificationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/read-all")
    ApiResponse<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        service.markAllRead(user);
        return ApiResponse.success(null);
    }
}
