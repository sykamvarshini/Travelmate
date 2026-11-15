package com.travelmate.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Automatically loads key-value pairs from a local .env file into
 * System properties and the Spring Environment property sources.
 * 
 * Supports local development without checking credentials into version control.
 * In cloud / production environments, system environment variables take precedence.
 */
public class DotenvLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = Logger.getLogger(DotenvLoader.class.getName());
    private static boolean loaded = false;

    public static synchronized Map<String, Object> load() {
        if (loaded) {
            return Map.of();
        }

        Map<String, Object> envVars = new HashMap<>();
        File envFile = findEnvFile();

        if (envFile != null && envFile.exists() && envFile.isFile()) {
            logger.info("[DotenvLoader] Found local .env file at: " + envFile.getAbsolutePath());
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();

                        // Strip optional surrounding single or double quotes
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            if (value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                        }

                        envVars.put(key, value);

                        // Set into System properties if not already provided as an OS environment variable
                        if (System.getenv(key) == null && System.getProperty(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
                logger.info("[DotenvLoader] Successfully loaded " + envVars.size() + " environment variables from .env");
            } catch (IOException e) {
                logger.warning("[DotenvLoader] Error reading .env file: " + e.getMessage());
            }
        } else {
            logger.info("[DotenvLoader] No local .env file found; using system environment variables.");
        }

        loaded = true;
        return envVars;
    }

    private static File findEnvFile() {
        // Check current directory
        File candidate1 = new File(".env");
        if (candidate1.exists()) return candidate1;

        // Check parent directory (when running from backend/ subdirectory)
        File candidate2 = new File("../.env");
        if (candidate2.exists()) return candidate2;

        // Check backend subdirectory (when running from project root)
        File candidate3 = new File("backend/.env");
        if (candidate3.exists()) return candidate3;

        return null;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, Object> props = load();
        if (!props.isEmpty()) {
            applicationContext.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", props));
        }
    }
}
