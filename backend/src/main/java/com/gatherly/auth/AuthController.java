package com.gatherly.auth;

import com.gatherly.common.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final int authRateLimitPerMinute;

    public AuthController(
            AuthService authService,
            RateLimiterService rateLimiterService,
            @Value("${security.auth.rate-limit-per-minute:60}") int authRateLimitPerMinute) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.authRateLimitPerMinute = authRateLimitPerMinute;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        enforceRateLimit(servletRequest);
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        enforceRateLimit(servletRequest);
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(user);
    }

    private void enforceRateLimit(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        rateLimiterService.checkLimit(
                "auth:" + clientIp,
                authRateLimitPerMinute,
                "Too many authentication attempts. Please wait a moment before trying again."
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
