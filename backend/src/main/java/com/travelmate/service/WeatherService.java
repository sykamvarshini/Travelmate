package com.travelmate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class WeatherService {

    @Value("${app.external-apis.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}")
    private String openMeteoBaseUrl;

    private final RestTemplate restTemplate;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Gets the weather forecast for a specific location.
     */
    public Map<String, Object> getWeatherForecast(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(openMeteoBaseUrl)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode")
                .queryParam("timezone", "Asia/Kolkata")
                .toUriString();

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            System.err.println("Error calling Open-Meteo API: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current live weather conditions for a specific location.
     */
    public Map<String, Object> getCurrentConditions(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(openMeteoBaseUrl)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code")
                .queryParam("daily", "precipitation_probability_max,temperature_2m_max")
                .queryParam("timezone", "Asia/Kolkata")
                .toUriString();

        try {
            Map res = restTemplate.getForObject(url, Map.class);
            if (res != null && res.containsKey("current")) {
                Map current = (Map) res.get("current");
                Map daily = (Map) res.get("daily");
                
                double temp = current.containsKey("temperature_2m") ? Double.parseDouble(current.get("temperature_2m").toString()) : 28.0;
                int weatherCode = current.containsKey("weather_code") ? Integer.parseInt(current.get("weather_code").toString()) : 0;
                
                double rainProb = 0.0;
                if (daily != null && daily.containsKey("precipitation_probability_max")) {
                    java.util.List probs = (java.util.List) daily.get("precipitation_probability_max");
                    if (probs != null && !probs.isEmpty() && probs.get(0) != null) {
                        rainProb = Double.parseDouble(probs.get(0).toString());
                    }
                }
                
                String condition = "Clear / Sunny";
                if (weatherCode >= 51 && weatherCode <= 67) {
                    condition = "Rainy / Showers";
                    if (rainProb < 40.0) rainProb = 65.0;
                } else if (weatherCode >= 80 && weatherCode <= 99) {
                    condition = "Heavy Rain / Thunderstorm";
                    if (rainProb < 70.0) rainProb = 85.0;
                } else if (weatherCode >= 1 && weatherCode <= 3) {
                    condition = "Partly Cloudy";
                }

                Map<String, Object> ret = new java.util.HashMap<>();
                ret.put("temperatureC", temp);
                ret.put("weatherCondition", condition);
                ret.put("rainProbability", rainProb);
                ret.put("weatherCode", weatherCode);
                return ret;
            }
        } catch (Exception e) {
            System.err.println("Open-Meteo current condition fetch: " + e.getMessage());
        }

        // Safe fallback
        Map<String, Object> fallback = new java.util.HashMap<>();
        fallback.put("temperatureC", 29.0);
        fallback.put("weatherCondition", "Clear Sky");
        fallback.put("rainProbability", 10.0);
        fallback.put("weatherCode", 0);
        return fallback;
    }
}
