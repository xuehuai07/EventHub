package com.eventhub.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationRequest(
        @NotBlank @Size(max = 1000) String code,
        @Size(max = 128) String deviceId) {}
