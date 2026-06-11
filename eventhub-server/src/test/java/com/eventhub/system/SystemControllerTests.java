package com.eventhub.system;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.SessionSeatSnapshotMapper;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.admin.dashboard.ActivityDashboardMapper;
import com.eventhub.admin.merchant.MerchantAdminMapper;
import com.eventhub.common.request.RequestIdFilter;
import com.eventhub.order.infrastructure.persistence.AvailabilityMapper;
import com.eventhub.order.infrastructure.persistence.OrderCommandMapper;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
import com.eventhub.user.UserIdentityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SystemControllerTests {

    @Autowired
    private MockMvc mockMvc;

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
    void returnsSystemStatusWithGeneratedRequestId() throws Exception {
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, matchesPattern("[a-f0-9-]{36}")))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.service").value("eventhub-server"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void preservesValidClientRequestId() throws Exception {
        String requestId = "client-request-123";

        mockMvc.perform(get("/api/system/status").header(RequestIdFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void returnsJsonForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }
}
