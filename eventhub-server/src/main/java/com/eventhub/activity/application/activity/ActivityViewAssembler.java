package com.eventhub.activity.application.activity;

import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.SessionView;
import com.eventhub.activity.dto.TicketTypeView;
import com.eventhub.activity.infrastructure.persistence.ActivityDetailRow;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.SessionRecord;
import org.springframework.stereotype.Component;

@Component
public class ActivityViewAssembler {

    private final ActivityQueryMapper mapper;

    public ActivityViewAssembler(ActivityQueryMapper mapper) {
        this.mapper = mapper;
    }

    public ActivityDetailView detail(ActivityDetailRow row) {
        return new ActivityDetailView(
                row.id(),
                row.categoryId(),
                row.categoryName(),
                row.merchantName(),
                row.title(),
                row.summary(),
                row.description(),
                row.coverUrl(),
                row.city(),
                row.status(),
                row.reviewReason(),
                row.version(),
                row.favoriteCount(),
                row.reviewCount(),
                row.averageRating(),
                mapper.findSessions(row.id()).stream().map(this::session).toList());
    }

    private SessionView session(SessionRecord session) {
        return new SessionView(
                session.getId(),
                session.getVenueId(),
                session.getVenueName(),
                session.getVenueAddress(),
                session.getSeatMode(),
                session.getName(),
                session.getStartAt(),
                session.getEndAt(),
                session.getSaleStartAt(),
                session.getSaleEndAt(),
                session.getStatus(),
                session.getVersion(),
                mapper.findTicketTypes(session.getId()).stream()
                        .map(ticket -> new TicketTypeView(
                                ticket.id(),
                                ticket.name(),
                                ticket.seatGrade(),
                                ticket.priceCents(),
                                ticket.totalStock(),
                                ticket.availableStock(),
                                ticket.saleLimitPerUser()))
                        .toList(),
                mapper.findSeatAreas(session.getVenueId()));
    }
}
