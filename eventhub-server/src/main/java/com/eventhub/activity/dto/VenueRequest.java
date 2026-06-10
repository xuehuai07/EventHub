package com.eventhub.activity.dto;

import com.eventhub.activity.domain.SeatMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VenueRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 64) String city,
        @NotBlank @Size(max = 255) String address,
        @NotNull SeatMode seatMode,
        @Min(0) int capacity,
        int version) {}
