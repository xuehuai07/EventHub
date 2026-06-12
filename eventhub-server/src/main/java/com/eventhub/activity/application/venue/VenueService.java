package com.eventhub.activity.application.venue;

import com.eventhub.activity.domain.SeatMode;
import com.eventhub.activity.dto.SeatAreaView;
import com.eventhub.activity.dto.SeatBlockRequest;
import com.eventhub.activity.dto.SeatGenerationRequest;
import com.eventhub.activity.dto.VenueRequest;
import com.eventhub.activity.dto.VenueView;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.activity.infrastructure.persistence.VenueRecord;
import com.eventhub.activity.infrastructure.persistence.VenueSeatRecord;
import com.eventhub.audit.OperationLogService;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.user.MerchantBinding;
import com.eventhub.user.MerchantContextService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueService {

    private static final int MAX_SEATS = 5000;

    private final VenueMapper mapper;
    private final MerchantContextService merchantContext;
    private final OperationLogService operationLogs;

    public VenueService(VenueMapper mapper, MerchantContextService merchantContext, OperationLogService operationLogs) {
        this.mapper = mapper;
        this.merchantContext = merchantContext;
        this.operationLogs = operationLogs;
    }

    public List<VenueView> list(AuthenticatedUser user) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        return mapper.findByMerchant(merchantId).stream().map(this::view).toList();
    }

    @Transactional
    public VenueView create(AuthenticatedUser user, VenueRequest request) {
        MerchantBinding binding = merchantContext.requireActiveMerchant(user);
        VenueRecord venue = record(binding.merchantId(), request);
        mapper.insert(venue);
        operationLogs.record(
                user, binding.merchantId(), "VENUE_CREATE", "VENUE", venue.getId(), "创建场馆：" + venue.getName());
        return view(venue);
    }

    @Transactional
    public VenueView update(AuthenticatedUser user, long venueId, VenueRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        VenueRecord current = requireOwned(venueId, merchantId);
        VenueRecord updated = record(merchantId, request);
        updated.setId(current.getId());
        if (mapper.update(updated) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_VERSION_CONFLICT);
        }
        operationLogs.record(user, merchantId, "VENUE_UPDATE", "VENUE", venueId, "更新场馆：" + updated.getName());
        return view(mapper.findById(venueId));
    }

    @Transactional
    public VenueView generateSeats(AuthenticatedUser user, long venueId, SeatGenerationRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        VenueRecord venue = requireOwned(venueId, merchantId);
        if (venue.getSeatMode() != SeatMode.FIXED) {
            throw new BusinessException(ErrorCode.TICKET_TYPE_INVALID, "只有固定座位场馆可以生成座位");
        }
        List<VenueSeatRecord> seats = buildSeats(venueId, request.blocks());
        mapper.deleteSeats(venueId);
        for (int start = 0; start < seats.size(); start += 500) {
            mapper.insertSeats(seats.subList(start, Math.min(start + 500, seats.size())));
        }
        mapper.updateCapacity(venueId, seats.size());
        operationLogs.record(
                user, merchantId, "VENUE_SEATS_REBUILD", "VENUE", venueId, "重建固定座位，共 " + seats.size() + " 个");
        return view(mapper.findById(venueId));
    }

    private List<VenueSeatRecord> buildSeats(long venueId, List<SeatBlockRequest> blocks) {
        List<VenueSeatRecord> seats = new ArrayList<>();
        int sort = 0;
        for (SeatBlockRequest block : blocks) {
            for (int row = 1; row <= block.rowCount(); row++) {
                String rowLabel = block.rowPrefix() + row;
                for (int number = 1; number <= block.seatsPerRow(); number++) {
                    String seatCode = block.areaName() + "-" + rowLabel + "-" + number;
                    seats.add(new VenueSeatRecord(
                            venueId,
                            block.areaName(),
                            rowLabel,
                            String.valueOf(number),
                            seatCode,
                            block.seatGrade(),
                            sort++));
                    if (seats.size() > MAX_SEATS) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "单个场馆最多配置 5000 个座位");
                    }
                }
            }
        }
        return seats;
    }

    private VenueRecord requireOwned(long venueId, long merchantId) {
        VenueRecord venue = mapper.findById(venueId);
        if (venue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        if (venue.getMerchantId() != merchantId) {
            throw new BusinessException(ErrorCode.VENUE_ACCESS_DENIED);
        }
        return venue;
    }

    private VenueRecord record(long merchantId, VenueRequest request) {
        VenueRecord venue = new VenueRecord();
        venue.setMerchantId(merchantId);
        venue.setName(request.name().trim());
        venue.setCity(request.city().trim());
        venue.setAddress(request.address().trim());
        venue.setSeatMode(request.seatMode());
        venue.setCapacity(request.capacity());
        venue.setVersion(request.version());
        return venue;
    }

    private VenueView view(VenueRecord venue) {
        List<SeatAreaView> areas = venue.getId() == null ? List.of() : mapper.findSeatAreas(venue.getId());
        return new VenueView(
                venue.getId(),
                venue.getName(),
                venue.getCity(),
                venue.getAddress(),
                venue.getSeatMode(),
                venue.getCapacity(),
                venue.getStatus() == null ? "ACTIVE" : venue.getStatus(),
                venue.getVersion(),
                areas);
    }
}
