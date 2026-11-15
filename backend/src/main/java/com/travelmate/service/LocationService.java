package com.travelmate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    @Value("${app.external-apis.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    private final RestTemplate restTemplate;

    public LocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Searches for a location in India using Nominatim API.
     */
    @Nullable
    public Map<String, Object> verifyAndGetLocation(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        String url = UriComponentsBuilder.fromHttpUrl(nominatimBaseUrl + "/search")
                .queryParam("q", query.trim())
                .queryParam("format", "json")
                .queryParam("countrycodes", "in")
                .queryParam("limit", "1")
                .toUriString();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TravelMateAI/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> response = responseEntity.getBody();
            if (response != null && !response.isEmpty()) {
                return response.get(0);
            }
        } catch (Exception e) {
            System.err.println("Error calling Nominatim API: " + e.getMessage());
        }
        return null;
    }
}
