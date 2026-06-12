package com.eventhub.ticket;

import java.time.LocalDateTime;

public record VerificationLogView(
        long id,
        String result,
        String deviceId,
        String requestIp,
        LocalDateTime verifiedAt,
        String ticketNo,
        String activityTitle,
        String sessionName,
        String venueName,
        String operatorName) {}
