package com.eventhub.admin.merchant;

import java.time.LocalDateTime;

public record MerchantView(
        long id, String name, String description, String status, int staffCount, LocalDateTime createdAt) {}
