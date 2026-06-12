package com.eventhub.ticket;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ticket-verifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketVerificationController {

    private final TicketVerificationService service;

    public AdminTicketVerificationController(TicketVerificationService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResponse<VerificationLogView>> logs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.logs(user, true, page, pageSize));
    }
}
