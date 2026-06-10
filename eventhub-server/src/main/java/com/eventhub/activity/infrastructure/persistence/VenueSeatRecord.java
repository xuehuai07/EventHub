package com.eventhub.activity.infrastructure.persistence;

public record VenueSeatRecord(
        long venueId,
        String areaName,
        String rowLabel,
        String seatNumber,
        String seatCode,
        String seatGrade,
        int sortOrder) {}
