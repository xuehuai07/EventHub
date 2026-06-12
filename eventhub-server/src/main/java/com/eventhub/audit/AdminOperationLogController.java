package com.eventhub.audit;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationLogController {

    private final OperationLogService service;

    public AdminOperationLogController(OperationLogService service) {
        this.service = service;
    }

    @Operation(operationId = "listAdminOperationLogs", summary = "查询管理操作审计")
    @GetMapping
    ApiResponse<PageResponse<OperationLogView>> list(
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                service.list(operatorUserId, action, resourceType, startDate, endDate, page, pageSize));
    }
}
