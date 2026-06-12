package com.eventhub.admin.merchant;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/merchants")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMerchantController {

    private final MerchantAdminService service;

    public AdminMerchantController(MerchantAdminService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<MerchantView>> list() {
        return ApiResponse.success(service.list());
    }

    @PostMapping
    ApiResponse<MerchantView> create(
            @AuthenticationPrincipal AuthenticatedUser operator, @Valid @RequestBody MerchantCreateRequest request) {
        return ApiResponse.success(service.create(operator, request));
    }

    @PutMapping("/{merchantId}/status")
    ApiResponse<Void> updateStatus(
            @AuthenticationPrincipal AuthenticatedUser operator,
            @PathVariable long merchantId,
            @Valid @RequestBody MerchantStatusRequest request) {
        service.updateStatus(operator, merchantId, request.status());
        return ApiResponse.success(null);
    }

    @PostMapping("/{merchantId}/staff")
    ApiResponse<Void> bindStaff(
            @AuthenticationPrincipal AuthenticatedUser operator,
            @PathVariable long merchantId,
            @Valid @RequestBody MerchantStaffRequest request) {
        service.bindStaff(operator, merchantId, request.identifier());
        return ApiResponse.success(null);
    }
}
