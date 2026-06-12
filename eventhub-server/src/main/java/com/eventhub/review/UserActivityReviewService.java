package com.eventhub.review;

import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.audit.OperationLogService;
import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserActivityReviewService {

    private final ActivityReviewMapper mapper;
    private final ActivityDetailCache cache;
    private final OperationLogService operationLogs;

    public UserActivityReviewService(
            ActivityReviewMapper mapper, ActivityDetailCache cache, OperationLogService operationLogs) {
        this.mapper = mapper;
        this.cache = cache;
        this.operationLogs = operationLogs;
    }

    public PageResponse<ActivityReviewView> publicReviews(long activityId, int page, int pageSize) {
        requirePublishedActivity(activityId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        List<ActivityReviewView> items = mapper.findPublic(activityId, (safePage - 1) * safeSize, safeSize);
        return PageResponse.of(items, safePage, safeSize, mapper.countPublic(activityId));
    }

    public ActivityReviewSummaryView summary(AuthenticatedUser user, long activityId) {
        requirePublishedActivity(activityId);
        return new ActivityReviewSummaryView(
                mapper.countPublic(activityId),
                mapper.averageRating(activityId),
                user != null && mapper.countEligibleOrder(user.id(), activityId) > 0);
    }

    public ActivityReviewView mine(AuthenticatedUser user, long activityId) {
        return mapper.findMine(user.id(), activityId);
    }

    @Transactional
    public ActivityReviewView save(AuthenticatedUser user, long activityId, ActivityReviewRequest request) {
        requirePublishedActivity(activityId);
        requireEligible(user.id(), activityId);
        String content = request.content().trim();
        ActivityReviewView existing = mapper.findMine(user.id(), activityId);
        if (existing != null && "HIDDEN".equals(existing.status())) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_HIDDEN);
        }
        if (existing == null) {
            try {
                mapper.insert(user.id(), activityId, request.rating(), content);
            } catch (DuplicateKeyException exception) {
                mapper.updateMine(user.id(), activityId, request.rating(), content);
            }
        } else {
            mapper.updateMine(user.id(), activityId, request.rating(), content);
        }
        cache.evict(activityId);
        return mapper.findMine(user.id(), activityId);
    }

    @Transactional
    public void deleteMine(AuthenticatedUser user, long activityId) {
        if (mapper.deleteMine(user.id(), activityId) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_NOT_FOUND);
        }
        cache.evict(activityId);
    }

    public PageResponse<ActivityReviewView> adminList(String status, String keyword, int page, int pageSize) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !List.of("PUBLISHED", "HIDDEN").contains(normalizedStatus)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "评价状态不正确");
        }
        String normalizedKeyword = normalize(keyword);
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        return PageResponse.of(
                mapper.findAdmin(normalizedStatus, normalizedKeyword, (safePage - 1) * safeSize, safeSize),
                safePage,
                safeSize,
                mapper.countAdmin(normalizedStatus, normalizedKeyword));
    }

    @Transactional
    public ActivityReviewView hide(AuthenticatedUser moderator, long reviewId, String reason) {
        ActivityReviewView review = requireReview(reviewId);
        if (mapper.hide(reviewId, moderator.id(), reason.trim()) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_HIDDEN);
        }
        operationLogs.record(
                moderator, null, "ACTIVITY_REVIEW_HIDE", "ACTIVITY_REVIEW", reviewId, "隐藏活动评价，原因：" + reason.trim());
        cache.evict(review.activityId());
        return requireReview(reviewId);
    }

    @Transactional
    public ActivityReviewView restore(AuthenticatedUser moderator, long reviewId) {
        ActivityReviewView review = requireReview(reviewId);
        if (mapper.restore(reviewId, moderator.id()) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_NOT_FOUND, "评价当前未隐藏");
        }
        operationLogs.record(moderator, null, "ACTIVITY_REVIEW_RESTORE", "ACTIVITY_REVIEW", reviewId, "恢复活动评价");
        cache.evict(review.activityId());
        return requireReview(reviewId);
    }

    private void requirePublishedActivity(long activityId) {
        if (mapper.countPublishedActivity(activityId) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_AVAILABLE);
        }
    }

    private void requireEligible(long userId, long activityId) {
        if (mapper.countEligibleOrder(userId, activityId) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_NOT_ELIGIBLE);
        }
    }

    private ActivityReviewView requireReview(long reviewId) {
        ActivityReviewView review = mapper.findById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_NOT_FOUND);
        }
        return review;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
