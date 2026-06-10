package com.eventhub.activity.api.merchant;

import com.eventhub.activity.application.venue.VenueService;
import com.eventhub.activity.dto.SeatGenerationRequest;
import com.eventhub.activity.dto.VenueRequest;
import com.eventhub.activity.dto.VenueView;
import com.eventhub.common.api.ApiResponse;
import com.eventhub.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/merchant/venues")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantVenueController {

    private final VenueService service;

    public MerchantVenueController(VenueService service) {
        this.service = service;
    }

    @Operation(summary = "查询当前商家的场馆")
    @GetMapping
    ApiResponse<List<VenueView>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.list(user));
    }

    @Operation(summary = "创建场馆")
    @PostMapping
    ApiResponse<VenueView> create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody VenueRequest request) {
        return ApiResponse.success(service.create(user, request));
    }

    @Operation(summary = "修改场馆")
    @PutMapping("/{venueId}")
    ApiResponse<VenueView> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long venueId,
            @Valid @RequestBody VenueRequest request) {
        return ApiResponse.success(service.update(user, venueId, request));
    }

    @Operation(summary = "批量生成固定座位")
    @PutMapping("/{venueId}/seats")
    ApiResponse<VenueView> generateSeats(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long venueId,
            @Valid @RequestBody SeatGenerationRequest request) {
        return ApiResponse.success(service.generateSeats(user, venueId, request));
    }
}
