package com.eventhub.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.activity.infrastructure.cache.ActivityDetailCache;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.security.ClientType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityFavoriteServiceTests {

    @Mock
    private ActivityFavoriteMapper mapper;

    @Mock
    private ActivityDetailCache cache;

    private ActivityFavoriteService service;

    private final AuthenticatedUser user =
            new AuthenticatedUser(7, "user", null, "用户", List.of("USER"), List.of(), ClientType.USER_WEB, "s");

    @BeforeEach
    void setUp() {
        service = new ActivityFavoriteService(mapper, cache);
    }

    @Test
    void favoriteIsIdempotentAndEvictsDetailCache() {
        when(mapper.countPublishedActivity(12)).thenReturn(1);

        ActivityFavoriteStatusView result = service.favorite(user, 12);

        assertThat(result.favorited()).isTrue();
        verify(mapper).insert(7, 12);
        verify(cache).evict(12);
    }

    @Test
    void cannotFavoriteUnavailableActivity() {
        when(mapper.countPublishedActivity(12)).thenReturn(0);

        assertThatThrownBy(() -> service.favorite(user, 12))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVITY_NOT_AVAILABLE);
    }

    @Test
    void unfavoriteIsIdempotent() {
        assertThat(service.unfavorite(user, 12).favorited()).isFalse();
        verify(mapper).delete(7, 12);
        verify(cache).evict(12);
    }
}
