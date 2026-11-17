package com.travelmate.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class MLIntegrationServiceTest {

    @Test
    void recommendationsAreSkippedWhenProductionMlServiceIsNotConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MLIntegrationService service = new MLIntegrationService(restTemplate);
        ReflectionTestUtils.setField(service, "mlServiceUrl", "");

        assertNull(service.getRecommendations(Map.of("city", "Goa")));
        verifyNoInteractions(restTemplate);
    }
}
