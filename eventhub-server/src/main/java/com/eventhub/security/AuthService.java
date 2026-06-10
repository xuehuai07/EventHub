package com.eventhub.security;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.user.MerchantBinding;
import com.eventhub.user.UserIdentityMapper;
import com.eventhub.user.UserRecord;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserIdentityMapper mapper;
    private final UserIdentityService identityService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthProperties properties;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(
            UserIdentityMapper mapper,
            UserIdentityService identityService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore,
            AuthProperties properties,
            LoginRateLimiter loginRateLimiter) {
        this.mapper = mapper;
        this.identityService = identityService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.properties = properties;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public AuthUserView register(RegisterRequest request) {
        String username = request.normalizedUsername();
        String phone = request.normalizedPhone();
        if (mapper.countByIdentifiers(username, phone) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或手机号已被注册");
        }
        UserRecord user = new UserRecord(
                username,
                phone,
                passwordEncoder.encode(request.password()),
                request.displayName().trim());
        try {
            mapper.insert(user);
            mapper.assignRole(user.getId(), "USER");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或手机号已被注册");
        }
        return AuthUserView.from(identityService.loadUser(user.getId(), ClientType.USER_WEB, "register"));
    }

    public SessionTokens login(LoginRequest request, String ip) {
        String identifier = request.identifier().trim();
        loginRateLimiter.check(identifier, ip);
        UserRecord user = identityService.findByIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(identifier, ip);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        AuthenticatedUser authenticatedUser =
                identityService.loadUser(user.getId(), request.clientType(), randomToken(18));
        assertClientAllowed(authenticatedUser);
        loginRateLimiter.clear(identifier, ip);
        return createSession(authenticatedUser);
    }

    public SessionTokens refresh(ClientType clientType, String refreshToken, String csrfHeader, String csrfCookie) {
        TokenParts parts = parseToken(refreshToken);
        RefreshSession session = refreshTokenStore.find(parts.tokenId());
        if (session == null
                || session.clientType() != clientType
                || !session.tokenHash().equals(TokenDigest.sha256(refreshToken))) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        if (csrfHeader == null
                || !csrfHeader.equals(csrfCookie)
                || !session.csrfHash().equals(TokenDigest.sha256(csrfHeader))) {
            throw new BusinessException(ErrorCode.AUTH_CSRF_INVALID);
        }
        if (!refreshTokenStore.consume(parts.tokenId(), session)) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        AuthenticatedUser user = identityService.loadUser(session.userId(), clientType, randomToken(18));
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        assertClientAllowed(user);
        return createSession(user);
    }

    public void logout(ClientType clientType, String refreshToken, String csrfHeader, String csrfCookie) {
        if (refreshToken == null) {
            return;
        }
        TokenParts parts = parseToken(refreshToken);
        RefreshSession session = refreshTokenStore.find(parts.tokenId());
        if (session == null) {
            return;
        }
        if (session.clientType() != clientType
                || csrfHeader == null
                || !csrfHeader.equals(csrfCookie)
                || !session.csrfHash().equals(TokenDigest.sha256(csrfHeader))) {
            throw new BusinessException(ErrorCode.AUTH_CSRF_INVALID);
        }
        refreshTokenStore.delete(parts.tokenId());
    }

    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private SessionTokens createSession(AuthenticatedUser user) {
        String tokenId = randomToken(18);
        String refreshToken = tokenId + "." + randomToken(32);
        String csrfToken = randomToken(24);
        refreshTokenStore.save(
                tokenId,
                new RefreshSession(
                        user.id(),
                        user.clientType(),
                        TokenDigest.sha256(refreshToken),
                        TokenDigest.sha256(csrfToken),
                        Instant.now().plus(properties.refreshTokenTtl()).getEpochSecond()),
                properties.refreshTokenTtl());
        return new SessionTokens(jwtService.createAccessToken(user), refreshToken, csrfToken, user);
    }

    private void assertClientAllowed(AuthenticatedUser user) {
        List<String> roles = user.roles();
        boolean allowed =
                switch (user.clientType()) {
                    case USER_WEB -> roles.contains("USER") && !roles.contains("MERCHANT") && !roles.contains("ADMIN");
                    case ADMIN_WEB -> roles.contains("MERCHANT") || roles.contains("ADMIN");
                };
        if (!allowed) {
            throw new BusinessException(ErrorCode.AUTH_CLIENT_NOT_ALLOWED);
        }
        if (user.clientType() == ClientType.ADMIN_WEB && roles.contains("MERCHANT") && !roles.contains("ADMIN")) {
            MerchantBinding binding = mapper.findMerchantBinding(user.id());
            if (binding == null || !binding.active()) {
                throw new BusinessException(ErrorCode.MERCHANT_INACTIVE);
            }
        }
    }

    private TokenParts parseToken(String token) {
        if (token == null) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        return new TokenParts(parts[0]);
    }

    private String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record TokenParts(String tokenId) {}
}
