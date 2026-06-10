package com.eventhub.activity.dto;

import com.eventhub.activity.domain.SeatMode;
import java.util.List;

public record VenueView(
        long id,
        String name,
        String city,
        String address,
        SeatMode seatMode,
        int capacity,
        String status,
        int version,
        List<SeatAreaView> seatAreas) {}
