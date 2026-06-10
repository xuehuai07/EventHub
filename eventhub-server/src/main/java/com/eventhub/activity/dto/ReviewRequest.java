package com.eventhub.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotBlank @Size(max = 500) String reason) {}
