package com.eventhub.admin.merchant;

import jakarta.validation.constraints.NotBlank;

public record MerchantStaffRequest(@NotBlank String identifier) {}
