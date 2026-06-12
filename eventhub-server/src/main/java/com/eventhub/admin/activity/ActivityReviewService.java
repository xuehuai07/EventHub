package com.eventhub.admin.activity;

import com.eventhub.activity.application.activity.ActivityViewAssembler;
import com.eventhub.activity.domain.ActivityStateMachine;
import com.eventhub.activity.dto.ActivityDetailView;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityDetailRow;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityRecord;
import com.eventhub.audit.OperationLogService;
import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityReviewService {

    private final ActivityCommandMapper commands;
    private final ActivityQueryMapper queries;
    private final ActivityStateMachine stateMachine;
    private final ActivityViewAssembler assembler;
    private final ActivityDetailCache cache;
    private final OperationLogService operationLogs;

    public ActivityReviewService(
            ActivityCommandMapper commands,
            ActivityQueryMapper queries,
            ActivityStateMachine stateMachine,
            ActivityViewAssembler assembler,
            ActivityDetailCache cache,
            OperationLogService operationLogs) {
        this.commands = commands;
        this.queries = queries;
        this.stateMachine = stateMachine;
        this.assembler = assembler;
        this.cache = cache;
        this.operationLogs = operationLogs;
    }

    public PageResponse<ActivitySummaryView> pending(String keyword, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        String normalized = normalize(keyword);
        List<ActivitySummaryView> items = queries.findPendingReviews(normalized, (safePage - 1) * safeSize, safeSize);
        return PageResponse.of(items, safePage, safeSize, queries.countPendingReviews(normalized));
    }

    public ActivityDetailView detail(long activityId) {
        ActivityDetailRow row = queries.findDetail(activityId);
        if (row == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return assembler.detail(row);
    }

    @Transactional
    public ActivityDetailView approve(AuthenticatedUser reviewer, long activityId) {
        ActivityRecord activity = requireActivity(activityId);
        stateMachine.requireReviewable(activity.getStatus());
        if (commands.approve(activityId, reviewer.id()) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
        cache.evict(activityId);
        operationLogs.record(reviewer, null, "ACTIVITY_APPROVE", "ACTIVITY", activityId, "审核通过活动");
        return detail(activityId);
    }

    @Transactional
    public ActivityDetailView reject(AuthenticatedUser reviewer, long activityId, String reason) {
        ActivityRecord activity = requireActivity(activityId);
        stateMachine.requireReviewable(activity.getStatus());
        if (commands.reject(activityId, reviewer.id(), reason.trim()) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
        cache.evict(activityId);
        operationLogs.record(reviewer, null, "ACTIVITY_REJECT", "ACTIVITY", activityId, "驳回活动，原因：" + reason.trim());
        return detail(activityId);
    }

    @Transactional
    public ActivityDetailView offShelf(AuthenticatedUser reviewer, long activityId, String reason) {
        ActivityRecord activity = requireActivity(activityId);
        stateMachine.requirePublished(activity.getStatus());
        if (commands.offShelf(activityId, reviewer.id(), reason.trim()) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID);
        }
        cache.evict(activityId);
        operationLogs.record(reviewer, null, "ACTIVITY_OFF_SHELF", "ACTIVITY", activityId, "下架活动，原因：" + reason.trim());
        return detail(activityId);
    }

    private ActivityRecord requireActivity(long activityId) {
        ActivityRecord activity = commands.findActivity(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
