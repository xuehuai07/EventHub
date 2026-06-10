package com.eventhub.admin.dashboard;

import com.eventhub.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final ActivityDashboardMapper mapper;

    public AdminDashboardController(ActivityDashboardMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/activity-summary")
    ApiResponse<ActivityDashboardSummary> summary() {
        return ApiResponse.success(mapper.summary());
    }
}
