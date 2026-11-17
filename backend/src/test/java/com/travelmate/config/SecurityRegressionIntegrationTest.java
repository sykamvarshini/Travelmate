package com.travelmate.config;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityRegressionIntegrationTest {

    @Test
    @DisplayName("1. DotenvLoader correctly finds and loads .env properties into system properties")
    void testDotenvLoader() {
        Map<String, Object> loaded = DotenvLoader.load();
        assertNotNull(loaded, "DotenvLoader should return loaded property map");
        assertTrue(loaded.containsKey("DB_NAME") || System.getProperty("DB_NAME") != null, "DB_NAME must be loaded");
        assertEquals("travelmate_ai", System.getProperty("DB_NAME", (String) loaded.get("DB_NAME")));
    }

    @Test
    @DisplayName("2. JwtTokenProvider generates and validates token with secure JWT_SECRET")
    void testJwtTokenProviderSuccess() {
        String testSecret = "DummyTestSecretKeyForTestingJwtTokenProvider32BytesLong!";
        JwtTokenProvider provider = new JwtTokenProvider(testSecret, 3600000L);

        String token = provider.createToken("testuser@travelmate.com", "ROLE_USER");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT token must have 3 segments");

        assertTrue(provider.validateToken(token));
        assertEquals("testuser@travelmate.com", provider.getEmail(token));
        assertEquals("ROLE_USER", provider.getRole(token));
    }

    @Test
    @DisplayName("3. JwtTokenProvider fails safely with IllegalStateException when JWT_SECRET is missing or empty")
    void testJwtTokenProviderMissingSecretThrows() {
        IllegalStateException exEmpty = assertThrows(IllegalStateException.class, () -> {
            new JwtTokenProvider("", 3600000L);
        });
        assertTrue(exEmpty.getMessage().contains("JWT_SECRET"), "Error message must indicate missing JWT_SECRET");

        IllegalStateException exNull = assertThrows(IllegalStateException.class, () -> {
            new JwtTokenProvider(null, 3600000L);
        });
        assertTrue(exNull.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("4. JwtTokenProvider fails safely when JWT_SECRET is shorter than 32 characters (256 bits)")
    void testJwtTokenProviderShortSecretThrows() {
        IllegalStateException exShort = assertThrows(IllegalStateException.class, () -> {
            new JwtTokenProvider("short-secret-under-32-bytes", 3600000L);
        });
        assertTrue(exShort.getMessage().contains("32 characters"), "Must reject keys shorter than 32 characters");
    }

    @Test
    @DisplayName("5. application.yml contains NO hardcoded database password or JWT secret")
    void testApplicationYmlNoHardcodedSecrets() throws Exception {
        File ymlFile = new File("backend/src/main/resources/application.yml");
        if (!ymlFile.exists()) {
            ymlFile = new File("src/main/resources/application.yml");
        }
        assertTrue(ymlFile.exists(), "application.yml must exist");

        String content = java.nio.file.Files.readString(ymlFile.toPath());
        assertFalse(content.matches("(?s).*password:\\s*[^$\\s]+.*"), "Passwords must be parameterized with environment variables");
        assertFalse(content.matches("(?s).*secret:\\s*[^$\\s]+.*"), "Secrets must be parameterized with environment variables");

        // Verify placeholder structure
        assertTrue(content.contains("${SPRING_DATASOURCE_PASSWORD:${DB_PASSWORD}}"), "Must support both SPRING_DATASOURCE_PASSWORD and DB_PASSWORD");
        assertTrue(content.contains("${JWT_SECRET}"), "Must reference JWT_SECRET variable");
        assertTrue(content.contains("${SPRING_DATASOURCE_URL:"), "Must support SPRING_DATASOURCE_URL for cloud DBs");
    }

    @Test
    @DisplayName("6. application-prod.yml exists, supports cloud configuration, and has zero hardcoded secrets")
    void testApplicationProdYml() throws Exception {
        File prodYmlFile = new File("backend/src/main/resources/application-prod.yml");
        if (!prodYmlFile.exists()) {
            prodYmlFile = new File("src/main/resources/application-prod.yml");
        }
        assertTrue(prodYmlFile.exists(), "application-prod.yml must exist");

        String content = java.nio.file.Files.readString(prodYmlFile.toPath());
        assertFalse(content.matches("(?s).*password:\\s*[^$\\s]+.*"), "Production configuration must contain no plaintext passwords");
        assertTrue(content.contains("validate"), "Production profile must configure schema validation");
        assertTrue(content.contains("base-url: ${ML_SERVICE_URL:}"),
                "Production must not assume the ML service is running on localhost");

        Map<?, ?> config = new Yaml().load(content);
        Map<?, ?> spring = (Map<?, ?>) config.get("spring");
        Map<?, ?> datasource = (Map<?, ?>) spring.get("datasource");
        Map<?, ?> hikari = (Map<?, ?>) datasource.get("hikari");
        long maxLifetime = ((Number) hikari.get("max-lifetime")).longValue();
        long keepaliveTime = ((Number) hikari.get("keepalive-time")).longValue();
        assertEquals(1200000L, maxLifetime);
        assertEquals(240000L, keepaliveTime);
        assertTrue(keepaliveTime < maxLifetime, "Hikari keepalive must be shorter than maxLifetime");
        assertEquals(600000L, ((Number) hikari.get("idle-timeout")).longValue());
        assertEquals(20000L, ((Number) hikari.get("connection-timeout")).longValue());
        assertEquals(5000L, ((Number) hikari.get("validation-timeout")).longValue());
    }

    @Test
    @DisplayName("7. .env is ignored by Git (.gitignore rule check)")
    void testGitignoreExcludesEnv() throws Exception {
        File gitignoreFile = new File(".gitignore");
        if (!gitignoreFile.exists()) {
            gitignoreFile = new File("../.gitignore");
        }
        assertTrue(gitignoreFile.exists(), ".gitignore must exist");

        String content = java.nio.file.Files.readString(gitignoreFile.toPath());
        assertTrue(content.contains(".env"), ".gitignore must contain .env rule");
        assertTrue(content.contains("!.env.example"), ".gitignore must keep !.env.example exception");
    }

    public static void main(String[] args) {
        System.out.println("=======================================================================");
        System.out.println("RUNNING TRAVELMATE SECURITY & CONFIGURATION REGRESSION TEST SUITE");
        System.out.println("=======================================================================");
        SecurityRegressionIntegrationTest runner = new SecurityRegressionIntegrationTest();
        int passed = 0;

        try {
            runner.testDotenvLoader();
            System.out.println("[PASS] 1. DotenvLoader correctly finds and loads .env properties into system properties");
            passed++;

            runner.testJwtTokenProviderSuccess();
            System.out.println("[PASS] 2. JwtTokenProvider generates and validates token with secure JWT_SECRET");
            passed++;

            runner.testJwtTokenProviderMissingSecretThrows();
            System.out.println("[PASS] 3. JwtTokenProvider fails safely with IllegalStateException when JWT_SECRET is missing or empty");
            passed++;

            runner.testJwtTokenProviderShortSecretThrows();
            System.out.println("[PASS] 4. JwtTokenProvider fails safely when JWT_SECRET is shorter than 32 characters (256 bits)");
            passed++;

            runner.testApplicationYmlNoHardcodedSecrets();
            System.out.println("[PASS] 5. application.yml contains NO hardcoded database password or JWT secret (consistent DB_PASSWORD and SPRING_DATASOURCE_PASSWORD handling)");
            passed++;

            runner.testApplicationProdYml();
            System.out.println("[PASS] 6. application-prod.yml exists, supports cloud configuration, and has zero hardcoded secrets");
            passed++;

            runner.testGitignoreExcludesEnv();
            System.out.println("[PASS] 7. .env is properly ignored by Git (.gitignore rule check)");
            passed++;

            System.out.println("\n>>> ALL 7 SECURITY CONFIGURATION REGRESSION TESTS PASSED (100% SUCCESS) <<<");
            System.out.println("=======================================================================");
        } catch (Throwable t) {
            System.err.println("[FAIL] Security regression test failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
