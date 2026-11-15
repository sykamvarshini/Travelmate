package com.travelmate.config;

import com.travelmate.entity.Role;
import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.logging.Logger;

@Component
@Order(1) // Run before general data initialization
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(AdminAccountInitializer.class.getName());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.admin1.email:sykamvarshini30@gmail.com}")
    private String admin1Email;

    @Value("${app.admin.admin1.password:${ADMIN1_PASSWORD:Admin@TravelMate2026}}")
    private String admin1Password;

    @Value("${app.admin.admin1.name:${ADMIN1_NAME:Varshini Sykam}}")
    private String admin1Name;

    @Value("${app.admin.admin2.email:sindhura11@gmail.com}")
    private String admin2Email;

    @Value("${app.admin.admin2.password:${ADMIN2_PASSWORD:Admin@TravelMate2026}}")
    private String admin2Password;

    @Value("${app.admin.admin2.name:${ADMIN2_NAME:Sindhura}}")
    private String admin2Name;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Verifying authorized administrator accounts...");
        initAdminAccount(admin1Email, admin1Name, admin1Password);
        initAdminAccount(admin2Email, admin2Name, admin2Password);
        logger.info("Administrator accounts verification completed.");
    }

    private void initAdminAccount(String email, String fullName, String rawPassword) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            boolean modified = false;

            // Ensure role is ROLE_ADMIN
            if (existingUser.getRole() != Role.ROLE_ADMIN) {
                existingUser.setRole(Role.ROLE_ADMIN);
                modified = true;
                logger.info("Upgraded existing user " + normalizedEmail + " to ROLE_ADMIN.");
            }

            // If initial password is provided via environment variables, update it securely
            if (StringUtils.hasText(rawPassword)) {
                existingUser.setPassword(passwordEncoder.encode(rawPassword));
                modified = true;
            }

            if (modified) {
                userRepository.save(existingUser);
            }
        } else {
            // Create new admin account
            User newAdmin = new User();
            newAdmin.setEmail(normalizedEmail);
            newAdmin.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : "Administrator");
            newAdmin.setRole(Role.ROLE_ADMIN);
            newAdmin.setAge(25);
            newAdmin.setPassword(passwordEncoder.encode(StringUtils.hasText(rawPassword) ? rawPassword : "Admin@TravelMate2026"));

            userRepository.save(newAdmin);
            logger.info("Created authorized administrator account: " + normalizedEmail);
        }
    }
}
