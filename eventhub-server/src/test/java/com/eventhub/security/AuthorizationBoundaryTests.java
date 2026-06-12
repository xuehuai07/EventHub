package com.eventhub.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventhub.activity.infrastructure.persistence.ActivityCommandMapper;
import com.eventhub.activity.infrastructure.persistence.ActivityQueryMapper;
import com.eventhub.activity.infrastructure.persistence.SessionSeatSnapshotMapper;
import com.eventhub.activity.infrastructure.persistence.VenueMapper;
import com.eventhub.admin.dashboard.ActivityDashboardMapper;
import com.eventhub.admin.dashboard.OperationsDashboardMapper;
import com.eventhub.admin.merchant.MerchantAdminMapper;
import com.eventhub.assistant.AssistantConversationMapper;
import com.eventhub.assistant.AssistantToolMapper;
import com.eventhub.audit.OperationLogMapper;
import com.eventhub.favorite.ActivityFavoriteMapper;
import com.eventhub.notification.NotificationMapper;
import com.eventhub.order.infrastructure.messaging.MessageConsumeMapper;
import com.eventhub.order.infrastructure.outbox.OutboxEventMapper;
import com.eventhub.order.infrastructure.persistence.AvailabilityMapper;
import com.eventhub.order.infrastructure.persistence.OrderCommandMapper;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
import com.eventhub.review.ActivityReviewMapper;
import com.eventhub.ticket.TicketMapper;
import com.eventhub.user.UserIdentityMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationBoundaryTests {
    @MockitoBean
    private OperationsDashboardMapper operationsDashboardMapper;

    @MockitoBean
    private OperationLogMapper operationLogMapper;

    @MockitoBean
    private ActivityFavoriteMapper activityFavoriteMapper;

    @MockitoBean
    private ActivityReviewMapper activityReviewMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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

    @MockitoBean
    private OutboxEventMapper outboxEventMapper;

    @MockitoBean
    private MessageConsumeMapper messageConsumeMapper;

    @MockitoBean
    private TicketMapper ticketMapper;

    @MockitoBean
    private NotificationMapper notificationMapper;

    @MockitoBean
    private AssistantConversationMapper assistantConversationMapper;

    @MockitoBean
    private AssistantToolMapper assistantToolMapper;

    @Test
    void userCannotAccessMerchantOrAdminEndpoints() throws Exception {
        String token = token(List.of("USER"), ClientType.USER_WEB);

        mockMvc.perform(get("/api/merchant/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        mockMvc.perform(get("/api/admin/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        mockMvc.perform(multipart("/api/merchant/uploads/activity-cover")
                        .file(new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] {1}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void merchantCanAccessMerchantButNotAdminEndpoint() throws Exception {
        String token = token(List.of("MERCHANT"), ClientType.ADMIN_WEB);

        mockMvc.perform(get("/api/merchant/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessBothBoundaries() throws Exception {
        String token = token(List.of("ADMIN"), ClientType.ADMIN_WEB);

        mockMvc.perform(get("/api/merchant/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String token(List<String> roles, ClientType clientType) {
        return jwtService.createAccessToken(new AuthenticatedUser(
                1L, "tester", null, "测试用户", roles, List.of("PROFILE_READ"), clientType, "test-session"));
    }
}
