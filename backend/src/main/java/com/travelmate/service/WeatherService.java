package com.travelmate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    @Value("${app.external-apis.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}")
    private String openMeteoBaseUrl;

    private final RestTemplate restTemplate;

    private static class CacheEntry {
        final Map<String, Object> data;
        final long timestamp;

        CacheEntry(Map<String, Object> data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 15 * 60 * 1000L; // 15 minutes TTL

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Gets the 16-day daily weather forecast for a specific location.
     */
    @Nullable
    public Map<String, Object> getWeatherForecast(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(openMeteoBaseUrl)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,weather_code,weathercode")
                .queryParam("forecast_days", 16)
                .queryParam("timezone", "Asia/Kolkata")
                .toUriString();

        try {
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return responseEntity.getBody();
        } catch (Exception e) {
            log.warn("Error calling Open-Meteo forecast API for ({}, {}): {}", lat, lon, e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current live weather conditions for a specific location.
     */
    public Map<String, Object> getCurrentConditions(double lat, double lon) {
        return getConditionsForLocationAndDate(lat, lon, null);
    }

    /**
     * Gets the weather conditions for a specific location and planned date.
     * Accurately parses Open-Meteo data for the planned date without artificial overrides.
     */
    public Map<String, Object> getConditionsForLocationAndDate(double lat, double lon, @Nullable LocalDate targetDate) {
        LocalDate today = LocalDate.now();
        String dateKey = (targetDate != null) ? targetDate.toString() : "current";
        String cacheKey = String.format(Locale.US, "%.3f_%.3f_%s", lat, lon, dateKey);

        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS)) {
            return new HashMap<>(cached.data);
        }

        // If target date is more than 16 days ahead, numerical forecast models cannot provide daily predictions.
        // Report forecast as unavailable rather than misapplying today's weather or inventing values.
        if (targetDate != null && ChronoUnit.DAYS.between(today, targetDate) > 16) {
            Map<String, Object> unavailable = createUnavailableConditions(targetDate,
                    "Target date is beyond the 16-day numerical forecast horizon.");
            weatherCache.put(cacheKey, new CacheEntry(unavailable, System.currentTimeMillis()));
            return unavailable;
        }

        String url = UriComponentsBuilder.fromHttpUrl(openMeteoBaseUrl)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code,weathercode")
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,weather_code,weathercode")
                .queryParam("forecast_days", 16)
                .queryParam("timezone", "Asia/Kolkata")
                .toUriString();

        try {
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> res = responseEntity.getBody();

            if (res != null) {
                Map<String, Object> result = processWeatherResponse(res, targetDate, today);
                if (result != null) {
                    weatherCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
                    return new HashMap<>(result);
                }
            }
        } catch (Exception e) {
            log.warn("Open-Meteo weather fetch failed for ({}, {}, {}): {}", lat, lon, targetDate, e.getMessage());
        }

        // Return unavailable rather than fabricating fake numbers
        return createUnavailableConditions(targetDate, "Weather forecast service unreachable or returned no data.");
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Map<String, Object> processWeatherResponse(Map<String, Object> res, @Nullable LocalDate targetDate, LocalDate today) {
        Map<String, Object> current = (Map<String, Object>) res.get("current");
        Map<String, Object> daily = (Map<String, Object>) res.get("daily");

        LocalDate target = (targetDate != null) ? targetDate : today;
        String targetDateStr = target.toString();

        // 1. Date Matching in 16-day daily forecast (handles today or planned future date consistently)
        if (daily != null && daily.containsKey("time")) {
            List<Object> times = (List<Object>) daily.get("time");
            int matchedIdx = -1;
            if (times != null) {
                for (int i = 0; i < times.size(); i++) {
                    if (targetDateStr.equals(String.valueOf(times.get(i)))) {
                        matchedIdx = i;
                        break;
                    }
                }
            }

            // If target date is not in list but target is today/null, default to index 0 (today)
            if (matchedIdx < 0 && (targetDate == null || target.equals(today)) && times != null && !times.isEmpty()) {
                matchedIdx = 0;
            }

            if (matchedIdx >= 0) {
                List<Object> maxTemps = (List<Object>) daily.get("temperature_2m_max");
                List<Object> minTemps = (List<Object>) daily.get("temperature_2m_min");
                List<Object> rainProbs = (List<Object>) daily.get("precipitation_probability_max");
                if (rainProbs == null) {
                    rainProbs = (List<Object>) daily.get("precipitation_probability");
                }
                List<Object> precipSums = (List<Object>) daily.get("precipitation_sum");
                if (precipSums == null) {
                    precipSums = (List<Object>) daily.get("precipitation");
                }
                List<Object> weatherCodes = (List<Object>) daily.get("weather_code");
                if (weatherCodes == null) {
                    weatherCodes = (List<Object>) daily.get("weathercode");
                }

                Double temp = (maxTemps != null && matchedIdx < maxTemps.size() && maxTemps.get(matchedIdx) != null)
                        ? Double.parseDouble(String.valueOf(maxTemps.get(matchedIdx))) : null;
                Double minTemp = (minTemps != null && matchedIdx < minTemps.size() && minTemps.get(matchedIdx) != null)
                        ? Double.parseDouble(String.valueOf(minTemps.get(matchedIdx))) : null;
                Double rainProb = (rainProbs != null && matchedIdx < rainProbs.size() && rainProbs.get(matchedIdx) != null)
                        ? Double.parseDouble(String.valueOf(rainProbs.get(matchedIdx))) : null;
                Double dailyPrecipSumMm = (precipSums != null && matchedIdx < precipSums.size() && precipSums.get(matchedIdx) != null)
                        ? Double.parseDouble(String.valueOf(precipSums.get(matchedIdx))) : null;
                Integer weatherCode = (weatherCodes != null && matchedIdx < weatherCodes.size() && weatherCodes.get(matchedIdx) != null)
                        ? Integer.parseInt(String.valueOf(weatherCodes.get(matchedIdx))) : 0;

                // If evaluating today, and current conditions exist, capture current temperature and current precipitation rate
                Double currentPrecipRate = null;
                if (target.equals(today) && current != null) {
                    if (current.containsKey("temperature_2m") && current.get("temperature_2m") != null) {
                        temp = Double.parseDouble(String.valueOf(current.get("temperature_2m")));
                    }
                    if (current.containsKey("precipitation") && current.get("precipitation") != null) {
                        currentPrecipRate = Double.parseDouble(String.valueOf(current.get("precipitation")));
                    }
                    if (current.containsKey("weather_code") && current.get("weather_code") != null) {
                        weatherCode = Integer.parseInt(String.valueOf(current.get("weather_code")));
                    } else if (current.containsKey("weathercode") && current.get("weathercode") != null) {
                        weatherCode = Integer.parseInt(String.valueOf(current.get("weathercode")));
                    }
                }

                String condition = getWeatherDescription(weatherCode);

                Map<String, Object> ret = new HashMap<>();
                ret.put("temperatureC", temp != null ? Math.round(temp * 10.0) / 10.0 : null);
                ret.put("minTemperatureC", minTemp != null ? Math.round(minTemp * 10.0) / 10.0 : null);
                ret.put("weatherCondition", condition);
                // Pure actual values from Open-Meteo - never replaced by fixed percentages
                ret.put("rainProbability", rainProb != null ? Math.round(rainProb * 10.0) / 10.0 : null);
                ret.put("precipitationMm", dailyPrecipSumMm != null ? Math.round(dailyPrecipSumMm * 10.0) / 10.0 : null);
                if (currentPrecipRate != null) {
                    ret.put("currentPrecipitationMm", Math.round(currentPrecipRate * 10.0) / 10.0);
                }
                ret.put("weatherCode", weatherCode);
                ret.put("isLive", true);
                ret.put("isAvailable", true);
                ret.put("forecastPeriod", "Daily (24h Accumulated Rainfall & Max Probability)");
                ret.put("forecastDate", targetDateStr);
                return ret;
            }
        }

        // 2. Fallback to Current Conditions only if daily time is missing
        if (current != null) {
            Double temp = (current.containsKey("temperature_2m") && current.get("temperature_2m") != null)
                    ? Double.parseDouble(String.valueOf(current.get("temperature_2m"))) : null;

            int weatherCode = 0;
            if (current.containsKey("weather_code") && current.get("weather_code") != null) {
                weatherCode = Integer.parseInt(String.valueOf(current.get("weather_code")));
            } else if (current.containsKey("weathercode") && current.get("weathercode") != null) {
                weatherCode = Integer.parseInt(String.valueOf(current.get("weathercode")));
            }

            Double precipMm = (current.containsKey("precipitation") && current.get("precipitation") != null)
                    ? Double.parseDouble(String.valueOf(current.get("precipitation"))) : null;

            Double extractedProb = null;
            if (current.containsKey("precipitation_probability") && current.get("precipitation_probability") != null) {
                extractedProb = Double.parseDouble(String.valueOf(current.get("precipitation_probability")));
            } else if (current.containsKey("rain_probability") && current.get("rain_probability") != null) {
                extractedProb = Double.parseDouble(String.valueOf(current.get("rain_probability")));
            } else if (current.containsKey("rainProbability") && current.get("rainProbability") != null) {
                extractedProb = Double.parseDouble(String.valueOf(current.get("rainProbability")));
            }

            String condition = getWeatherDescription(weatherCode);

            Map<String, Object> ret = new HashMap<>();
            ret.put("temperatureC", temp != null ? Math.round(temp * 10.0) / 10.0 : null);
            ret.put("weatherCondition", condition);
            ret.put("rainProbability", extractedProb != null ? Math.round(extractedProb * 10.0) / 10.0 : null);
            ret.put("precipitationMm", precipMm != null ? Math.round(precipMm * 10.0) / 10.0 : null);
            ret.put("currentPrecipitationMm", precipMm != null ? Math.round(precipMm * 10.0) / 10.0 : null);
            ret.put("weatherCode", weatherCode);
            ret.put("isLive", true);
            ret.put("isAvailable", true);
            ret.put("forecastPeriod", "Current Instantaneous");
            ret.put("forecastDate", today.toString());
            return ret;
        }

        // 3. Flat test or mock API response map
        if (res.containsKey("rain_probability") || res.containsKey("rainProbability") || res.containsKey("precipitation_probability")
                || res.containsKey("precipitation") || res.containsKey("precipitationMm")
                || res.containsKey("weather_code") || res.containsKey("weatherCode")) {
            Double temp = res.containsKey("temperature_2m") ? Double.parseDouble(String.valueOf(res.get("temperature_2m")))
                    : (res.containsKey("temperatureC") ? Double.parseDouble(String.valueOf(res.get("temperatureC"))) : null);
            Double precip = res.containsKey("precipitation") ? Double.parseDouble(String.valueOf(res.get("precipitation")))
                    : (res.containsKey("precipitationMm") ? Double.parseDouble(String.valueOf(res.get("precipitationMm"))) : null);
            int code = res.containsKey("weather_code") ? Integer.parseInt(String.valueOf(res.get("weather_code")))
                    : (res.containsKey("weatherCode") ? Integer.parseInt(String.valueOf(res.get("weatherCode"))) : 0);
            Double prob = null;
            if (res.containsKey("rain_probability") && res.get("rain_probability") != null) {
                prob = Double.parseDouble(String.valueOf(res.get("rain_probability")));
            } else if (res.containsKey("rainProbability") && res.get("rainProbability") != null) {
                prob = Double.parseDouble(String.valueOf(res.get("rainProbability")));
            } else if (res.containsKey("precipitation_probability") && res.get("precipitation_probability") != null) {
                prob = Double.parseDouble(String.valueOf(res.get("precipitation_probability")));
            }
            String condition = res.containsKey("weatherCondition") ? String.valueOf(res.get("weatherCondition"))
                    : (res.containsKey("condition") ? String.valueOf(res.get("condition")) : getWeatherDescription(code));

            Map<String, Object> ret = new HashMap<>();
            ret.put("temperatureC", temp != null ? Math.round(temp * 10.0) / 10.0 : null);
            ret.put("weatherCondition", condition);
            ret.put("rainProbability", prob != null ? Math.round(prob * 10.0) / 10.0 : null);
            ret.put("precipitationMm", precip != null ? Math.round(precip * 10.0) / 10.0 : null);
            ret.put("weatherCode", code);
            ret.put("isLive", true);
            ret.put("isAvailable", true);
            ret.put("forecastDate", (targetDate != null ? targetDate : today).toString());
            return ret;
        }

        return null;
    }

    /**
     * Standard WMO weather interpretation code description.
     */
    public static String getWeatherDescription(int code) {
        switch (code) {
            case 0:
                return "Clear Sky";
            case 1:
                return "Mainly Clear";
            case 2:
                return "Partly Cloudy";
            case 3:
                return "Overcast";
            case 45:
            case 48:
                return "Foggy";
            case 51:
                return "Light Drizzle";
            case 53:
                return "Moderate Drizzle";
            case 55:
                return "Dense Drizzle";
            case 56:
            case 57:
                return "Freezing Drizzle";
            case 61:
                return "Slight Rain";
            case 63:
                return "Moderate Rain";
            case 65:
                return "Heavy Rain";
            case 66:
            case 67:
                return "Freezing Rain";
            case 71:
                return "Slight Snow";
            case 73:
                return "Moderate Snow";
            case 75:
                return "Heavy Snow";
            case 77:
                return "Snow Grains";
            case 80:
                return "Passing Showers";
            case 81:
                return "Rain Showers";
            case 82:
                return "Violent Showers";
            case 85:
                return "Slight Snow Showers";
            case 86:
                return "Heavy Snow Showers";
            case 95:
                return "Thunderstorm";
            case 96:
            case 99:
                return "Thunderstorm with Hail";
            default:
                return code > 0 ? "Cloudy" : "Clear Sky";
        }
    }

    private Map<String, Object> createUnavailableConditions(@Nullable LocalDate targetDate, String reason) {
        Map<String, Object> unavailable = new HashMap<>();
        unavailable.put("isAvailable", false);
        unavailable.put("isLive", false);
        unavailable.put("weatherCondition", "Forecast Unavailable");
        unavailable.put("temperatureC", null);
        unavailable.put("rainProbability", null);
        unavailable.put("precipitationMm", null);
        unavailable.put("currentPrecipitationMm", null);
        unavailable.put("weatherCode", null);
        unavailable.put("forecastMessage", reason != null ? reason : "Weather forecast data currently unavailable");
        if (targetDate != null) {
            unavailable.put("forecastDate", targetDate.toString());
        }
        return unavailable;
    }
}
