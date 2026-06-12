package com.eventhub.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.eventhub.common.error.BusinessException;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.security.ClientType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantToolExecutorTests {

    @Mock
    private AssistantToolMapper mapper;

    private final AuthenticatedUser user =
            new AuthenticatedUser(7L, "user", null, "User", List.of("USER"), List.of(), ClientType.USER_WEB, "s");

    @Test
    void ticketToolContainsOnlySanitizedItineraryAndStatus() {
        when(mapper.findTickets(7L, "UNUSED", 20))
                .thenReturn(List.of(new AssistantToolMapper.TicketToolRow(
                        31L,
                        22L,
                        "UNUSED",
                        null,
                        "城市夜游",
                        "周六场",
                        "老仓库",
                        "普通票",
                        "A区",
                        "3排",
                        "8座",
                        LocalDateTime.of(2026, 6, 20, 19, 30))));
        AssistantToolExecutor executor = new AssistantToolExecutor(mapper, new ObjectMapper().findAndRegisterModules());

        AssistantToolExecutor.ToolResult result = executor.execute("list_my_tickets", "{\"status\":\"UNUSED\"}", user);

        assertThat(result.json())
                .contains("城市夜游", "UNUSED", "3排")
                .doesNotContain("ticketNo", "credential", "phone", "token");
        assertThat(result.resources()).singleElement().satisfies(resource -> {
            assertThat(resource.href()).isEqualTo("/tickets/31");
            assertThat(resource.type()).isEqualTo("TICKET");
        });
    }

    @Test
    void rejectsUnsupportedTicketStatus() {
        AssistantToolExecutor executor = new AssistantToolExecutor(mapper, new ObjectMapper().findAndRegisterModules());

        assertThatThrownBy(() -> executor.execute("list_my_tickets", "{\"status\":\"SECRET\"}", user))
                .isInstanceOf(BusinessException.class);
    }
}
