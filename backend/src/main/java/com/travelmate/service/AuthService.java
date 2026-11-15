package com.travelmate.service;

import com.travelmate.config.JwtTokenProvider;
import com.travelmate.dto.AuthRequest;
import com.travelmate.dto.AuthResponse;
import com.travelmate.dto.RegisterRequest;
import com.travelmate.entity.Role;
import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AdminActivityLogService adminActivityLogService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager,
                       AdminActivityLogService adminActivityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.adminActivityLogService = adminActivityLogService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        // STRICT SECURITY REQUIREMENT: Public self-registration ALWAYS receives ROLE_USER.
        // Users can never register themselves as administrators.
        user.setRole(Role.ROLE_USER);
        user.setAge(request.getAge());
        user.setPhone(request.getPhone());

        User savedUser = userRepository.save(user);
        
        String token = jwtTokenProvider.createToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole().name(), savedUser.getAge());
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String email = authentication.getName();
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = jwtTokenProvider.createToken(email, user.getRole().name());
            return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(), user.getAge());
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public AuthResponse adminLogin(AuthRequest request, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: Account is not authorized as an administrator."
            );
        }

        String token = jwtTokenProvider.createToken(email, user.getRole().name());

        adminActivityLogService.logActivity(
                user.getEmail(),
                "LOGIN",
                "Administrator logged in successfully",
                ipAddress
        );

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(), user.getAge());
    }

    public void adminLogout(String email, String ipAddress) {
        adminActivityLogService.logActivity(
                email != null ? email : "Administrator",
                "LOGOUT",
                "Administrator logged out",
                ipAddress
        );
    }
}
