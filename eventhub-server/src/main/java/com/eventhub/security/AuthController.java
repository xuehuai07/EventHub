package com.eventhub.security;

import com.eventhub.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "注册普通用户")
    @PostMapping("/register")
    ApiResponse<AuthUserView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        SessionTokens tokens = authService.login(request, clientIp(servletRequest));
        cookieService.writeSessionCookies(
                servletResponse, request.clientType(), tokens.refreshToken(), tokens.csrfToken());
        return ApiResponse.success(response(tokens));
    }

    @Operation(summary = "刷新登录状态")
    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(
            @RequestHeader("X-Client-Type") ClientType clientType,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        SessionTokens tokens = authService.refresh(
                clientType,
                cookieService.readCookie(request, clientType.refreshCookie()),
                csrfToken,
                cookieService.readCookie(request, clientType.csrfCookie()));
        cookieService.writeSessionCookies(response, clientType, tokens.refreshToken(), tokens.csrfToken());
        return ApiResponse.success(response(tokens));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @RequestHeader("X-Client-Type") ClientType clientType,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(
                clientType,
                cookieService.readCookie(request, clientType.refreshCookie()),
                csrfToken,
                cookieService.readCookie(request, clientType.csrfCookie()));
        cookieService.clearSessionCookies(response, clientType);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    ApiResponse<AuthUserView> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(AuthUserView.from(user));
    }

    private AuthResponse response(SessionTokens tokens) {
        return new AuthResponse(
                tokens.accessToken(), authService.accessTokenExpiresInSeconds(), AuthUserView.from(tokens.user()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
