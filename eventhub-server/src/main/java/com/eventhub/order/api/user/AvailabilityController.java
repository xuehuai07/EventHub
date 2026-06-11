package com.eventhub.order.api.user;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.order.application.lock.AvailabilityService;
import com.eventhub.order.dto.AvailabilityView;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvailabilityController {

    private final AvailabilityService service;

    public AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    @Operation(summary = "查询场次实时座位与库存")
    @GetMapping("/api/sessions/{sessionId}/availability")
    ApiResponse<AvailabilityView> get(@PathVariable long sessionId) {
        return ApiResponse.success(service.get(sessionId));
    }
}
