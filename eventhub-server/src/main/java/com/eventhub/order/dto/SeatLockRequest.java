package com.eventhub.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatLockRequest(
        @NotNull Long sessionId,
        @NotNull Long ticketTypeId,
        @Size(max = 6) List<Long> sessionSeatIds,
        @Min(1) int quantity) {}
