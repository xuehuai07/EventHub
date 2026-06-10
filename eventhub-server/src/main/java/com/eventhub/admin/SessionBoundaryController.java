package com.eventhub.admin;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.security.AuthUserView;
import com.eventhub.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionBoundaryController {

    @Operation(summary = "验证商家后台会话")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    @GetMapping("/api/merchant/session")
    ApiResponse<AuthUserView> merchantSession(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(AuthUserView.from(user));
    }

    @Operation(summary = "验证平台管理会话")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/session")
    ApiResponse<AuthUserView> adminSession(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(AuthUserView.from(user));
    }
}
