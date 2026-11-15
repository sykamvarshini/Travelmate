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

/**
 * Initializes default administrator accounts from environment variables.
 * No sensitive credentials or passwords are hardcoded in the codebase.
 */
@Component
@Order(1) // Run before general data initialization
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(AdminAccountInitializer.class.getName());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.admin1.email:${ADMIN1_EMAIL:}}")
    private String admin1Email;

    @Value("${app.admin.admin1.password:${ADMIN1_PASSWORD:}}")
    private String admin1Password;

    @Value("${app.admin.admin1.name:${ADMIN1_NAME:Administrator 1}}")
    private String admin1Name;

    @Value("${app.admin.admin2.email:${ADMIN2_EMAIL:}}")
    private String admin2Email;

    @Value("${app.admin.admin2.password:${ADMIN2_PASSWORD:}}")
    private String admin2Password;

    @Value("${app.admin.admin2.name:${ADMIN2_NAME:Administrator 2}}")
    private String admin2Name;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Verifying authorized administrator accounts from environment configuration...");
        if (StringUtils.hasText(admin1Email)) {
            initAdminAccount(admin1Email, admin1Name, admin1Password);
        } else {
            logger.info("ADMIN1_EMAIL is not set in environment; skipping Admin 1 seeding.");
        }

        if (StringUtils.hasText(admin2Email)) {
            initAdminAccount(admin2Email, admin2Name, admin2Password);
        } else {
            logger.info("ADMIN2_EMAIL is not set in environment; skipping Admin 2 seeding.");
        }
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
            if (!StringUtils.hasText(rawPassword)) {
                logger.warning("Admin account for " + normalizedEmail + " requested, but no password was provided in environment variables. Account creation skipped for security.");
                return;
            }

            // Create new admin account
            User newAdmin = new User();
            newAdmin.setEmail(normalizedEmail);
            newAdmin.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : "Administrator");
            newAdmin.setRole(Role.ROLE_ADMIN);
            newAdmin.setAge(25);
            newAdmin.setPassword(passwordEncoder.encode(rawPassword));

            userRepository.save(newAdmin);
            logger.info("Created authorized administrator account: " + normalizedEmail);
        }
    }
}
