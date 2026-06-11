package com.eventhub.order.api.user;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.order.application.lock.SeatLockService;
import com.eventhub.order.dto.SeatLockRequest;
import com.eventhub.order.dto.SeatLockView;
import com.eventhub.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seat-locks")
@PreAuthorize("hasRole('USER')")
public class SeatLockController {

    private final SeatLockService service;

    public SeatLockController(SeatLockService service) {
        this.service = service;
    }

    @PostMapping
    ApiResponse<SeatLockView> create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SeatLockRequest request) {
        return ApiResponse.success(service.create(user, request));
    }

    @GetMapping("/{lockNo}")
    ApiResponse<SeatLockView> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String lockNo) {
        return ApiResponse.success(service.get(user, lockNo));
    }

    @DeleteMapping("/{lockNo}")
    ApiResponse<SeatLockView> release(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String lockNo) {
        return ApiResponse.success(service.release(user, lockNo));
    }
}
