package com.eventhub.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewModerationRequest(
        @NotBlank @Size(max = 500) String reason) {}
