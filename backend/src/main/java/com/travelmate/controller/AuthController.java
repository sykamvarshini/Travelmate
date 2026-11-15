package com.travelmate.controller;

import com.travelmate.dto.AuthRequest;
import com.travelmate.dto.AuthResponse;
import com.travelmate.dto.RegisterRequest;
import com.travelmate.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @NonNull RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @NonNull AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@RequestBody @NonNull AuthRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        AuthResponse response = authService.adminLogin(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/logout")
    public ResponseEntity<Map<String, String>> adminLogout(HttpServletRequest httpRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null && auth.isAuthenticated()) ? auth.getName() : "Administrator";
        String clientIp = getClientIp(httpRequest);
        authService.adminLogout(email, clientIp);
        return ResponseEntity.ok(Map.of("message", "Administrator logged out successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
