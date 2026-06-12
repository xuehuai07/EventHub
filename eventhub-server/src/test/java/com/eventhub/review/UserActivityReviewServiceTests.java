package com.eventhub.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.audit.OperationLogService;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.security.ClientType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityReviewServiceTests {

    @Mock
    private ActivityReviewMapper mapper;

    @Mock
    private ActivityDetailCache cache;

    @Mock
    private OperationLogService operationLogs;

    private UserActivityReviewService service;

    private final AuthenticatedUser user =
            new AuthenticatedUser(7, "user", null, "用户", List.of("USER"), List.of(), ClientType.USER_WEB, "s");

    @BeforeEach
    void setUp() {
        service = new UserActivityReviewService(mapper, cache, operationLogs);
    }

    @Test
    void eligibleBuyerCanCreateReview() {
        ActivityReviewView saved = review("PUBLISHED");
        when(mapper.countPublishedActivity(12)).thenReturn(1);
        when(mapper.countEligibleOrder(7, 12)).thenReturn(1);
        when(mapper.findMine(7, 12)).thenReturn(null, saved);

        ActivityReviewView result = service.save(user, 12, new ActivityReviewRequest(5, " 很棒 "));

        assertThat(result).isEqualTo(saved);
        verify(mapper).insert(7, 12, 5, "很棒");
        verify(cache).evict(12);
    }

    @Test
    void userWithoutStartedPaidOrderCannotReview() {
        when(mapper.countPublishedActivity(12)).thenReturn(1);
        when(mapper.countEligibleOrder(7, 12)).thenReturn(0);

        assertThatThrownBy(() -> service.save(user, 12, new ActivityReviewRequest(5, "很棒")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVITY_REVIEW_NOT_ELIGIBLE);

        verify(mapper, never()).insert(7, 12, 5, "很棒");
    }

    @Test
    void hiddenReviewIsEvictedFromPublicDetailAggregation() {
        AuthenticatedUser admin =
                new AuthenticatedUser(1, "admin", null, "管理员", List.of("ADMIN"), List.of(), ClientType.ADMIN_WEB, "s");
        ActivityReviewView review = review("PUBLISHED");
        when(mapper.findById(5)).thenReturn(review, review("HIDDEN"));
        when(mapper.hide(5, 1, "违规内容")).thenReturn(1);

        ActivityReviewView result = service.hide(admin, 5, "违规内容");

        assertThat(result.status()).isEqualTo("HIDDEN");
        verify(operationLogs).record(admin, null, "ACTIVITY_REVIEW_HIDE", "ACTIVITY_REVIEW", 5, "隐藏活动评价，原因：违规内容");
        verify(cache).evict(12);
    }

    private ActivityReviewView review(String status) {
        return new ActivityReviewView(
                5,
                12,
                "活动",
                7,
                "用户",
                5,
                "很棒",
                status,
                "HIDDEN".equals(status) ? "违规内容" : null,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
