package com.eventhub.security;

import com.eventhub.user.UserIdentityMapper;
import com.eventhub.user.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class UserIdentityService {

    private final UserIdentityMapper mapper;

    public UserIdentityService(UserIdentityMapper mapper) {
        this.mapper = mapper;
    }

    public UserRecord findByIdentifier(String identifier) {
        return mapper.findByIdentifier(identifier);
    }

    public AuthenticatedUser loadUser(long userId, ClientType clientType, String sessionId) {
        UserRecord user = mapper.findById(userId);
        if (user == null) {
            return null;
        }
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getDisplayName(),
                mapper.findRoles(userId),
                mapper.findPermissions(userId),
                clientType,
                sessionId);
    }
}
