package com.eventhub.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatGenerationRequest(
        @NotEmpty @Size(max = 20) List<@Valid SeatBlockRequest> blocks) {}
