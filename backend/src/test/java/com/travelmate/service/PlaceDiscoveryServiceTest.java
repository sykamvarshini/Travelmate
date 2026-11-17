package com.travelmate.service;

import com.travelmate.repository.AttractionCategoryRepository;
import com.travelmate.repository.AttractionRepository;
import com.travelmate.repository.CityRepository;
import com.travelmate.repository.StateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlaceDiscoveryServiceTest {

    @Test
    void autocompleteCachesResultsAndThrottlesNominatimRequests() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AttractionRepository attractionRepository = mock(AttractionRepository.class);
        CityRepository cityRepository = mock(CityRepository.class);
        PlaceDiscoveryService service = new PlaceDiscoveryService(
                restTemplate,
                attractionRepository,
                cityRepository,
                mock(StateRepository.class),
                mock(AttractionCategoryRepository.class));
        ReflectionTestUtils.setField(service, "nominatimBaseUrl", "https://nominatim.openstreetmap.org");
        when(cityRepository.searchCities(anyString())).thenReturn(List.of());
        when(attractionRepository.searchAttractions(anyString())).thenReturn(List.of());
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        )).thenReturn(ResponseEntity.ok(List.of()));

        assertTrue(service.autocomplete("goa").isEmpty());
        assertTrue(service.autocomplete("goa").isEmpty());
        assertTrue(service.autocomplete("goap").isEmpty());

        verify(restTemplate, times(1)).exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        );
    }
}
