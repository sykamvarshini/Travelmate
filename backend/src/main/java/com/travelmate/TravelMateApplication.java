package com.travelmate;

import com.travelmate.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelMateApplication {
    public static void main(String[] args) {
        // Load local .env into system properties if present before Spring context bootstraps
        DotenvLoader.load();
        SpringApplication.run(TravelMateApplication.class, args);
    }
}
