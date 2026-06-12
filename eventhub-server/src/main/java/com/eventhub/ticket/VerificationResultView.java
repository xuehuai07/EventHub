package com.eventhub.ticket;

import java.time.LocalDateTime;

public record VerificationResultView(
        TicketView ticket, boolean success, boolean alreadyUsed, LocalDateTime verifiedAt, String verifierName) {}
