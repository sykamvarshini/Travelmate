package com.travelmate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    public Map<String, Object> verifyAndGetLocation(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(nominatimBaseUrl + "/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("countrycodes", "in")
                .queryParam("limit", "1")
                .toUriString();

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "TravelMateAI/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<List> responseEntity = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    List.class
            );
            
            List<Map> response = responseEntity.getBody();
            if (response != null && !response.isEmpty()) {
                return (Map<String, Object>) response.get(0);
            }
        } catch (Exception e) {
            System.err.println("Error calling Nominatim API: " + e.getMessage());
        }
        return null;
    }
}
