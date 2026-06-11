package com.eventhub;

import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.SessionSeatSnapshotMapper;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.admin.dashboard.ActivityDashboardMapper;
import com.eventhub.admin.merchant.MerchantAdminMapper;
import com.eventhub.order.infrastructure.persistence.AvailabilityMapper;
import com.eventhub.order.infrastructure.persistence.OrderCommandMapper;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
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

    @MockitoBean
    private SessionSeatSnapshotMapper sessionSeatSnapshotMapper;

    @MockitoBean
    private AvailabilityMapper availabilityMapper;

    @MockitoBean
    private SeatLockMapper seatLockMapper;

    @MockitoBean
    private OrderCommandMapper orderCommandMapper;

    @MockitoBean
    private OrderQueryMapper orderQueryMapper;

    @Test
    void contextLoads() {}
}
