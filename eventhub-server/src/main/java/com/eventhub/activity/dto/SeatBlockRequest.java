package com.eventhub.activity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeatBlockRequest(
        @NotBlank @Size(max = 64) String areaName,
        @NotBlank @Size(max = 32) String seatGrade,
        @NotBlank @Size(max = 10) String rowPrefix,
        @Min(1) @Max(100) int rowCount,
        @Min(1) @Max(100) int seatsPerRow) {}
