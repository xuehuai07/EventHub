package com.eventhub.admin.dashboard;

public record OperationsDashboardView(
        long paidAmountCents,
        long paidOrderCount,
        long soldTicketCount,
        long usedTicketCount,
        long publishedActivityCount,
        long activeMerchantCount) {}
