package com.eventhub.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record SessionRequest(
        @NotNull @Positive Long venueId,
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull LocalDateTime saleStartAt,
        @NotNull LocalDateTime saleEndAt,
        @NotEmpty @Size(max = 20) List<@Valid TicketTypeRequest> ticketTypes,
        int version) {}
