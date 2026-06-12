package com.eventhub.admin.dashboard;

import com.eventhub.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final ActivityDashboardMapper mapper;
    private final OperationsDashboardService operations;

    public AdminDashboardController(ActivityDashboardMapper mapper, OperationsDashboardService operations) {
        this.mapper = mapper;
        this.operations = operations;
    }

    @GetMapping("/activity-summary")
    ApiResponse<ActivityDashboardSummary> summary() {
        return ApiResponse.success(mapper.summary());
    }

    @Operation(operationId = "getAdminOperationsDashboard", summary = "查询平台运营概览")
    @GetMapping("/operations")
    ApiResponse<OperationsDashboardView> operations() {
        return ApiResponse.success(operations.operations(null));
    }

    @Operation(operationId = "getAdminSalesTrend", summary = "查询平台销售趋势")
    @GetMapping("/sales-trend")
    ApiResponse<List<SalesTrendView>> salesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(operations.salesTrend(null, startDate, endDate));
    }

    @Operation(operationId = "getAdminTopActivities", summary = "查询平台热门活动")
    @GetMapping("/top-activities")
    ApiResponse<List<TopActivityView>> topActivities(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(operations.topActivities(null, limit));
    }
}
