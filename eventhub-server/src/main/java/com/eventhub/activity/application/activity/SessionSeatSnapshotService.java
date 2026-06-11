package com.eventhub.activity.application.activity;

import com.eventhub.activity.infrastructure.persistence.SessionSeatSnapshotMapper;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class SessionSeatSnapshotService {

    private final SessionSeatSnapshotMapper mapper;

    public SessionSeatSnapshotService(SessionSeatSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    public void rebuild(long sessionId, long venueId) {
        mapper.deleteBySession(sessionId);
        if (!"FIXED".equals(mapper.findSeatMode(venueId))) {
            return;
        }
        mapper.insertFixedSnapshot(sessionId, venueId);
        mapper.synchronizeFixedStock(sessionId);
    }

    public void requireComplete(long activityId) {
        if (mapper.countActivityUnmappedSeats(activityId) > 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_INCOMPLETE, "固定座位等级必须匹配有效票档");
        }
    }
}
