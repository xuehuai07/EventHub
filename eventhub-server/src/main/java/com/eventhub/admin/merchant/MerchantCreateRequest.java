package com.eventhub.admin.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {}
