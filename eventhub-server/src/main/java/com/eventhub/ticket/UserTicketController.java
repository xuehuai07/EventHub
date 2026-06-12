package com.eventhub.ticket;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('USER')")
public class UserTicketController {

    private final UserTicketService service;

    public UserTicketController(UserTicketService service) {
        this.service = service;
    }

    @GetMapping("/api/tickets")
    ApiResponse<PageResponse<TicketView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.list(user, status, page, pageSize));
    }

    @GetMapping("/api/tickets/{ticketId}")
    ApiResponse<TicketView> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long ticketId) {
        return ApiResponse.success(service.detail(user, ticketId));
    }

    @PostMapping("/api/tickets/{ticketId}/credential")
    ApiResponse<TicketCredentialView> credential(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long ticketId) {
        return ApiResponse.success(service.credential(user, ticketId));
    }

    @GetMapping("/api/orders/{orderId}/tickets")
    ApiResponse<List<TicketView>> orderTickets(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long orderId) {
        return ApiResponse.success(service.orderTickets(user, orderId));
    }
}
