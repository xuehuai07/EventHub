package com.eventhub.system;

import com.eventhub.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final String applicationName;

    public SystemController(@Value("${spring.application.name:eventhub-server}") String applicationName) {
        this.applicationName = applicationName;
    }

    @Operation(summary = "获取公开系统状态")
    @GetMapping("/status")
    public ApiResponse<SystemStatus> status() {
        return ApiResponse.success(new SystemStatus(applicationName, "UP"));
    }

    public record SystemStatus(String service, String status) {}
}
