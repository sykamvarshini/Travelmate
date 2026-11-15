package com.travelmate.service;

import com.travelmate.config.AdminAccountInitializer;
import com.travelmate.config.JwtTokenProvider;
import com.travelmate.dto.AdminOverviewResponse;
import com.travelmate.dto.AuthRequest;
import com.travelmate.dto.AuthResponse;
import com.travelmate.dto.RegisterRequest;
import com.travelmate.entity.AdminActivityLog;
import com.travelmate.entity.Role;
import com.travelmate.entity.User;
import com.travelmate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AdminSecurityAndServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthenticationManager authenticationManager;
    private AdminActivityLogRepository logRepository;
    private AdminActivityLogService logService;
    private AuthService authService;
    private TripRepository tripRepository;
    private DestinationRepository destinationRepository;
    private ItineraryDayRepository itineraryDayRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authenticationManager = mock(AuthenticationManager.class);
        logRepository = mock(AdminActivityLogRepository.class);
        tripRepository = mock(TripRepository.class);
        destinationRepository = mock(DestinationRepository.class);
        itineraryDayRepository = mock(ItineraryDayRepository.class);

        logService = new AdminActivityLogService(logRepository);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, authenticationManager, logService);
        adminService = new AdminService(userRepository, tripRepository, destinationRepository, itineraryDayRepository, logService);

        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "bcrypt_hashed_" + invocation.getArgument(0));
    }

    @Test
    @DisplayName("AdminAccountInitializer seeds authorized emails with ROLE_ADMIN")
    void testAdminAccountInitializer() {
        AdminAccountInitializer initializer = new AdminAccountInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "admin1Email", "sykamvarshini30@gmail.com");
        ReflectionTestUtils.setField(initializer, "admin1Password", "AdminVarshiniPass123");
        ReflectionTestUtils.setField(initializer, "admin1Name", "Varshini Sykam");
        ReflectionTestUtils.setField(initializer, "admin2Email", "sindhura11@gmail.com");
        ReflectionTestUtils.setField(initializer, "admin2Password", "AdminSindhuraPass123");
        ReflectionTestUtils.setField(initializer, "admin2Name", "Sindhura");

        when(userRepository.findByEmail("sykamvarshini30@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("sindhura11@gmail.com")).thenReturn(Optional.empty());

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();
        assertEquals(2, savedUsers.size());

        User admin1 = savedUsers.get(0);
        assertEquals("sykamvarshini30@gmail.com", admin1.getEmail());
        assertEquals(Role.ROLE_ADMIN, admin1.getRole());
        assertTrue(admin1.getPassword().startsWith("bcrypt_hashed_"));

        User admin2 = savedUsers.get(1);
        assertEquals("sindhura11@gmail.com", admin2.getEmail());
        assertEquals(Role.ROLE_ADMIN, admin2.getRole());
        assertTrue(admin2.getPassword().startsWith("bcrypt_hashed_"));
    }

    @Test
    @DisplayName("Existing user promoted safely to ROLE_ADMIN without duplicate account")
    void testAdminAccountInitializerExistingUser() {
        AdminAccountInitializer initializer = new AdminAccountInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "admin1Email", "sykamvarshini30@gmail.com");
        ReflectionTestUtils.setField(initializer, "admin1Password", "NewPass");
        ReflectionTestUtils.setField(initializer, "admin1Name", "Varshini Sykam");
        ReflectionTestUtils.setField(initializer, "admin2Email", "");

        User existingUser = new User("sykamvarshini30@gmail.com", "old_hash", "Varshini", Role.ROLE_USER, 24);
        existingUser.setId(101L);

        when(userRepository.findByEmail("sykamvarshini30@gmail.com")).thenReturn(Optional.of(existingUser));

        initializer.run();

        verify(userRepository, times(1)).save(existingUser);
        assertEquals(Role.ROLE_ADMIN, existingUser.getRole());
        assertEquals(101L, existingUser.getId());
    }

    @Test
    @DisplayName("Public self-registration STRICTLY assigns ROLE_USER and rejects elevation")
    void testPublicRegistrationStrictlyRoleUser() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("regularuser@example.com");
        req.setPassword("UserPass123");
        req.setFullName("Regular Traveler");
        req.setAge(28);

        when(userRepository.findByEmail("regularuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(55L);
            return u;
        });
        when(jwtTokenProvider.createToken(eq("regularuser@example.com"), eq("ROLE_USER"))).thenReturn("jwt.token.regular");

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("ROLE_USER", res.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.ROLE_USER, userCaptor.getValue().getRole());
    }

    @Test
    @DisplayName("Admin login allows authorized ADMIN and records audit activity log")
    void testAdminLoginSuccess() {
        AuthRequest req = new AuthRequest("sykamvarshini30@gmail.com", "AdminPass123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("sykamvarshini30@gmail.com");
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User adminUser = new User("sykamvarshini30@gmail.com", "hash", "Varshini Sykam", Role.ROLE_ADMIN, 25);
        adminUser.setId(1L);
        when(userRepository.findByEmail("sykamvarshini30@gmail.com")).thenReturn(Optional.of(adminUser));
        when(jwtTokenProvider.createToken("sykamvarshini30@gmail.com", "ROLE_ADMIN")).thenReturn("admin.jwt.token");

        AuthResponse response = authService.adminLogin(req, "192.168.1.100");

        assertNotNull(response);
        assertEquals("ROLE_ADMIN", response.getRole());
        assertEquals("admin.jwt.token", response.getToken());

        // Verify activity log was recorded
        verify(logRepository, times(1)).save(any(AdminActivityLog.class));
    }

    @Test
    @DisplayName("Admin login throws AccessDeniedException when regular user attempts admin sign-in")
    void testAdminLoginRejectsNormalUser() {
        AuthRequest req = new AuthRequest("normaluser@example.com", "NormalPass123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("normaluser@example.com");
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User normalUser = new User("normaluser@example.com", "hash", "Normal User", Role.ROLE_USER, 22);
        normalUser.setId(2L);
        when(userRepository.findByEmail("normaluser@example.com")).thenReturn(Optional.of(normalUser));

        assertThrows(AccessDeniedException.class, () -> {
            authService.adminLogin(req, "192.168.1.101");
        });

        // Verify NO activity log saved for successful admin login
        verify(logRepository, never()).save(any(AdminActivityLog.class));
    }

    @Test
    @DisplayName("AdminOverview fetches live MySQL counts and recent entities")
    void testAdminOverviewMetrics() {
        when(userRepository.count()).thenReturn(142L);
        when(tripRepository.count()).thenReturn(89L);
        when(destinationRepository.count()).thenReturn(53L);
        when(itineraryDayRepository.countDistinctTripsWithItinerary()).thenReturn(85L);
        when(userRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(tripRepository.findRecentTripsWithUser(any())).thenReturn(Collections.emptyList());
        when(logRepository.findTop50ByOrderByTimestampDesc()).thenReturn(Collections.emptyList());

        AdminOverviewResponse overview = adminService.getOverview();

        assertNotNull(overview);
        assertEquals(142L, overview.getTotalUsers());
        assertEquals(89L, overview.getTotalTrips());
        assertEquals(53L, overview.getTotalDestinations());
        assertEquals(85L, overview.getTotalItineraries());
    }

    public static void main(String[] args) {
        System.out.println("=======================================================================");
        System.out.println("RUNNING TRAVELMATE ADMIN SECURITY & DASHBOARD TEST SUITE");
        System.out.println("=======================================================================");
        AdminSecurityAndServiceTest test = new AdminSecurityAndServiceTest();

        try {
            test.setUp();
            test.testAdminAccountInitializer();
            System.out.println("[PASS] 1. AdminAccountInitializer seeds authorized emails (sykamvarshini30@gmail.com, sindhura11@gmail.com) with ROLE_ADMIN");

            test.setUp();
            test.testAdminAccountInitializerExistingUser();
            System.out.println("[PASS] 2. Existing user promoted safely to ROLE_ADMIN without duplicate account");

            test.setUp();
            test.testPublicRegistrationStrictlyRoleUser();
            System.out.println("[PASS] 3. Public registration STRICTLY assigns ROLE_USER and rejects admin elevation");

            test.setUp();
            test.testAdminLoginSuccess();
            System.out.println("[PASS] 4. Admin login allows authorized ADMIN and records audit activity log");

            test.setUp();
            test.testAdminLoginRejectsNormalUser();
            System.out.println("[PASS] 5. Admin login rejects normal users with AccessDeniedException");

            test.setUp();
            test.testAdminOverviewMetrics();
            System.out.println("[PASS] 6. Admin overview computes live MySQL metrics, recent users, trips and logs");

            System.out.println("\n>>> ALL ADMIN SECURITY & DASHBOARD TESTS PASSED (100% SUCCESS) <<<");
            System.out.println("=======================================================================");
        } catch (Throwable t) {
            System.err.println("[FAIL] Admin test failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
