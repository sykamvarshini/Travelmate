package com.travelmate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MLIntegrationService {
    private static final Logger log = LoggerFactory.getLogger(MLIntegrationService.class);

    @Value("${app.ml-service.base-url:${ml.service.url:http://localhost:8000}}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate;

    public MLIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Nullable
    public Map<String, Object> getRecommendations(Map<String, Object> request) {
        if (mlServiceUrl == null || mlServiceUrl.isBlank()) {
            log.debug("ML recommendations are disabled because ML_SERVICE_URL is not configured.");
            return null;
        }

        String url = mlServiceUrl.replaceAll("/+$", "") + "/recommend";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.warn("ML recommendations are unavailable; continuing without them: {}", e.getMessage());
            return null;
        }
    }
}
