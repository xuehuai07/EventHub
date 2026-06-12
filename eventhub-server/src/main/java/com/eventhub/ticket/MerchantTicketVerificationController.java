package com.eventhub.ticket;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ticket-verifications")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantTicketVerificationController {

    private final TicketVerificationService service;

    public MerchantTicketVerificationController(TicketVerificationService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    ApiResponse<VerificationResultView> preview(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody VerificationRequest request) {
        return ApiResponse.success(service.preview(user, request));
    }

    @PostMapping
    ApiResponse<VerificationResultView> verify(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody VerificationRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(service.verify(user, request, clientIp(servletRequest)));
    }

    @GetMapping
    ApiResponse<PageResponse<VerificationLogView>> logs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.logs(user, false, page, pageSize));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
    }
}
