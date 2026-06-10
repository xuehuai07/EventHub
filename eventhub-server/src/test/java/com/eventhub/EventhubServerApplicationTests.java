package com.eventhub;

import com.eventhub.user.UserIdentityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EventhubServerApplicationTests {

    @MockitoBean
    private UserIdentityMapper userIdentityMapper;

    @Test
    void contextLoads() {}
}
