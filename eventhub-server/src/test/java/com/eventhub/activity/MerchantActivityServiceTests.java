package com.eventhub.activity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.activity.application.activity.ActivityViewAssembler;
import com.eventhub.activity.application.activity.MerchantActivityService;
import com.eventhub.activity.application.activity.SessionSeatSnapshotService;
import com.eventhub.activity.domain.ActivityStateMachine;
import com.eventhub.activity.domain.ActivityStatus;
import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivityRequest;
import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityDetailRow;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityRecord;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.audit.OperationLogService;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.security.ClientType;
import com.eventhub.user.MerchantBinding;
import com.eventhub.user.MerchantContextService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantActivityServiceTests {

    @Mock
    private ActivityCommandMapper commands;

    @Mock
    private ActivityQueryMapper queries;

    @Mock
    private VenueMapper venueMapper;

    @Mock
    private MerchantContextService merchantContext;

    @Mock
    private ActivityViewAssembler assembler;

    @Mock
    private ActivityDetailCache cache;

    @Mock
    private SessionSeatSnapshotService seatSnapshots;

    @Mock
    private OperationLogService operationLogs;

    private MerchantActivityService service;

    private final AuthenticatedUser user =
            new AuthenticatedUser(7, "merchant", null, "商家", List.of("MERCHANT"), List.of(), ClientType.ADMIN_WEB, "s");

    @BeforeEach
    void setUp() {
        service = new MerchantActivityService(
                commands,
                queries,
                venueMapper,
                merchantContext,
                new ActivityStateMachine(),
                assembler,
                cache,
                seatSnapshots,
                operationLogs);
        when(merchantContext.requireActiveMerchant(user))
                .thenReturn(new MerchantBinding(9, "测试商家", "ACTIVE", "ACTIVE"));
    }

    @Test
    void publishedActivityUpdatesDisplayContentOnly() {
        ActivityRecord current = activity(ActivityStatus.PUBLISHED);
        ActivityRequest request = request("原活动", 3, "上海", 5);
        ActivityDetailRow row = detailRow(6);
        ActivityDetailView view = detailView(6);
        when(commands.findActivity(1)).thenReturn(current);
        when(commands.updatePublishedContent(any(ActivityRecord.class))).thenReturn(1);
        when(queries.findDetail(1)).thenReturn(row);
        when(assembler.detail(row)).thenReturn(view);

        service.update(user, 1, request);

        verify(commands).updatePublishedContent(any(ActivityRecord.class));
        verify(commands, never()).updateActivity(any(ActivityRecord.class));
        verify(cache).evict(1);
    }

    @Test
    void publishedActivityRejectsImmutableFieldChanges() {
        when(commands.findActivity(1)).thenReturn(activity(ActivityStatus.PUBLISHED));

        assertThatThrownBy(() -> service.update(user, 1, request("修改后的标题", 3, "上海", 5)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVITY_STATUS_INVALID);

        verify(commands, never()).updatePublishedContent(any(ActivityRecord.class));
        verify(cache, never()).evict(1);
    }

    @Test
    void draftActivityKeepsFullUpdateBehavior() {
        ActivityRecord current = activity(ActivityStatus.DRAFT);
        ActivityRequest request = request("新标题", 4, "杭州", 5);
        ActivityDetailRow row = detailRow(6);
        when(commands.findActivity(1)).thenReturn(current);
        when(commands.updateActivity(any(ActivityRecord.class))).thenReturn(1);
        when(queries.findDetail(1)).thenReturn(row);
        when(assembler.detail(row)).thenReturn(detailView(6));

        service.update(user, 1, request);

        verify(commands).updateActivity(any(ActivityRecord.class));
        verify(commands, never()).updatePublishedContent(any(ActivityRecord.class));
    }

    @Test
    void publishedActivityKeepsOptimisticLock() {
        when(commands.findActivity(1)).thenReturn(activity(ActivityStatus.PUBLISHED));
        when(commands.updatePublishedContent(any(ActivityRecord.class))).thenReturn(0);

        assertThatThrownBy(() -> service.update(user, 1, request("原活动", 3, "上海", 5)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVITY_VERSION_CONFLICT);
    }

    private ActivityRecord activity(ActivityStatus status) {
        ActivityRecord activity = new ActivityRecord();
        activity.setId(1L);
        activity.setMerchantId(9);
        activity.setCategoryId(3);
        activity.setTitle("原活动");
        activity.setSummary("原简介");
        activity.setDescription("原详情");
        activity.setCoverUrl("/old.png");
        activity.setCity("上海");
        activity.setStatus(status);
        activity.setVersion(5);
        return activity;
    }

    private ActivityRequest request(String title, long categoryId, String city, int version) {
        return new ActivityRequest(categoryId, title, "新简介", "新详情", "/new.png", city, version);
    }

    private ActivityDetailRow detailRow(int version) {
        return new ActivityDetailRow(
                1,
                9,
                3,
                "音乐",
                "测试商家",
                "原活动",
                "新简介",
                "新详情",
                "/new.png",
                "上海",
                ActivityStatus.PUBLISHED,
                null,
                version,
                0,
                0,
                null);
    }

    private ActivityDetailView detailView(int version) {
        return new ActivityDetailView(
                1,
                3,
                "音乐",
                "测试商家",
                "原活动",
                "新简介",
                "新详情",
                "/new.png",
                "上海",
                ActivityStatus.PUBLISHED,
                null,
                version,
                0,
                0,
                null,
                List.of());
    }
}
