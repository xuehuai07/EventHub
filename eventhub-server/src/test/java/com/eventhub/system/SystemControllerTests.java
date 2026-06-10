package com.eventhub.system;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventhub.common.request.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SystemControllerTests {

    @Autowired
    private MockMvc mockMvc;

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
