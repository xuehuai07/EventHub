package com.eventhub.activity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketTypeRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 32) String seatGrade,
        @Min(0) long priceCents,
        @Min(1) int totalStock,
        @Min(1) @Max(20) int saleLimitPerUser) {}
