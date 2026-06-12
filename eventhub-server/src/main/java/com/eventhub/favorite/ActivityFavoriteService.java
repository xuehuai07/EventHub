package com.eventhub.favorite;

import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityFavoriteService {

    private final ActivityFavoriteMapper mapper;
    private final ActivityDetailCache cache;

    public ActivityFavoriteService(ActivityFavoriteMapper mapper, ActivityDetailCache cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    public PageResponse<ActivityFavoriteView> list(AuthenticatedUser user, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        List<ActivityFavoriteView> items = mapper.findByUser(user.id(), (safePage - 1) * safeSize, safeSize);
        return PageResponse.of(items, safePage, safeSize, mapper.countByUser(user.id()));
    }

    public ActivityFavoriteStatusView status(AuthenticatedUser user, long activityId) {
        return new ActivityFavoriteStatusView(mapper.countFavorite(user.id(), activityId) > 0);
    }

    @Transactional
    public ActivityFavoriteStatusView favorite(AuthenticatedUser user, long activityId) {
        if (mapper.countPublishedActivity(activityId) == 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_AVAILABLE);
        }
        mapper.insert(user.id(), activityId);
        cache.evict(activityId);
        return new ActivityFavoriteStatusView(true);
    }

    @Transactional
    public ActivityFavoriteStatusView unfavorite(AuthenticatedUser user, long activityId) {
        mapper.delete(user.id(), activityId);
        cache.evict(activityId);
        return new ActivityFavoriteStatusView(false);
    }
}
