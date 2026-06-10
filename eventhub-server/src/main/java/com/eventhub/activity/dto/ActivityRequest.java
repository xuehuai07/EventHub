package com.eventhub.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActivityRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 300) String summary,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 500) String coverUrl,
        @NotBlank @Size(max = 64) String city,
        int version) {}
