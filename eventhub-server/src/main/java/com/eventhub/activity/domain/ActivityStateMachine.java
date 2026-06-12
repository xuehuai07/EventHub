package com.eventhub.activity.domain;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ActivityStateMachine {

    public void requireEditable(ActivityStatus status) {
        if (status != ActivityStatus.DRAFT && status != ActivityStatus.REJECTED) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
    }

    public void requireContentEditable(ActivityStatus status) {
        if (status != ActivityStatus.DRAFT && status != ActivityStatus.REJECTED && status != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
    }

    public void requireSubmittable(ActivityStatus status) {
        requireEditable(status);
    }

    public void requireReviewable(ActivityStatus status) {
        if (status != ActivityStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
    }

    public void requirePublished(ActivityStatus status) {
        if (status != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
    }
}
