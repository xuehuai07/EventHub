package com.eventhub;

import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.admin.dashboard.ActivityDashboardMapper;
import com.eventhub.admin.merchant.MerchantAdminMapper;
import com.eventhub.user.UserIdentityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EventhubServerApplicationTests {

    @MockitoBean
    private UserIdentityMapper userIdentityMapper;

    @MockitoBean
    private ActivityCommandMapper activityCommandMapper;

    @MockitoBean
    private ActivityQueryMapper activityQueryMapper;

    @MockitoBean
    private VenueMapper venueMapper;

    @MockitoBean
    private MerchantAdminMapper merchantAdminMapper;

    @MockitoBean
    private ActivityDashboardMapper activityDashboardMapper;

    @Test
    void contextLoads() {}
}
