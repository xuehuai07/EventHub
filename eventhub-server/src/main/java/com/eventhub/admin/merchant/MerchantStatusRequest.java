package com.eventhub.admin.merchant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MerchantStatusRequest(
        @NotNull @Pattern(regexp = "ACTIVE|DISABLED", message = "商家状态只能为 ACTIVE 或 DISABLED") String status) {}
