package com.travelmate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class WeatherServiceTest {

    private RestTemplate restTemplate;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        weatherService = new WeatherService(restTemplate);
        ReflectionTestUtils.setField(weatherService, "openMeteoBaseUrl", "https://api.open-meteo.com/v1/forecast");
    }

    @Test
    void testGetCurrentConditions_PreservesRealPrecipitationProbability() {
        // Mock Open-Meteo response with weather_code 80 (passing shower) and low rain prob (15%)
        Map<String, Object> current = new HashMap<>();
        current.put("temperature_2m", 29.4);
        current.put("weather_code", 80);
        current.put("precipitation", 0.0);

        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(LocalDate.now().toString()));
        daily.put("precipitation_probability_max", List.of(15.0));
        daily.put("temperature_2m_max", List.of(31.0));

        Map<String, Object> body = new HashMap<>();
        body.put("current", current);
        body.put("daily", daily);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        // Call for Fort Aguada, Goa (15.4925, 73.7737)
        Map<String, Object> result = weatherService.getCurrentConditions(15.4925, 73.7737);

        assertNotNull(result);
        assertEquals(29.4, (Double) result.get("temperatureC"), 0.1);
        // Rain probability must reflect the API's actual 15%, NOT be overridden to 85%
        assertEquals(15.0, (Double) result.get("rainProbability"), 0.1);
        assertEquals("Passing Showers", result.get("weatherCondition"));
        assertNull(result.get("precipitationMm"), "Daily rainfall must remain unavailable when Open-Meteo omits its daily sum");
        assertEquals(0.0, (Double) result.get("currentPrecipitationMm"), 0.1);
        assertEquals(80, result.get("weatherCode"));
        assertTrue((Boolean) result.get("isLive"));
    }

    @Test
    void testThunderstormWith43PercentRain_WeatherServicePreservesExact43Percent() {
        // Mock Open-Meteo response with Thunderstorm (code 95), 16.8 mm precipitation, and 43% rain probability
        Map<String, Object> current = new HashMap<>();
        current.put("temperature_2m", 28.0);
        current.put("weather_code", 95);
        current.put("precipitation", 16.8);

        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(LocalDate.now().toString()));
        daily.put("precipitation_probability_max", List.of(43.0));
        daily.put("precipitation_sum", List.of(16.8));
        daily.put("weather_code", List.of(95));
        daily.put("temperature_2m_max", List.of(28.0));

        Map<String, Object> body = new HashMap<>();
        body.put("current", current);
        body.put("daily", daily);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = weatherService.getCurrentConditions(15.4925, 73.7737);

        assertNotNull(result);
        assertEquals(43.0, (Double) result.get("rainProbability"), 0.01, "Rain probability must remain 43%");
        assertEquals(16.8, (Double) result.get("precipitationMm"), 0.01, "Precipitation must remain 16.8 mm");
        assertEquals("Thunderstorm", result.get("weatherCondition"));
        assertEquals(95, result.get("weatherCode"));
        assertEquals(28.0, (Double) result.get("temperatureC"), 0.01);
    }

    @Test
    void testWarningFlow_WithThunderstormAndHeavyRain_PreservesExact43PercentProbability() {
        // Setup Weather API response returning:
        // Rain probability: 43%
        // Precipitation: 16.8 mm
        // Condition: Thunderstorm (code 95)
        Map<String, Object> current = new HashMap<>();
        current.put("temperature_2m", 28.0);
        current.put("weather_code", 95);
        current.put("precipitation", 16.8);

        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(LocalDate.now().toString()));
        daily.put("precipitation_probability_max", List.of(43.0));
        daily.put("precipitation_sum", List.of(16.8));
        daily.put("weather_code", List.of(95));
        daily.put("temperature_2m_max", List.of(28.0));

        Map<String, Object> body = new HashMap<>();
        body.put("current", current);
        body.put("daily", daily);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        // Create Mocks for TripService dependencies
        com.travelmate.repository.TripRepository tripRepo = mock(com.travelmate.repository.TripRepository.class);
        com.travelmate.repository.UserRepository userRepo = mock(com.travelmate.repository.UserRepository.class);
        MLIntegrationService mlService = mock(MLIntegrationService.class);
        LocationService locService = mock(LocationService.class);
        PlaceDiscoveryService placeService = mock(PlaceDiscoveryService.class);
        com.travelmate.repository.ItineraryItemRepository itemRepo = mock(com.travelmate.repository.ItineraryItemRepository.class);
        com.travelmate.repository.TripMemberRepository memberRepo = mock(com.travelmate.repository.TripMemberRepository.class);
        RouteOptimizationService routeService = new RouteOptimizationService();
        com.travelmate.repository.AttractionRepository attrRepo = mock(com.travelmate.repository.AttractionRepository.class);

        TripService tripService = new TripService(
                tripRepo, userRepo, mlService, locService, placeService,
                weatherService, itemRepo, memberRepo, routeService, attrRepo
        );

        // Build Test Trip with Fort Aguada as next activity
        com.travelmate.entity.Trip trip = new com.travelmate.entity.Trip();
        trip.setId(100L);
        trip.setStartDate(LocalDate.now());

        com.travelmate.entity.ItineraryDay day = new com.travelmate.entity.ItineraryDay();
        day.setId(1L);
        day.setDate(LocalDate.now());
        day.setDayNumber(1);

        com.travelmate.entity.ItineraryItem item1 = new com.travelmate.entity.ItineraryItem();
        item1.setId(101L);
        item1.setTitle("Sinquerim Beach");
        item1.setStatus("COMPLETED");
        item1.setIndoorOutdoor("Outdoor");
        item1.setItineraryDay(day);

        com.travelmate.entity.ItineraryItem nextItem = new com.travelmate.entity.ItineraryItem();
        nextItem.setId(102L);
        nextItem.setTitle("Fort Aguada");
        nextItem.setStatus("SCHEDULED");
        nextItem.setIndoorOutdoor("Outdoor");
        nextItem.setLatitude(15.4925);
        nextItem.setLongitude(73.7737);
        nextItem.setItineraryDay(day);

        day.getItems().add(item1);
        day.getItems().add(nextItem);
        trip.getDays().add(day);

        when(tripRepo.findById(100L)).thenReturn(Optional.of(trip));

        // Call checkNextActivity for live weather (weatherOverride = null)
        Map<String, Object> checkResult = tripService.checkNextActivity("traveler@travelmate.com", 100L, 101L, null);

        assertNotNull(checkResult);
        // 1. Severe weather warning must trigger because of Thunderstorm & 16.8mm rain
        assertFalse((Boolean) checkResult.get("isSuitable"), "Warning must trigger for Thunderstorm and heavy rain");
        assertTrue((Boolean) checkResult.get("userActionRequired"));

        // 2. The displayed rain probability must be EXACTLY 43%, NOT overridden to 85%
        assertEquals(43L, ((Number) checkResult.get("rainProbability")).longValue(), "Rain probability must remain 43%");
        assertEquals(16.8, (Double) checkResult.get("precipitationMm"), 0.01, "Precipitation must remain 16.8 mm");
        assertFalse((Boolean) checkResult.get("isSimulated"), "Must be live weather, not simulated");

        // 3. Condition message must state 43% and 16.8 mm without 85%
        String alertMsg = (String) checkResult.get("alertMessage");
        assertNotNull(alertMsg);
        assertTrue(alertMsg.contains("43%"), "Alert message must include exact 43% probability: " + alertMsg);
        assertTrue(alertMsg.contains("16.8 mm"), "Alert message must include precipitation amount: " + alertMsg);
        assertFalse(alertMsg.contains("85%"), "Alert message must NOT contain 85%: " + alertMsg);
    }

    @Test
    void testGetConditionsForLocationAndDate_MatchesTargetFutureDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = LocalDate.now().plusDays(2);

        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(LocalDate.now().toString(), tomorrow.toString(), dayAfter.toString()));
        daily.put("temperature_2m_max", List.of(30.0, 28.5, 32.0));
        daily.put("temperature_2m_min", List.of(22.0, 21.0, 23.0));
        daily.put("precipitation_probability_max", List.of(5.0, 70.0, 10.0));
        daily.put("precipitation_sum", List.of(0.0, 8.5, 0.0));
        daily.put("weather_code", List.of(0, 65, 1)); // Clear, Heavy Rain, Mainly Clear

        Map<String, Object> body = new HashMap<>();
        body.put("daily", daily);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        // Query tomorrow's date
        Map<String, Object> resultTomorrow = weatherService.getConditionsForLocationAndDate(15.4925, 73.7737, tomorrow);

        assertNotNull(resultTomorrow);
        assertEquals(28.5, (Double) resultTomorrow.get("temperatureC"), 0.1);
        assertEquals(70.0, (Double) resultTomorrow.get("rainProbability"), 0.1);
        assertEquals("Heavy Rain", resultTomorrow.get("weatherCondition"));
        assertEquals(8.5, (Double) resultTomorrow.get("precipitationMm"), 0.1);
        assertEquals(65, resultTomorrow.get("weatherCode"));
        assertEquals(tomorrow.toString(), resultTomorrow.get("forecastDate"));
    }

    @Test
    void requestedDateMissingFromForecastIsUnavailableRatherThanUsingAnotherDay() {
        Map<String, Object> body = createTwoDayForecast();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        LocalDate unavailableDate = LocalDate.now().plusDays(3);
        Map<String, Object> result = weatherService.getConditionsForLocationAndDate(
                15.4925, 73.7737, unavailableDate);

        assertFalse((Boolean) result.get("isAvailable"));
        assertNull(result.get("rainProbability"));
        assertNull(result.get("temperatureC"));
        assertNull(result.get("weatherCode"));
        assertEquals(unavailableDate.toString(), result.get("forecastDate"));
    }

    @Test
    void matchedForecastDateWithNoWeatherValuesIsUnavailable() {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(LocalDate.now().toString()));
        body.put("daily", daily);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = weatherService.getCurrentConditions(15.4925, 73.7737);

        assertFalse((Boolean) result.get("isAvailable"));
        assertNull(result.get("temperatureC"));
        assertNull(result.get("rainProbability"));
        assertNull(result.get("weatherCode"));
    }

    @Test
    void testGetConditionsForLocationAndDate_UnavailableForDistantFutureBeyond16Days() {
        LocalDate farFuture = LocalDate.now().plusDays(45);

        Map<String, Object> result = weatherService.getConditionsForLocationAndDate(15.4925, 73.7737, farFuture);

        assertNotNull(result);
        assertFalse((Boolean) result.get("isAvailable"), "Must indicate weather data is unavailable beyond 16 days");
        assertFalse((Boolean) result.get("isLive"));
        assertNull(result.get("rainProbability"), "Must not invent fake rain probability");
        assertNull(result.get("precipitationMm"), "Must not invent fake precipitation mm");
        assertEquals("Forecast Temporarily Unavailable", result.get("weatherCondition"));
        // API should not be called for dates beyond 16 days
        verify(restTemplate, never()).exchange(anyString(), any(), any(), ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());
    }

    @Test
    void testFallbackWhenApiFails_DoesNotInventValues() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new RuntimeException("Connection timed out"));

        Map<String, Object> result = weatherService.getCurrentConditions(15.4925, 73.7737);

        assertNotNull(result);
        assertFalse((Boolean) result.get("isAvailable"), "Must report unavailable when API fails");
        assertFalse((Boolean) result.get("isLive"));
        assertEquals("Forecast Temporarily Unavailable", result.get("weatherCondition"));
        assertNull(result.get("rainProbability"), "Must not fabricate 10% rain probability on failure");
        assertNull(result.get("precipitationMm"), "Must not fabricate precipitation on failure");
        assertNull(result.get("temperatureC"), "Must not fabricate 29C temperature on failure");
    }

    @Test
    void testTennetiPark_ConsistentWeatherBetweenItineraryAndNextActivityCheck() {
        // Destination: Tenneti Park, Visakhapatnam (17.747, 83.344)
        // Scenario reported by user:
        // Daily Max Rain probability: 73%
        // Daily Precipitation sum: 106 mm
        // Current hourly precipitation: 12 mm
        // Condition: Heavy Rain (code 65)
        LocalDate today = LocalDate.now();

        Map<String, Object> current = new HashMap<>();
        current.put("temperature_2m", 27.5);
        current.put("weather_code", 65);
        current.put("precipitation", 12.0); // Current hourly rainfall rate

        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(today.toString()));
        daily.put("precipitation_probability_max", List.of(73.0)); // Daily maximum probability
        daily.put("precipitation_sum", List.of(106.0)); // Daily accumulated rainfall
        daily.put("weather_code", List.of(65));
        daily.put("temperature_2m_max", List.of(27.5));

        Map<String, Object> body = new HashMap<>();
        body.put("current", current);
        body.put("daily", daily);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        // 1. WeatherService directly verifies separation of daily sum (106mm) vs current rate (12mm) and preserves 73%
        Map<String, Object> weatherResult = weatherService.getConditionsForLocationAndDate(17.747, 83.344, today);
        assertNotNull(weatherResult);
        assertEquals(73.0, (Double) weatherResult.get("rainProbability"), 0.01, "Rain probability must be 73%");
        assertEquals(106.0, (Double) weatherResult.get("precipitationMm"), 0.01, "Precipitation must be daily sum 106.0 mm");
        assertEquals(12.0, (Double) weatherResult.get("currentPrecipitationMm"), 0.01, "Current hourly precipitation rate must be 12.0 mm");
        assertEquals("Heavy Rain", weatherResult.get("weatherCondition"));
        assertTrue((Boolean) weatherResult.get("isLive"));
        assertTrue((Boolean) weatherResult.get("isAvailable"));

        // 2. TripService.checkNextActivity verifies consistency with Itinerary Day forecast
        com.travelmate.repository.TripRepository tripRepo = mock(com.travelmate.repository.TripRepository.class);
        com.travelmate.repository.UserRepository userRepo = mock(com.travelmate.repository.UserRepository.class);
        MLIntegrationService mlService = mock(MLIntegrationService.class);
        LocationService locService = mock(LocationService.class);
        PlaceDiscoveryService placeService = mock(PlaceDiscoveryService.class);
        com.travelmate.repository.ItineraryItemRepository itemRepo = mock(com.travelmate.repository.ItineraryItemRepository.class);
        com.travelmate.repository.TripMemberRepository memberRepo = mock(com.travelmate.repository.TripMemberRepository.class);
        RouteOptimizationService routeService = new RouteOptimizationService();
        com.travelmate.repository.AttractionRepository attrRepo = mock(com.travelmate.repository.AttractionRepository.class);

        TripService tripService = new TripService(
                tripRepo, userRepo, mlService, locService, placeService,
                weatherService, itemRepo, memberRepo, routeService, attrRepo
        );

        com.travelmate.entity.Trip trip = new com.travelmate.entity.Trip();
        trip.setId(200L);
        trip.setStartDate(today);

        com.travelmate.entity.ItineraryDay day1 = new com.travelmate.entity.ItineraryDay();
        day1.setId(10L);
        day1.setDate(today);
        day1.setDayNumber(1);

        com.travelmate.entity.ItineraryItem itemPrev = new com.travelmate.entity.ItineraryItem();
        itemPrev.setId(201L);
        itemPrev.setTitle("RK Beach");
        itemPrev.setStatus("COMPLETED");
        itemPrev.setItineraryDay(day1);

        com.travelmate.entity.ItineraryItem itemNext = new com.travelmate.entity.ItineraryItem();
        itemNext.setId(202L);
        itemNext.setTitle("Tenneti Park");
        itemNext.setStatus("SCHEDULED");
        itemNext.setIndoorOutdoor("Outdoor");
        itemNext.setLatitude(17.747);
        itemNext.setLongitude(83.344);
        itemNext.setItineraryDay(day1);

        day1.getItems().add(itemPrev);
        day1.getItems().add(itemNext);
        trip.getDays().add(day1);

        when(tripRepo.findById(200L)).thenReturn(Optional.of(trip));

        // Execute Live Weather Next Activity Check
        Map<String, Object> liveCheck = tripService.checkNextActivity("traveler@travelmate.com", 200L, 201L, null);

        assertNotNull(liveCheck);
        assertFalse((Boolean) liveCheck.get("isSuitable"), "Outdoor activity must be flagged as unsuitable during Heavy Rain");
        assertFalse((Boolean) liveCheck.get("isSimulated"), "Must be live weather check, not simulated");
        assertEquals(73L, ((Number) liveCheck.get("rainProbability")).longValue(), "Rain probability in popup must match 73%");
        assertEquals(106.0, (Double) liveCheck.get("precipitationMm"), 0.01, "Precipitation in popup must match daily sum 106.0 mm");
        assertEquals(12.0, (Double) liveCheck.get("currentPrecipitationMm"), 0.01, "Current rainfall rate must be 12.0 mm");

        String alertMsg = (String) liveCheck.get("alertMessage");
        assertNotNull(alertMsg);
        assertTrue(alertMsg.contains("73%"), "Alert message must include 73% probability: " + alertMsg);
        assertTrue(alertMsg.contains("106.0 mm"), "Alert message must include 106.0 mm rainfall: " + alertMsg);
        assertFalse(alertMsg.contains("85%"), "Alert message must not contain 85% in live mode: " + alertMsg);

        // Execute Simulated Weather Check
        Map<String, Object> simCheck = tripService.checkNextActivity("traveler@travelmate.com", 200L, 201L, "rain");
        assertNotNull(simCheck);
        assertTrue((Boolean) simCheck.get("isSimulated"), "Must clearly flag simulated test mode");
        assertNotNull(simCheck.get("simulatedWeather"), "Must isolate simulated conditions");
        assertNotNull(simCheck.get("liveWeather"), "Must preserve actual live readings separately even during simulation");
        Map<?, ?> simLive = (Map<?, ?>) simCheck.get("liveWeather");
        assertEquals("73%", simLive.get("rainProbability"), "Live weather rain probability must remain 73% inside liveWeather");
        assertEquals(106.0, simLive.get("precipitationMm"), "Live weather precipitation must remain 106.0 mm inside liveWeather");
        Map<?, ?> simValues = (Map<?, ?>) simCheck.get("simulatedWeather");
        assertEquals(85L, ((Number) simCheck.get("rainProbability")).longValue());
        assertEquals("85%", simValues.get("rainProbability"));
        assertEquals(12.0, simValues.get("precipitationMm"));
        assertEquals("73%", simLive.get("rainProbability"), "Simulated rain must not replace live readings");

        String simAlert = (String) simCheck.get("alertMessage");
        assertTrue(simAlert.startsWith("[SIMULATION TEST]"), "Simulated alert must explicitly identify itself as a test: " + simAlert);

        // 3. Test Switching back to Live Weather: verify simulation is cleared and actual Open-Meteo values are restored
        Map<String, Object> liveRestored = tripService.checkNextActivity("traveler@travelmate.com", 200L, 201L, "live");
        assertNotNull(liveRestored);
        assertFalse((Boolean) liveRestored.get("isSimulated"), "isSimulated must be false when switching back to live mode");
        assertNull(liveRestored.get("simulatedWeather"), "simulatedWeather must be null in live mode");
        assertEquals(73L, ((Number) liveRestored.get("rainProbability")).longValue(), "Must restore actual live 73% rain probability");
        assertEquals(106.0, (Double) liveRestored.get("precipitationMm"), 0.01, "Must restore actual live 106.0 mm precipitation");
        String restoredAlert = (String) liveRestored.get("alertMessage");
        assertFalse(restoredAlert.contains("SIMULATION"), "Must not contain SIMULATION in live mode");
        assertTrue(restoredAlert.contains("73%"), "Must display actual 73% in live mode");
    }

    @Test
    void cachesForecastByNormalizedCoordinatesAcrossDates() {
        Map<String, Object> body = createTwoDayForecast();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        LocalDate today = LocalDate.now();
        Map<String, Object> first = weatherService.getConditionsForLocationAndDate(15.4925, 73.7737, today);
        Map<String, Object> second = weatherService.getConditionsForLocationAndDate(15.4930, 73.7729, today.plusDays(1));

        assertEquals(15.0, first.get("rainProbability"));
        assertEquals(70.0, second.get("rainProbability"));
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    @Test
    void getWeatherForecastReturnsCachedForecastWithoutAnotherRequest() {
        Map<String, Object> body = createTwoDayForecast();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));

        Map<String, Object> first = weatherService.getWeatherForecast(15.4925, 73.7737);
        Map<String, Object> second = weatherService.getWeatherForecast(15.4930, 73.7729);

        assertTrue((Boolean) first.get("isAvailable"));
        assertFalse((Boolean) first.get("isStale"));
        assertSame(body.get("daily"), second.get("daily"));
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    @Test
    void getWeatherForecastClearlyMarksStaleCachedData() {
        Map<String, Object> body = createTwoDayForecast();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body));
        weatherService.getWeatherForecast(19.123, 80.234);

        Map<?, ?> cache = (Map<?, ?>) ReflectionTestUtils.getField(weatherService, "weatherCache");
        Object cachedEntry = cache.get("19.12_80.23");
        long staleTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(7);
        ReflectionTestUtils.setField(cachedEntry, "timestamp", staleTimestamp);
        ((java.util.concurrent.atomic.AtomicLong) ReflectionTestUtils.getField(weatherService, "rateLimitUntilMillis"))
                .set(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));

        Map<String, Object> stale = weatherService.getWeatherForecast(19.123, 80.234);

        assertTrue((Boolean) stale.get("isAvailable"));
        assertTrue((Boolean) stale.get("isStale"));
        assertEquals("Live weather temporarily unavailable — showing cached forecast.", stale.get("forecastMessage"));
        assertEquals(staleTimestamp, stale.get("forecastTimestamp"));
        assertSame(body.get("daily"), stale.get("daily"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void diagnosticsLogCacheSourcesAndOutboundRequestCount(CapturedOutput output) {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(createTwoDayForecast()));

        weatherService.getWeatherForecast(15.4925, 73.7737);
        weatherService.getWeatherForecast(15.4925, 73.7737);

        Map<?, ?> cache = (Map<?, ?>) ReflectionTestUtils.getField(weatherService, "weatherCache");
        Object cachedEntry = cache.get("15.49_73.77");
        ReflectionTestUtils.setField(cachedEntry, "timestamp", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(7));
        ((java.util.concurrent.atomic.AtomicLong) ReflectionTestUtils.getField(weatherService, "rateLimitUntilMillis"))
                .set(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        Map<String, Object> stale = weatherService.getWeatherForecast(15.4925, 73.7737);

        assertTrue((Boolean) stale.get("isStale"));
        assertTrue(output.getAll().contains("event=cache_miss"));
        assertTrue(output.getAll().contains("event=outbound_request"));
        assertTrue(output.getAll().contains("requestCount=1"));
        assertTrue(output.getAll().contains("status=200 source=upstream_api"));
        assertTrue(output.getAll().contains("source=fresh_cache"));
        assertTrue(output.getAll().contains("source=stale_cache"));
        assertTrue(output.getAll().contains("coordinates=15.49_73.77"));
        assertTrue(output.getAll().contains("event=request_suppressed reason=cooldown"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void rateLimitDiagnosticsIncludeSafeHeadersAndRedactedResponseBody(CapturedOutput output) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "120");
        headers.set("X-RateLimit-Remaining", "0");
        String responseBody = "{\"error\":\"quota exceeded\",\"api_key\":\"never-log-this-secret\"}";
        HttpClientErrorException rateLimit = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                responseBody.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(rateLimit);

        Map<String, Object> unavailable = weatherService.getWeatherForecast(15.4925, 73.7737);

        assertFalse((Boolean) unavailable.get("isAvailable"));
        assertTrue(output.getAll().contains("event=upstream_rate_limited"));
        assertTrue(output.getAll().contains("status=429"));
        assertTrue(output.getAll().contains("rateLimitResponseCount=1"));
        assertTrue(output.getAll().contains("retryAfter=120"));
        assertTrue(output.getAll().contains("X-RateLimit-Remaining=0"));
        assertTrue(output.getAll().contains("cooldownRemainingMs="));
        assertTrue(output.getAll().contains("quota exceeded"));
        assertFalse(output.getAll().contains("never-log-this-secret"));
        assertTrue(output.getAll().contains("[REDACTED]"));
    }

    @Test
    void rateLimitHonorsRetryAfterAndSuppressesRequestsDuringCooldown() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "120");
        HttpClientErrorException rateLimit = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(rateLimit);

        long beforeRequest = System.currentTimeMillis();
        Map<String, Object> first = weatherService.getCurrentConditions(19.123, 80.234);
        Map<String, Object> second = weatherService.getCurrentConditions(19.123, 80.234);
        long cooldownUntil = ((java.util.concurrent.atomic.AtomicLong) ReflectionTestUtils
                .getField(weatherService, "rateLimitUntilMillis")).get();

        assertFalse((Boolean) first.get("isAvailable"));
        assertFalse((Boolean) second.get("isAvailable"));
        assertTrue(cooldownUntil >= beforeRequest + TimeUnit.SECONDS.toMillis(110));
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    @Test
    void timeoutReturnsUnavailableAndUsesBackoffToSuppressImmediateRetry() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new ResourceAccessException("Weather request timed out", new SocketTimeoutException("read timed out")));

        Map<String, Object> first = weatherService.getCurrentConditions(19.123, 80.234);
        Map<String, Object> second = weatherService.getCurrentConditions(19.123, 80.234);

        assertFalse((Boolean) first.get("isAvailable"));
        assertEquals("Forecast Temporarily Unavailable", first.get("weatherCondition"));
        assertNull(first.get("temperatureC"));
        assertNull(first.get("rainProbability"));
        assertFalse((Boolean) second.get("isAvailable"));
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    @Test
    void simultaneousForecastCallsForSameNormalizedLocationShareOneRequest() throws Exception {
        Map<String, Object> body = createTwoDayForecast();
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch releaseRequest = new CountDownLatch(1);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenAnswer(invocation -> {
            requestStarted.countDown();
            if (!releaseRequest.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release mocked forecast request");
            }
            return ResponseEntity.ok(body);
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Map<String, Object>> first = executor.submit(
                    () -> weatherService.getConditionsForLocationAndDate(15.4925, 73.7737, LocalDate.now()));
            assertTrue(requestStarted.await(1, TimeUnit.SECONDS));
            Future<Map<String, Object>> second = executor.submit(
                    () -> weatherService.getConditionsForLocationAndDate(15.4930, 73.7729, LocalDate.now()));
            Thread.sleep(100);
            releaseRequest.countDown();

            assertEquals(15.0, first.get(2, TimeUnit.SECONDS).get("rainProbability"));
            assertEquals(15.0, second.get(2, TimeUnit.SECONDS).get("rainProbability"));
            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    isNull(),
                    ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
            );
        } finally {
            releaseRequest.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void rateLimitDoesNotRetryAndKeepsLastValidForecast() {
        LocalDate today = LocalDate.now();
        Map<String, Object> body = createTwoDayForecast();
        HttpClientErrorException rateLimit = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(body)).thenThrow(rateLimit);

        Map<String, Object> initial = weatherService.getConditionsForLocationAndDate(17.747, 83.344, today);
        Map<?, ?> cache = (Map<?, ?>) ReflectionTestUtils.getField(weatherService, "weatherCache");
        Object cachedEntry = cache.get("17.75_83.34");
        long staleTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(7);
        ReflectionTestUtils.setField(cachedEntry, "timestamp", staleTimestamp);
        Map<?, ?> dateCache = (Map<?, ?>) ReflectionTestUtils.getField(weatherService, "conditionsCache");
        Object cachedConditions = dateCache.get("17.75_83.34:" + today);
        ReflectionTestUtils.setField(cachedConditions, "timestamp", staleTimestamp);
        ((java.util.concurrent.atomic.AtomicLong) ReflectionTestUtils.getField(weatherService, "nextRequestAllowedMillis")).set(0L);

        Map<String, Object> limited = weatherService.getConditionsForLocationAndDate(17.747, 83.344, today);
        Map<String, Object> suppressedRetry = weatherService.getConditionsForLocationAndDate(17.747, 83.344, today);

        assertEquals(15.0, initial.get("rainProbability"));
        assertEquals(15.0, limited.get("rainProbability"));
        assertTrue((Boolean) limited.get("isStale"));
        assertTrue((Boolean) limited.get("isAvailable"));
        assertFalse((Boolean) limited.get("isLive"));
        assertEquals("Live weather temporarily unavailable — showing cached forecast.", limited.get("forecastMessage"));
        assertEquals(15.0, suppressedRetry.get("rainProbability"));
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    @Test
    void rateLimitWithoutCachedForecastReturnsUnavailableWithoutRetrying() {
        HttpClientErrorException rateLimit = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(rateLimit);

        Map<String, Object> first = weatherService.getCurrentConditions(18.123, 80.234);
        Map<String, Object> second = weatherService.getCurrentConditions(18.123, 80.234);

        assertEquals("Forecast Temporarily Unavailable", first.get("weatherCondition"));
        assertEquals("Live weather temporarily unavailable", first.get("forecastMessage"));
        assertNull(first.get("rainProbability"));
        assertNull(first.get("precipitationMm"));
        assertNull(first.get("temperatureC"));
        assertEquals("Forecast Temporarily Unavailable", second.get("weatherCondition"));
        assertEquals("Live weather temporarily unavailable", second.get("forecastMessage"));
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    @Test
    void throttlesRequestsForDifferentLocations() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(createTwoDayForecast()));

        Map<String, Object> first = weatherService.getConditionsForLocationAndDate(15.4925, 73.7737, LocalDate.now());
        Map<String, Object> throttled = weatherService.getConditionsForLocationAndDate(17.747, 83.344, LocalDate.now());

        assertTrue((Boolean) first.get("isAvailable"));
        assertFalse((Boolean) throttled.get("isAvailable"));
        assertEquals("Live weather temporarily unavailable", throttled.get("forecastMessage"));
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );
    }

    private Map<String, Object> createTwoDayForecast() {
        Map<String, Object> daily = new HashMap<>();
        daily.put("time", List.of(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString()));
        daily.put("temperature_2m_max", List.of(29.0, 28.0));
        daily.put("temperature_2m_min", List.of(21.0, 20.0));
        daily.put("precipitation_probability_max", List.of(15.0, 70.0));
        daily.put("precipitation_sum", List.of(0.0, 8.5));
        daily.put("weather_code", List.of(0, 65));
        Map<String, Object> body = new HashMap<>();
        body.put("daily", daily);
        return body;
    }

    @Test
    void testWeatherDescriptionMapping() {
        assertEquals("Clear Sky", WeatherService.getWeatherDescription(0));
        assertEquals("Partly Cloudy", WeatherService.getWeatherDescription(2));
        assertEquals("Overcast", WeatherService.getWeatherDescription(3));
        assertEquals("Light Drizzle", WeatherService.getWeatherDescription(51));
        assertEquals("Slight Rain", WeatherService.getWeatherDescription(61));
        assertEquals("Heavy Rain", WeatherService.getWeatherDescription(65));
        assertEquals("Passing Showers", WeatherService.getWeatherDescription(80));
        assertEquals("Rain Showers", WeatherService.getWeatherDescription(81));
        assertEquals("Violent Showers", WeatherService.getWeatherDescription(82));
        assertEquals("Thunderstorm", WeatherService.getWeatherDescription(95));
        assertEquals("Thunderstorm with Hail", WeatherService.getWeatherDescription(99));
    }
}
