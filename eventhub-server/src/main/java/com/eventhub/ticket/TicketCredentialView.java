package com.eventhub.ticket;

import java.time.Instant;

public record TicketCredentialView(String credential, Instant expiresAt) {}
