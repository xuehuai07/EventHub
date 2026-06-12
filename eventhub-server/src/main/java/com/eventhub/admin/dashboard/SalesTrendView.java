package com.eventhub.admin.dashboard;

import java.time.LocalDate;

public record SalesTrendView(LocalDate date, long paidAmountCents, long paidOrderCount, long soldTicketCount) {}
