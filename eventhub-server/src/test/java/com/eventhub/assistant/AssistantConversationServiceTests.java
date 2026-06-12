package com.eventhub.assistant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.security.ClientType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantConversationServiceTests {

    @Mock
    private AssistantConversationMapper mapper;

    @Test
    void rejectsConversationOwnedByAnotherUser() {
        AssistantConversationRecord record = new AssistantConversationRecord();
        record.setId(44L);
        record.setUserId(9L);
        when(mapper.findConversation(44L)).thenReturn(record);
        AssistantConversationService service = new AssistantConversationService(mapper, new ObjectMapper());
        AuthenticatedUser user =
                new AuthenticatedUser(7L, "user", null, "User", List.of("USER"), List.of(), ClientType.USER_WEB, "s");

        assertThatThrownBy(() -> service.requireOwned(user, 44L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                                ((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AI_CONVERSATION_ACCESS_DENIED));
    }
}
