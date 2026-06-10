package com.eventhub.activity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventhub.activity.domain.ActivityStateMachine;
import com.eventhub.activity.domain.ActivityStatus;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class ActivityStateMachineTests {

    private final ActivityStateMachine stateMachine = new ActivityStateMachine();

    @Test
    void draftAndRejectedActivitiesAreEditable() {
        assertThatCode(() -> stateMachine.requireEditable(ActivityStatus.DRAFT)).doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.requireEditable(ActivityStatus.REJECTED))
                .doesNotThrowAnyException();
    }

    @Test
    void publishedActivityCannotBeEdited() {
        assertThatThrownBy(() -> stateMachine.requireEditable(ActivityStatus.PUBLISHED))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVITY_STATUS_INVALID);
    }

    @Test
    void onlyPendingReviewCanBeApprovedOrRejected() {
        assertThatCode(() -> stateMachine.requireReviewable(ActivityStatus.PENDING_REVIEW))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> stateMachine.requireReviewable(ActivityStatus.DRAFT))
                .isInstanceOf(BusinessException.class);
    }
}
