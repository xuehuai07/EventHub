package com.eventhub.activity.application.activity;

import com.eventhub.activity.domain.ActivityStateMachine;
import com.eventhub.activity.domain.ActivityStatus;
import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivityRequest;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.dto.SessionRequest;
import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityDetailRow;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityRecord;
import com.eventhub.activity.infrastructure.persistence.SessionRecord;
import com.eventhub.activity.infrastructure.persistence.TicketTypeRecord;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.activity.infrastructure.persistence.VenueRecord;
import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.user.MerchantContextService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantActivityService {

    private final ActivityCommandMapper commands;
    private final ActivityQueryMapper queries;
    private final VenueMapper venueMapper;
    private final MerchantContextService merchantContext;
    private final ActivityStateMachine stateMachine;
    private final ActivityViewAssembler assembler;
    private final ActivityDetailCache cache;

    public MerchantActivityService(
            ActivityCommandMapper commands,
            ActivityQueryMapper queries,
            VenueMapper venueMapper,
            MerchantContextService merchantContext,
            ActivityStateMachine stateMachine,
            ActivityViewAssembler assembler,
            ActivityDetailCache cache) {
        this.commands = commands;
        this.queries = queries;
        this.venueMapper = venueMapper;
        this.merchantContext = merchantContext;
        this.stateMachine = stateMachine;
        this.assembler = assembler;
        this.cache = cache;
    }

    public PageResponse<ActivitySummaryView> list(
            AuthenticatedUser user, ActivityStatus status, String keyword, int page, int pageSize) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        List<ActivitySummaryView> items = queries.findMerchantActivities(
                merchantId, status, normalize(keyword), (safePage - 1) * safeSize, safeSize);
        long total = queries.countMerchantActivities(merchantId, status, normalize(keyword));
        return PageResponse.of(items, safePage, safeSize, total);
    }

    public ActivityDetailView detail(AuthenticatedUser user, long activityId) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityDetailRow row = requireOwnedDetail(activityId, merchantId);
        return assembler.detail(row);
    }

    @Transactional
    public ActivityDetailView create(AuthenticatedUser user, ActivityRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityRecord activity = record(merchantId, request);
        commands.insertActivity(activity);
        return assembler.detail(queries.findDetail(activity.getId()));
    }

    @Transactional
    public ActivityDetailView update(AuthenticatedUser user, long activityId, ActivityRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityRecord current = requireOwned(activityId, merchantId);
        stateMachine.requireEditable(current.getStatus());
        ActivityRecord updated = record(merchantId, request);
        updated.setId(activityId);
        if (commands.updateActivity(updated) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_VERSION_CONFLICT);
        }
        cache.evict(activityId);
        return assembler.detail(queries.findDetail(activityId));
    }

    @Transactional
    public ActivityDetailView createSession(AuthenticatedUser user, long activityId, SessionRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityRecord activity = requireOwned(activityId, merchantId);
        stateMachine.requireEditable(activity.getStatus());
        requireOwnedVenue(request.venueId(), merchantId);
        validateSession(request);
        SessionRecord session = session(activityId, request);
        commands.insertSession(session);
        replaceTickets(session.getId(), request);
        cache.evict(activityId);
        return assembler.detail(queries.findDetail(activityId));
    }

    @Transactional
    public ActivityDetailView updateSession(
            AuthenticatedUser user, long activityId, long sessionId, SessionRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityRecord activity = requireOwned(activityId, merchantId);
        stateMachine.requireEditable(activity.getStatus());
        requireOwnedVenue(request.venueId(), merchantId);
        validateSession(request);
        SessionRecord session = session(activityId, request);
        session.setId(sessionId);
        if (commands.updateSession(session) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_VERSION_CONFLICT);
        }
        replaceTickets(sessionId, request);
        cache.evict(activityId);
        return assembler.detail(queries.findDetail(activityId));
    }

    @Transactional
    public ActivityDetailView deleteSession(AuthenticatedUser user, long activityId, long sessionId) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityRecord activity = requireOwned(activityId, merchantId);
        stateMachine.requireEditable(activity.getStatus());
        SessionRecord session = commands.findSession(activityId, sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND, "场次不存在");
        }
        commands.deleteTicketTypes(sessionId);
        commands.deleteSession(activityId, sessionId);
        cache.evict(activityId);
        return assembler.detail(queries.findDetail(activityId));
    }

    @Transactional
    public ActivityDetailView submit(AuthenticatedUser user, long activityId) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        ActivityRecord activity = requireOwned(activityId, merchantId);
        stateMachine.requireSubmittable(activity.getStatus());
        if (commands.countCompleteSessions(activityId) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_INCOMPLETE);
        }
        if (commands.submit(activityId, merchantId) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
        cache.evict(activityId);
        return assembler.detail(queries.findDetail(activityId));
    }

    private void replaceTickets(long sessionId, SessionRequest request) {
        commands.deleteTicketTypes(sessionId);
        List<TicketTypeRecord> tickets = request.ticketTypes().stream()
                .map(ticket -> new TicketTypeRecord(
                        null,
                        sessionId,
                        ticket.name().trim(),
                        normalize(ticket.seatGrade()),
                        ticket.priceCents(),
                        ticket.totalStock(),
                        ticket.totalStock(),
                        ticket.saleLimitPerUser()))
                .toList();
        commands.insertTicketTypes(tickets);
    }

    private void validateSession(SessionRequest request) {
        if (!request.startAt().isBefore(request.endAt())
                || !request.saleStartAt().isBefore(request.saleEndAt())
                || request.saleEndAt().isAfter(request.startAt())) {
            throw new BusinessException(ErrorCode.SESSION_TIME_INVALID);
        }
    }

    private ActivityRecord requireOwned(long activityId, long merchantId) {
        ActivityRecord activity = commands.findActivity(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        if (activity.getMerchantId() != merchantId) {
            throw new BusinessException(ErrorCode.ACTIVITY_ACCESS_DENIED);
        }
        return activity;
    }

    private ActivityDetailRow requireOwnedDetail(long activityId, long merchantId) {
        ActivityDetailRow row = queries.findDetail(activityId);
        if (row == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        if (row.merchantId() != merchantId) {
            throw new BusinessException(ErrorCode.ACTIVITY_ACCESS_DENIED);
        }
        return row;
    }

    private VenueRecord requireOwnedVenue(long venueId, long merchantId) {
        VenueRecord venue = venueMapper.findById(venueId);
        if (venue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        if (venue.getMerchantId() != merchantId) {
            throw new BusinessException(ErrorCode.VENUE_ACCESS_DENIED);
        }
        return venue;
    }

    private ActivityRecord record(long merchantId, ActivityRequest request) {
        ActivityRecord activity = new ActivityRecord();
        activity.setMerchantId(merchantId);
        activity.setCategoryId(request.categoryId());
        activity.setTitle(request.title().trim());
        activity.setSummary(request.summary().trim());
        activity.setDescription(request.description().trim());
        activity.setCoverUrl(normalize(request.coverUrl()));
        activity.setCity(request.city().trim());
        activity.setVersion(request.version());
        return activity;
    }

    private SessionRecord session(long activityId, SessionRequest request) {
        SessionRecord session = new SessionRecord();
        session.setActivityId(activityId);
        session.setVenueId(request.venueId());
        session.setName(request.name().trim());
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        session.setSaleStartAt(request.saleStartAt());
        session.setSaleEndAt(request.saleEndAt());
        session.setVersion(request.version());
        return session;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
