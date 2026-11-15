package com.travelmate.service;

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

    @Value("${app.ml-service.base-url:${ml.service.url:http://localhost:8000}}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate;

    public MLIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Nullable
    public Map<String, Object> getRecommendations(Map<String, Object> request) {
        String url = mlServiceUrl + "/recommend";
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
            System.out.println("ML service query skipped or offline: " + e.getMessage());
            return null;
        }
    }
}
