package com.eventhub.admin.dashboard;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.user.MerchantContextService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/dashboard")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantDashboardController {

    private final OperationsDashboardService service;
    private final MerchantContextService merchantContext;

    public MerchantDashboardController(OperationsDashboardService service, MerchantContextService merchantContext) {
        this.service = service;
        this.merchantContext = merchantContext;
    }

    @Operation(operationId = "getMerchantOperationsDashboard", summary = "查询本商家运营概览")
    @GetMapping("/operations")
    ApiResponse<OperationsDashboardView> operations(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.operations(merchantId(user)));
    }

    @Operation(operationId = "getMerchantSalesTrend", summary = "查询本商家销售趋势")
    @GetMapping("/sales-trend")
    ApiResponse<List<SalesTrendView>> salesTrend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(service.salesTrend(merchantId(user), startDate, endDate));
    }

    @Operation(operationId = "getMerchantTopActivities", summary = "查询本商家热门活动")
    @GetMapping("/top-activities")
    ApiResponse<List<TopActivityView>> topActivities(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(service.topActivities(merchantId(user), limit));
    }

    private long merchantId(AuthenticatedUser user) {
        return merchantContext.requireActiveMerchant(user).merchantId();
    }
}
