package com.travelmate.service;

import com.travelmate.entity.Attraction;
import com.travelmate.entity.City;
import com.travelmate.entity.State;
import com.travelmate.repository.AttractionCategoryRepository;
import com.travelmate.repository.AttractionRepository;
import com.travelmate.repository.CityRepository;
import com.travelmate.repository.StateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlaceDiscoveryService {

    @Value("${app.external-apis.google-places.api-key:}")
    private String googlePlacesApiKey;

    @Value("${app.external-apis.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    private final RestTemplate restTemplate;
    private final AttractionRepository attractionRepository;
    private final CityRepository cityRepository;
    private final StateRepository stateRepository;
    private final AttractionCategoryRepository categoryRepository;

    // Standard User-Agent for OpenStreetMap and Wikimedia APIs
    private static final String APP_USER_AGENT = "TravelMateAI/1.0 (https://travelmate.ai; contact@travelmate.ai)";
    private static final long AUTOCOMPLETE_CACHE_TTL_MS = 15 * 60 * 1000L;
    private static final long NOMINATIM_MIN_INTERVAL_MS = 1000L;

    private static class AutocompleteCacheEntry {
        final List<Map<String, Object>> results;
        final long timestamp;

        AutocompleteCacheEntry(List<Map<String, Object>> results) {
            this.results = results;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // In-memory cache for fast responsive searches and low network overhead
    private final Map<String, Map<String, Object>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, AutocompleteCacheEntry> autocompleteCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> nearbyCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> categoryCache = new ConcurrentHashMap<>();
    private final Object nominatimThrottleLock = new Object();
    private long nextNominatimRequestAtMillis;

    // Well-known foreign destinations to ensure strict India-only scope
    private static final Set<String> FOREIGN_DESTINATIONS = Set.of(
            "paris", "london", "dubai", "new york", "singapore", "bangkok", "tokyo", "rome", 
            "sydney", "toronto", "barcelona", "amsterdam", "los angeles", "berlin", "istanbul", 
            "kuala lumpur", "doha", "abu dhabi", "bali", "hong kong", "san francisco", "chicago",
            "maldives", "switzerland", "zurich", "las vegas", "miami", "venice", "florence", 
            "madrid", "seoul", "beijing", "shanghai", "cairo", "cape town", "colombo", "kathmandu"
    );

    public PlaceDiscoveryService(
            RestTemplate restTemplate,
            AttractionRepository attractionRepository,
            CityRepository cityRepository,
            StateRepository stateRepository,
            AttractionCategoryRepository categoryRepository) {
        this.restTemplate = restTemplate;
        this.attractionRepository = attractionRepository;
        this.cityRepository = cityRepository;
        this.stateRepository = stateRepository;
        this.categoryRepository = categoryRepository;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", APP_USER_AGENT);
        headers.set("Accept", "application/json");
        return headers;
    }

    /**
     * Checks if a search query refers to an international location outside India.
     */
    public boolean isForeignLocation(String query) {
        if (query == null || query.trim().isEmpty()) return false;
        String q = query.trim().toLowerCase();
        
        for (String foreign : FOREIGN_DESTINATIONS) {
            if (q.equals(foreign) || q.startsWith(foreign + ",") || q.startsWith(foreign + " ") || q.endsWith(" " + foreign)) {
                return true;
            }
        }

        // Worldwide Geocoding check if prominent international city
        try {
            URI uri = URI.create(nominatimBaseUrl + "/search?q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8) + "&format=json&limit=1");
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<List<Map<String, Object>>> res = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> body = res.getBody();
            if (body != null && !body.isEmpty()) {
                Map<String, Object> first = body.get(0);
                Object latObj = first.get("lat");
                Object lonObj = first.get("lon");
                if (latObj != null && lonObj != null) {
                    double lat = Double.parseDouble(latObj.toString());
                    double lon = Double.parseDouble(lonObj.toString());
                    // India bounding box roughly: Lat 6.0 to 38.0, Lon 68.0 to 98.0
                    boolean insideIndia = (lat >= 6.0 && lat <= 38.0 && lon >= 68.0 && lon <= 98.0);
                    if (!insideIndia) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Autocomplete location suggestions across India.
     */
    public List<Map<String, Object>> autocomplete(String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }
        String cleanQuery = query.trim().toLowerCase();

        AutocompleteCacheEntry cached = autocompleteCache.get(cleanQuery);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < AUTOCOMPLETE_CACHE_TTL_MS) {
            return new ArrayList<>(cached.results);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        // 1. Search database cities first
        List<City> matchedCities = cityRepository.searchCities(cleanQuery);
        for (City city : matchedCities) {
            String nameLower = city.getName().toLowerCase();
            if (!seenNames.contains(nameLower)) {
                seenNames.add(nameLower);
                Map<String, Object> item = new HashMap<>();
                item.put("placeId", "city_" + city.getId());
                item.put("name", city.getName());
                String stName = city.getState() != null ? city.getState().getName() : "India";
                item.put("displayName", city.getName() + ", " + stName + ", India");
                item.put("city", city.getName());
                item.put("state", stName);
                item.put("category", "City / Tourist Hub");
                item.put("lat", city.getLatitude());
                item.put("lon", city.getLongitude());
                item.put("latitude", city.getLatitude());
                item.put("longitude", city.getLongitude());
                item.put("secondary", stName + ", India");
                results.add(item);
            }
        }

        // 2. Search database attractions
        List<Attraction> matchedAttractions = attractionRepository.searchAttractions(cleanQuery);
        for (Attraction attr : matchedAttractions) {
            String nameLower = attr.getName().toLowerCase();
            if (!seenNames.contains(nameLower)) {
                seenNames.add(nameLower);
                Map<String, Object> item = new HashMap<>();
                item.put("placeId", "attr_" + attr.getId());
                item.put("name", attr.getName());
                String cityName = attr.getCity() != null ? attr.getCity().getName() : "India";
                String stName = (attr.getCity() != null && attr.getCity().getState() != null) ? attr.getCity().getState().getName() : "India";
                item.put("displayName", attr.getName() + ", " + cityName + ", " + stName + ", India");
                item.put("city", cityName);
                item.put("state", stName);
                item.put("category", attr.getCategory() != null ? attr.getCategory().getName() : "Sightseeing");
                item.put("lat", attr.getLatitude());
                item.put("lon", attr.getLongitude());
                item.put("latitude", attr.getLatitude());
                item.put("longitude", attr.getLongitude());
                item.put("secondary", cityName + ", " + stName + ", India");
                results.add(item);
                if (results.size() >= 10) break;
            }
        }

        // 2b. Multi-token search for compound queries like "Gayatri University"
        if (results.size() < 6 && cleanQuery.contains(" ")) {
            String[] tokens = cleanQuery.split("\\s+");
            for (String tok : tokens) {
                if (tok.length() >= 3) {
                    List<Attraction> tokenMatches = attractionRepository.searchAttractions(tok);
                    for (Attraction attr : tokenMatches) {
                        String nameLower = attr.getName().toLowerCase();
                        if (!seenNames.contains(nameLower)) {
                            seenNames.add(nameLower);
                            Map<String, Object> item = new HashMap<>();
                            item.put("placeId", "attr_" + attr.getId());
                            item.put("name", attr.getName());
                            String cityName = attr.getCity() != null ? attr.getCity().getName() : "India";
                            String stName = (attr.getCity() != null && attr.getCity().getState() != null) ? attr.getCity().getState().getName() : "India";
                            item.put("displayName", attr.getName() + ", " + cityName + ", " + stName + ", India");
                            item.put("city", cityName);
                            item.put("state", stName);
                            item.put("category", attr.getCategory() != null ? attr.getCategory().getName() : "Sightseeing");
                            item.put("lat", attr.getLatitude());
                            item.put("lon", attr.getLongitude());
                            item.put("latitude", attr.getLatitude());
                            item.put("longitude", attr.getLongitude());
                            item.put("secondary", cityName + ", " + stName + ", India");
                            results.add(item);
                            if (results.size() >= 10) break;
                        }
                    }
                }
            }
        }

        // 3. Google Places Autocomplete if configured
        if (results.size() < 6 && googlePlacesApiKey != null && !googlePlacesApiKey.trim().isEmpty()) {
            try {
                URI uri = URI.create("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" 
                        + URLEncoder.encode(query, StandardCharsets.UTF_8) 
                        + "&components=country:in&key=" + googlePlacesApiKey);
                ResponseEntity<Map<String, Object>> gResp = restTemplate.exchange(
                        uri, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
                Map<String, Object> response = gResp.getBody();
                if (response != null && response.containsKey("predictions")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> predictions = (List<Map<String, Object>>) response.get("predictions");
                    if (predictions != null) {
                        for (Map<String, Object> p : predictions) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("placeId", p.get("place_id"));
                            item.put("displayName", p.get("description"));
                            @SuppressWarnings("unchecked")
                            Map<String, Object> formatting = (Map<String, Object>) p.get("structured_formatting");
                            if (formatting != null) {
                                item.put("name", formatting.get("main_text"));
                                item.put("secondary", formatting.get("secondary_text"));
                            } else {
                                item.put("name", p.get("description"));
                                item.put("secondary", "India");
                            }
                            String nLower = String.valueOf(item.get("name")).toLowerCase();
                            if (!seenNames.contains(nLower)) {
                                seenNames.add(nLower);
                                results.add(item);
                            }
                            if (results.size() >= 8) break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Google Places Autocomplete error: " + e.getMessage());
            }
        }

        // 4. Open Location Engine (Nominatim) fallback if results are scarce
        if (results.size() < 5 && allowNominatimAutocompleteRequest()) {
            try {
                URI nomUri = URI.create(nominatimBaseUrl + "/search?q=" 
                        + URLEncoder.encode(query, StandardCharsets.UTF_8) 
                        + "&countrycodes=in&format=json&addressdetails=1&limit=6");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<List<Map<String, Object>>> res = restTemplate.exchange(
                        nomUri, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> list = res.getBody();
                if (list != null) {
                    for (Map<String, Object> item : list) {
                        String name = (String) item.get("name");
                        String displayName = (String) item.get("display_name");
                        if (name == null || name.trim().isEmpty()) {
                            name = displayName != null ? displayName.split(",")[0] : query;
                        }
                        String nLower = name.toLowerCase();
                        if (seenNames.contains(nLower) || isNonTouristOrNoise(nLower, (String) item.get("type"), (String) item.get("class"))) continue;
                        seenNames.add(nLower);

                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("placeId", String.valueOf(item.get("osm_id")));
                        suggestion.put("name", name);
                        suggestion.put("displayName", cleanDisplayName(displayName));
                        double lat = Double.parseDouble(String.valueOf(item.get("lat")));
                        double lon = Double.parseDouble(String.valueOf(item.get("lon")));
                        suggestion.put("lat", lat);
                        suggestion.put("lon", lon);
                        suggestion.put("latitude", lat);
                        suggestion.put("longitude", lon);
                        suggestion.put("type", item.get("type"));

                        @SuppressWarnings("unchecked")
                        Map<String, Object> addr = (Map<String, Object>) item.get("address");
                        if (addr != null) {
                            String city = extractCity(addr);
                            String state = (String) addr.get("state");
                            suggestion.put("city", city != null ? city : "India");
                            suggestion.put("state", state != null ? state : "India");
                            suggestion.put("secondary", (city != null ? city + ", " : "") + (state != null ? state + ", " : "") + "India");
                        } else {
                            suggestion.put("secondary", "India");
                        }

                        results.add(suggestion);
                        if (results.size() >= 8) break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Nominatim autocomplete error: " + e.getMessage());
            }
        }

        List<Map<String, Object>> cachedResults = new ArrayList<>(results);
        autocompleteCache.put(cleanQuery, new AutocompleteCacheEntry(cachedResults));
        return new ArrayList<>(cachedResults);
    }

    private boolean allowNominatimAutocompleteRequest() {
        synchronized (nominatimThrottleLock) {
            long now = System.currentTimeMillis();
            if (now < nextNominatimRequestAtMillis) {
                return false;
            }
            nextNominatimRequestAtMillis = now + NOMINATIM_MIN_INTERVAL_MS;
            return true;
        }
    }

    /**
     * Search and retrieve full verified details for any Indian location.
     */
    public Map<String, Object> searchPlace(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        String cleanQuery = query.trim();

        // 1. Check India-Only Scope
        if (isForeignLocation(cleanQuery)) {
            Map<String, Object> foreignRes = new HashMap<>();
            foreignRes.put("isIndia", false);
            foreignRes.put("message", "TravelMate AI currently supports travel within India.");
            foreignRes.put("query", cleanQuery);
            return foreignRes;
        }

        String cacheKey = cleanQuery.toLowerCase();
        if (searchCache.containsKey(cacheKey)) {
            return searchCache.get(cacheKey);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isIndia", true);
        result.put("query", cleanQuery);

        // 2. Check Database Cities
        Optional<City> cityOpt = cityRepository.findByNameIgnoreCase(cleanQuery);
        if (cityOpt.isEmpty()) {
            List<City> cities = cityRepository.searchCities(cleanQuery);
            if (!cities.isEmpty()) {
                cityOpt = Optional.of(cities.get(0));
            }
        }

        if (cityOpt.isPresent()) {
            City c = cityOpt.get();
            result.put("name", c.getName());
            result.put("city", c.getName());
            String st = c.getState() != null ? c.getState().getName() : "India";
            result.put("state", st);
            result.put("lat", c.getLatitude());
            result.put("lon", c.getLongitude());
            result.put("latitude", c.getLatitude());
            result.put("longitude", c.getLongitude());
            result.put("description", c.getDescription());
            result.put("address", c.getName() + ", " + st + ", India");
            result.put("category", "City / Tourist Hub");
            result.put("verified", true);
            result.put("rating", 4.8);
            result.put("reviewsCount", 15000);
            result.put("openingHours", "Always Open");

            // Attach genuine attractions from this city
            List<Map<String, Object>> nearby = discoverNearby(c.getLatitude(), c.getLongitude(), c.getName(), null, 8);
            result.put("nearbyAttractions", nearby);
            searchCache.put(cacheKey, result);
            return result;
        }

        // 3. Check Database Attractions directly
        Optional<Attraction> attrOpt = attractionRepository.findByNameIgnoreCase(cleanQuery);
        if (attrOpt.isEmpty()) {
            List<Attraction> attrs = attractionRepository.searchAttractions(cleanQuery);
            if (!attrs.isEmpty()) {
                attrOpt = Optional.of(attrs.get(0));
            }
        }
        if (attrOpt.isEmpty() && cleanQuery.contains(",")) {
            String firstPart = cleanQuery.split(",")[0].trim();
            attrOpt = attractionRepository.findByNameIgnoreCase(firstPart);
            if (attrOpt.isEmpty()) {
                List<Attraction> attrs = attractionRepository.searchAttractions(firstPart);
                if (!attrs.isEmpty()) {
                    attrOpt = Optional.of(attrs.get(0));
                }
            }
        }
        if (attrOpt.isEmpty() && cleanQuery.contains(" ")) {
            String[] tokens = cleanQuery.split("\\s+");
            for (String tok : tokens) {
                if (tok.length() >= 3 && !tok.equalsIgnoreCase("city") && !tok.equalsIgnoreCase("hub")) {
                    List<Attraction> attrs = attractionRepository.searchAttractions(tok);
                    if (!attrs.isEmpty()) {
                        attrOpt = Optional.of(attrs.get(0));
                        break;
                    }
                }
            }
        }

        if (attrOpt.isPresent()) {
            Attraction a = attrOpt.get();
            result.put("name", a.getName());
            String cName = a.getCity() != null ? a.getCity().getName() : "India";
            String stName = (a.getCity() != null && a.getCity().getState() != null) ? a.getCity().getState().getName() : "India";
            result.put("city", cName);
            result.put("state", stName);
            result.put("lat", a.getLatitude());
            result.put("lon", a.getLongitude());
            result.put("latitude", a.getLatitude());
            result.put("longitude", a.getLongitude());
            result.put("description", a.getDescription());
            result.put("address", a.getName() + ", " + cName + ", " + stName + ", India");
            result.put("category", formatCategory(a.getCategory() != null ? a.getCategory().getName() : a.getName()));
            result.put("rating", a.getRating() != null ? a.getRating() : 4.7);
            result.put("reviewsCount", a.getReviewsCount() != null ? a.getReviewsCount() : 5000);
            result.put("openingHours", a.getOpeningTime() + " - " + a.getClosingTime());
            result.put("verified", true);

            List<Map<String, Object>> nearby = discoverNearby(a.getLatitude(), a.getLongitude(), cName, null, 8);
            result.put("nearbyAttractions", nearby);
            searchCache.put(cacheKey, result);
            return result;
        }

        // 4. Check State
        Optional<State> stateOpt = stateRepository.findByNameIgnoreCase(cleanQuery);
        if (stateOpt.isPresent()) {
            State s = stateOpt.get();
            List<City> stateCities = cityRepository.findByStateNameIgnoreCase(s.getName());
            City firstCity = stateCities.isEmpty() ? null : stateCities.get(0);
            result.put("name", s.getName());
            result.put("state", s.getName());
            result.put("city", firstCity != null ? firstCity.getName() : s.getName());
            double stLat = firstCity != null ? firstCity.getLatitude() : 20.5937;
            double stLon = firstCity != null ? firstCity.getLongitude() : 78.9629;
            result.put("lat", stLat);
            result.put("lon", stLon);
            result.put("latitude", stLat);
            result.put("longitude", stLon);
            result.put("address", s.getName() + ", India");
            result.put("category", "State of India");
            result.put("verified", true);

            List<Map<String, Object>> nearby = discoverNearby(stLat, stLon, s.getName(), null, 8);
            result.put("nearbyAttractions", nearby);
            searchCache.put(cacheKey, result);
            return result;
        }

        // 5. Google Places Details if configured
        if ((!result.containsKey("lat") || !result.containsKey("lon")) && googlePlacesApiKey != null && !googlePlacesApiKey.trim().isEmpty()) {
            try {
                URI searchUri = URI.create("https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                        + URLEncoder.encode(cleanQuery + ", India", StandardCharsets.UTF_8)
                        + "&key=" + googlePlacesApiKey);
                ResponseEntity<Map<String, Object>> gResp = restTemplate.exchange(
                        searchUri, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
                Map<String, Object> gRes = gResp.getBody();
                if (gRes != null && gRes.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> gList = (List<Map<String, Object>>) gRes.get("results");
                    if (gList != null && !gList.isEmpty()) {
                        Map<String, Object> primary = gList.get(0);
                        result.put("name", primary.get("name"));
                        result.put("address", primary.get("formatted_address"));
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> geometry = (Map<String, Object>) primary.get("geometry");
                        if (geometry != null && geometry.containsKey("location")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> loc = (Map<String, Object>) geometry.get("location");
                            if (loc != null) {
                                double lat = Double.parseDouble(String.valueOf(loc.get("lat")));
                                double lon = Double.parseDouble(String.valueOf(loc.get("lng")));
                                result.put("lat", lat);
                                result.put("lon", lon);
                                result.put("latitude", lat);
                                result.put("longitude", lon);
                            }
                        }

                        if (primary.containsKey("rating")) {
                            result.put("rating", primary.get("rating"));
                        } else {
                            result.put("rating", "Information unavailable");
                        }

                        if (primary.containsKey("user_ratings_total")) {
                            result.put("reviewsCount", primary.get("user_ratings_total"));
                        } else {
                            result.put("reviewsCount", "Information unavailable");
                        }

                        @SuppressWarnings("unchecked")
                        List<Object> types = (List<Object>) primary.get("types");
                        result.put("category", formatCategory(types != null && !types.isEmpty() ? types.get(0).toString() : "attraction"));
                        result.put("openingHours", "Information unavailable");

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> photos = (List<Map<String, Object>>) primary.get("photos");
                        if (photos != null && !photos.isEmpty()) {
                            Map<String, Object> p = photos.get(0);
                            String pref = (String) p.get("photo_reference");
                            if (pref != null) {
                                result.put("image", "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=" + pref + "&key=" + googlePlacesApiKey);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Google Places search error: " + e.getMessage());
            }
        }

        // 6. Open Location Engine (Nominatim) if lat/lon not yet found
        if (!result.containsKey("lat") || !result.containsKey("lon")) {
            try {
                URI nomUri = URI.create(nominatimBaseUrl + "/search?q=" 
                        + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8) 
                        + "&countrycodes=in&format=json&addressdetails=1&extratags=1&limit=1");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<List<Map<String, Object>>> res = restTemplate.exchange(
                        nomUri, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> list = res.getBody();
                
                if (list != null && !list.isEmpty()) {
                    Map<String, Object> primary = list.get(0);
                    String name = (String) primary.get("name");
                    String displayName = (String) primary.get("display_name");
                    result.put("name", name != null && !name.trim().isEmpty() ? name : (displayName != null ? displayName.split(",")[0] : cleanQuery));
                    result.put("address", displayName);
                    double lat = Double.parseDouble(String.valueOf(primary.get("lat")));
                    double lon = Double.parseDouble(String.valueOf(primary.get("lon")));
                    result.put("lat", lat);
                    result.put("lon", lon);
                    result.put("latitude", lat);
                    result.put("longitude", lon);
                    
                    String type = (String) primary.get("type");
                    String cls = (String) primary.get("class");
                    result.put("category", formatCategory(type != null ? type : cls));

                    @SuppressWarnings("unchecked")
                    Map<String, Object> addr = (Map<String, Object>) primary.get("address");
                    if (addr != null) {
                        String city = extractCity(addr);
                        String state = (String) addr.get("state");
                        result.put("city", city != null ? city : "India");
                        result.put("state", state != null ? state : "India");
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> extra = (Map<String, Object>) primary.get("extratags");
                    if (extra != null) {
                        if (extra.containsKey("opening_hours")) {
                            result.put("openingHours", extra.get("opening_hours"));
                        }
                        if (extra.containsKey("website")) {
                            result.put("website", extra.get("website"));
                        }
                        if (extra.containsKey("phone")) {
                            result.put("phone", extra.get("phone"));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Nominatim search error: " + e.getMessage());
            }
        }

        // 7. Enrich with Wikipedia summary
        String lookupTitle = (String) result.get("name");
        if (lookupTitle == null) lookupTitle = cleanQuery;
        enrichWithWikipedia(result, lookupTitle);

        if (!result.containsKey("rating") || result.get("rating") == null) {
            result.put("rating", "Information unavailable");
        }
        if (!result.containsKey("reviewsCount") || result.get("reviewsCount") == null) {
            result.put("reviewsCount", "Information unavailable");
        }
        if (!result.containsKey("openingHours") || result.get("openingHours") == null) {
            result.put("openingHours", "Information unavailable");
        }
        if (!result.containsKey("city") || result.get("city") == null) {
            result.put("city", cleanQuery);
        }
        if (!result.containsKey("state") || result.get("state") == null) {
            result.put("state", "India");
        }
        if (!result.containsKey("address") || result.get("address") == null) {
            result.put("address", cleanQuery + ", India");
        }
        if (!result.containsKey("category") || result.get("category") == null) {
            result.put("category", "Tourist Attraction");
        }

        if (result.containsKey("lat")) {
            result.put("latitude", result.get("lat"));
        }
        if (result.containsKey("lon")) {
            result.put("longitude", result.get("lon"));
        }
        result.put("verified", result.containsKey("lat") && result.get("lat") != null);

        if (result.containsKey("lat") && result.containsKey("lon")) {
            double lat = Double.parseDouble(result.get("lat").toString());
            double lon = Double.parseDouble(result.get("lon").toString());
            String city = (String) result.get("city");
            List<Map<String, Object>> nearby = discoverNearby(lat, lon, city, null, 8);
            result.put("nearbyAttractions", nearby);
        }

        searchCache.put(cacheKey, result);
        return result;
    }

    /**
     * Discovers real attractions near coordinates, prioritizing verified Indian attractions registry.
     */
    public List<Map<String, Object>> discoverNearby(double lat, double lon, String destination, String category, int limit) {
        String cacheKey = String.format(Locale.ROOT, "%.2f_%.2f_%s_%s", lat, lon, destination != null ? destination : "", category != null ? category : "");
        if (nearbyCache.containsKey(cacheKey)) {
            return nearbyCache.get(cacheKey);
        }

        List<Map<String, Object>> spots = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. PRIORITY 1: Check database attractions around this destination or coordinates
        List<Attraction> dbAttractions = new ArrayList<>();
        if (destination != null && !destination.trim().isEmpty()) {
            dbAttractions.addAll(attractionRepository.findByCityNameIgnoreCase(destination));
            if (dbAttractions.isEmpty()) {
                dbAttractions.addAll(attractionRepository.findByCityStateNameIgnoreCase(destination));
            }
            if (dbAttractions.isEmpty()) {
                dbAttractions.addAll(attractionRepository.searchAttractions(destination));
            }
        }

        // Also query all attractions and filter by radius (50 km) if few or empty
        if (dbAttractions.size() < limit) {
            List<Attraction> all = attractionRepository.findAll();
            for (Attraction a : all) {
                if (a.getLatitude() != null && a.getLongitude() != null) {
                    double dist = calculateHaversineKm(lat, lon, a.getLatitude(), a.getLongitude());
                    if (dist <= 40.0 && !dbAttractions.contains(a)) {
                        dbAttractions.add(a);
                    }
                }
            }
        }

        for (Attraction a : dbAttractions) {
            String catName = a.getCategory() != null ? a.getCategory().getName() : "Sightseeing";
            if (category != null && !matchesCategory(catName, category) && !matchesCategory(a.getName(), category)) {
                continue;
            }
            String nLow = a.getName().toLowerCase();
            if (seen.contains(nLow)) continue;
            seen.add(nLow);

            Map<String, Object> spot = new HashMap<>();
            spot.put("name", a.getName());
            spot.put("title", a.getName());
            spot.put("lat", a.getLatitude());
            spot.put("lon", a.getLongitude());
            spot.put("latitude", a.getLatitude());
            spot.put("longitude", a.getLongitude());
            spot.put("category", catName);
            spot.put("rating", a.getRating() != null ? a.getRating() : 4.7);
            spot.put("reviewsCount", a.getReviewsCount() != null ? a.getReviewsCount() : 10000);
            spot.put("openingHours", a.getOpeningTime() + " - " + a.getClosingTime());
            spot.put("entryFeeInr", a.getEntryFeeInr() != null ? a.getEntryFeeInr() : 0.0);
            spot.put("description", a.getDescription());
            spot.put("indoorOutdoor", a.getIndoorOutdoor());
            spot.put("verificationStatus", "100% Real Location • Verified");
            spots.add(spot);
            if (spots.size() >= limit) break;
        }

        // 2. PRIORITY 2: Wikipedia GeoSearch strictly for authentic tourist attractions only
        if (spots.size() < limit) {
            try {
                String geoCoordStr = String.format(Locale.ROOT, "%.4f%%7C%.4f", lat, lon);
                URI wikiGeoUri = URI.create("https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=" 
                        + geoCoordStr + "&gsradius=10000&gslimit=30&format=json");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                        wikiGeoUri, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
                Map<String, Object> body = res.getBody();
                if (body != null && body.containsKey("query")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> qMap = (Map<String, Object>) body.get("query");
                    if (qMap != null && qMap.containsKey("geosearch")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) qMap.get("geosearch");
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                String title = (String) item.get("title");
                                if (title == null) continue;
                                String tLow = title.toLowerCase();
                                
                                // Strict Tourist Check: Reject schools, hospitals, courts, offices, localities
                                if (seen.contains(tLow) || isNonTouristOrNoise(tLow, null, null)) continue;
                                if (!isAuthenticTouristAttraction(tLow, null, null)) continue;

                                String inferredCategory = inferCategoryFromTitle(title);
                                if (inferredCategory == null) continue;

                                if (category != null && !matchesCategory(inferredCategory, category) && !matchesCategory(tLow, category)) {
                                    continue;
                                }

                                Map<String, Object> spot = new HashMap<>();
                                spot.put("name", title);
                                spot.put("title", title);
                                spot.put("lat", item.get("lat"));
                                spot.put("lon", item.get("lon"));
                                spot.put("latitude", item.get("lat"));
                                spot.put("longitude", item.get("lon"));
                                spot.put("category", inferredCategory);
                                spot.put("rating", "Information unavailable");
                                spot.put("reviewsCount", "Information unavailable");
                                spot.put("openingHours", "Information unavailable");
                                spot.put("verificationStatus", "100% Real Location • Verified");

                                seen.add(tLow);
                                spots.add(spot);
                                if (spots.size() >= limit) break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Wikipedia geosearch error: " + e.getMessage());
            }
        }

        // Fetch thumbnails for top spots from Wikipedia if image not present
        for (int i = 0; i < Math.min(spots.size(), 4); i++) {
            Map<String, Object> s = spots.get(i);
            enrichWithWikipedia(s, (String) s.get("name"));
        }

        nearbyCache.put(cacheKey, spots);
        return spots;
    }

    /**
     * Category-based discovery in and around a specific Indian destination.
     */
    public List<Map<String, Object>> discoverByCategory(String destination, String category) {
        if (destination == null || destination.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cacheKey = (destination + "_" + (category != null ? category : "all")).toLowerCase();
        if (categoryCache.containsKey(cacheKey)) {
            return categoryCache.get(cacheKey);
        }

        List<Map<String, Object>> spots = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Check database attractions directly matching destination & category
        List<Attraction> dbAttrs = attractionRepository.findByCityNameIgnoreCase(destination);
        if (dbAttrs.isEmpty()) {
            dbAttrs = attractionRepository.findByCityStateNameIgnoreCase(destination);
        }
        if (dbAttrs.isEmpty()) {
            dbAttrs = attractionRepository.searchAttractions(destination);
        }

        for (Attraction a : dbAttrs) {
            String catName = a.getCategory() != null ? a.getCategory().getName() : "";
            if (category == null || matchesCategory(catName, category) || matchesCategory(a.getName(), category)) {
                String nLow = a.getName().toLowerCase();
                if (!seen.contains(nLow)) {
                    seen.add(nLow);
                    Map<String, Object> spot = new HashMap<>();
                    spot.put("name", a.getName());
                    spot.put("title", a.getName());
                    spot.put("lat", a.getLatitude());
                    spot.put("lon", a.getLongitude());
                    spot.put("latitude", a.getLatitude());
                    spot.put("longitude", a.getLongitude());
                    spot.put("category", catName);
                    spot.put("rating", a.getRating() != null ? a.getRating() : 4.7);
                    spot.put("reviewsCount", a.getReviewsCount() != null ? a.getReviewsCount() : 8000);
                    spot.put("openingHours", a.getOpeningTime() + " - " + a.getClosingTime());
                    spot.put("entryFeeInr", a.getEntryFeeInr() != null ? a.getEntryFeeInr() : 0.0);
                    spot.put("description", a.getDescription());
                    spot.put("indoorOutdoor", a.getIndoorOutdoor());
                    spot.put("verificationStatus", "100% Real Location • Verified");
                    spots.add(spot);
                }
            }
        }

        // If found from DB, enrich thumbnails and return
        if (!spots.isEmpty()) {
            for (int i = 0; i < Math.min(spots.size(), 4); i++) {
                Map<String, Object> s = spots.get(i);
                enrichWithWikipedia(s, (String) s.get("name"));
            }
            categoryCache.put(cacheKey, spots);
            return spots;
        }

        // 2. Fallback to discoverNearby
        Map<String, Object> destDetails = searchPlace(destination);
        if (destDetails.containsKey("lat") && destDetails.containsKey("lon")) {
            double lat = Double.parseDouble(destDetails.get("lat").toString());
            double lon = Double.parseDouble(destDetails.get("lon").toString());
            spots.addAll(discoverNearby(lat, lon, destination, category, 10));
        }

        categoryCache.put(cacheKey, spots);
        return spots;
    }

    public boolean isNonTouristOrNoise(String name, String type, String osmClass) {
        if (name == null || name.trim().isEmpty()) return true;
        String n = name.toLowerCase().trim();

        // 0. Whitelist famous tourist landmarks, heritage routes, and iconic bridges/roads/gardens
        if (n.contains("howrah bridge") || n.contains("vidyasagar setu") || n.contains("pamban bridge") 
                || n.contains("mall road") || n.contains("the ridge") || n.contains("mg marg")
                || n.contains("marine drive") || n.contains("bandra-worli sea link") || n.contains("sea link")
                || n.contains("shalimar") || n.contains("nishat bagh") || n.contains("chashme shahi")
                || n.contains("ram ki paidi") || n.contains("lakshman jhula") || n.contains("ram jhula") 
                || n.contains("janki jhula") || n.contains("atal bridge") || n.contains("coronation bridge")
                || n.contains("fontainhas") || n.contains("panchakki") || n.contains("silathoranam")
                || n.contains("dharohar") || n.contains("bagore ki haveli") || n.contains("toorji ka jhalra")
                || n.contains("shilpgram") || n.contains("bada bagh") || n.contains("swargadwar")
                || n.contains("raghurajpur") || n.contains("bapu museum") || n.contains("kursura")
                || n.contains("sea harrier") || n.contains("bhavani island") || n.contains("gandhi hill")
                || n.contains("chaparai") || n.contains("katiki") || n.contains("talakona")
                || n.contains("borra caves") || n.contains("belum caves") || n.contains("pennar")
                || n.contains("mahakal lok") || n.contains("ram ghat") || n.contains("vishram ghat")
                || n.contains("nidhivan") || n.contains("govardhan hill") || n.contains("raneh falls")) {
            return false;
        }

        // 1. Education noise: ordinary nursery, primary schools, tuitions, coaching (colleges & universities are preserved)
        if (n.contains("kindergarten") || n.contains("nursery") || n.contains("tuition") || n.contains("coaching")
                || n.contains("senior secondary") || n.contains("primary school") || n.contains("play school")
                || n.contains("dps ") || n.contains("dav ") || n.contains("play group")) {
            return true;
        }

        // 2. Health & Medical: hospitals, clinics, dispensaries, nursing homes
        if (n.contains("hospital") || n.contains("clinic") || n.contains("dispensary") || n.contains("nursing home")
                || n.contains("medical centre") || n.contains("medical center") || n.contains("health centre")
                || n.contains("health center") || n.contains("phc") || n.contains("maternity") || n.contains("dental")
                || n.contains("pathology") || n.contains("diagnostic") || n.contains("pharmacy") || n.contains("chemist")
                || n.contains("ayurvedic hospital") || n.contains("homeo")) {
            return true;
        }

        // 3. Judicial, Administrative, Offices & Law Enforcement
        if (n.contains("court") || n.contains("tees hazari") || n.contains("tis hazari") || n.contains("patiala house")
                || n.contains("karkardooma") || n.contains("rohini court") || n.contains("saket court")
                || n.contains("nyayalaya") || n.contains("tribunal") || n.contains("police") || n.contains("thana")
                || n.contains("chowki") || n.contains("jail") || n.contains("prison") || n.contains("collectorate")
                || n.contains("secretariat") || n.contains("tahsildar") || n.contains("tehsil") || n.contains("panchayat")
                || n.contains("mandal office") || n.contains("corporation office") || n.contains("commissioner")
                || n.contains("ministry") || n.contains("directorate") || n.contains("headquarters")
                || n.contains("head office") || n.contains("branch office") || n.contains("sub-division")
                || n.contains("department of") || n.contains("dept of")) {
            return true;
        }

        // Specific administrative / residential buildings (except famous royal palaces or President residence)
        if (n.contains("lok niwas") || n.contains("raj niwas") || n.contains("shastri bhavan")
                || n.contains("krishi bhavan") || n.contains("rail bhavan") || n.contains("sanchar bhavan")
                || n.contains("udyog bhavan") || n.contains("vikas bhavan") || n.contains("yojna bhavan")
                || n.contains("dak bhavan") || n.contains("vidhan bhavan") || n.contains("sena bhavan")) {
            return true;
        }

        // 4. Residential Buildings, Housing, and Ordinary Localities
        if (n.contains("colony") || n.contains("enclave") || n.contains("apartments") || n.contains("apartment")
                || n.contains("residency") || n.contains("housing society") || n.contains("cooperative society")
                || n.contains("vihar") || n.contains("mohalla") || n.contains("gali") || n.contains("lane")
                || n.contains("sector ") || n.contains("pocket ") || n.contains("block ")
                || n.contains("karol bagh") || n.contains("lajpat nagar") || n.contains("sarojini nagar")
                || n.contains("kamla nagar") || n.contains("pitampura") || n.contains("rohini")
                || n.contains("dwarka") || n.contains("janakpuri") || n.contains("paschim vihar")
                || n.contains("uttam nagar") || n.contains("shahdara") || n.contains("patparganj")
                || n.contains("mayur vihar") || n.contains("vikaspuri") || n.contains("shalimar bagh")
                || n.contains("ashok vihar") || n.contains("model town") || n.contains("punjabi bagh")
                || n.contains("tilak nagar") || n.contains("rajouri garden") || n.contains("kirti nagar")) {
            return true;
        }

        // 5. Commercial, Utilities & Infrastructure
        if (n.contains("bus stop") || n.contains("bus stand") || n.contains("bus terminal")
                || n.contains("railway station") || n.contains("junction") || n.contains("metro station")
                || n.contains("flyover") || n.contains("bridge") || n.contains("underpass") || n.contains("subway")
                || n.contains("toll plaza") || n.contains("parking") || n.contains("petrol pump")
                || n.contains("cng station") || n.contains("atm") || n.contains("bank")
                || n.contains("warehouse") || n.contains("depot") || n.contains("factory") || n.contains("industrial area")
                || n.contains("bypas") || n.contains("road") || n.contains("marg") || n.contains("street")) {
            return true;
        }

        // 6. Administrative / Statistical boundaries
        if (n.contains("mandal") || n.contains("district") || n.contains("division") || n.contains("constituency")
                || n.contains("ward") || n.contains("taluk") || n.contains("census town") || n.contains("township")) {
            return true;
        }

        // 7. Check OSM type / class
        if (type != null) {
            String t = type.toLowerCase();
            if (t.equals("kindergarten") || t.equals("hospital")
                    || t.equals("clinic") || t.equals("pharmacy") || t.equals("bank") || t.equals("atm")
                    || t.equals("courthouse") || t.equals("police") || t.equals("post_office") || t.equals("fuel")
                    || t.equals("parking") || t.equals("bus_stop") || t.equals("station") || t.equals("railway")) {
                return true;
            }
        }
        if (osmClass != null) {
            String c = osmClass.toLowerCase();
            if (c.equals("highway") || c.equals("railway") || c.equals("boundary") || c.equals("office")) {
                return true;
            }
        }

        return false;
    }

    public boolean isAuthenticTouristAttraction(String name, String type, String osmClass) {
        if (name == null || name.trim().isEmpty()) return false;
        
        // Fast-path: If place exists in our curated authentic database, it is 100% authentic
        if (attractionRepository != null && attractionRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
            return true;
        }

        if (isNonTouristOrNoise(name, type, osmClass)) {
            return false;
        }
        String n = name.toLowerCase().trim();

        // Must match recognized tourist criteria
        boolean isHeritage = n.contains("fort") || n.contains("palace") || n.contains("qila") || n.contains("killa")
                || n.contains("mahal") || n.contains("tomb") || n.contains("caves") || n.contains("cave")
                || n.contains("monument") || n.contains("memorial") || n.contains("minar") || n.contains("minaret")
                || n.contains("stupa") || n.contains("baoli") || n.contains("stepwell") || n.contains("gate")
                || n.contains("pillar") || n.contains("ruins") || n.contains("citadel") || n.contains("archaeological")
                || n.contains("heritage") || n.contains("observatory") || n.contains("jantar mantar")
                || n.contains("haveli") || n.contains("chhatri") || n.contains("cenotaph") || n.contains("bavdi")
                || n.contains("arch") || n.contains("barrage") || n.contains("statue");

        boolean isSpiritual = n.contains("temple") || n.contains("mandir") || n.contains("masjid") || n.contains("mosque")
                || n.contains("church") || n.contains("cathedral") || n.contains("basilica") || n.contains("gurudwara")
                || n.contains("dargah") || n.contains("ashram") || n.contains("monastery") || n.contains("shrine")
                || n.contains("jyotirlinga") || n.contains("ghat") || n.contains("pagoda") || n.contains("aarti")
                || n.contains("kund") || n.contains("theertham") || n.contains("samadhi");

        boolean isMuseum = n.contains("museum") || n.contains("gallery") || n.contains("planetarium")
                || n.contains("exhibition") || n.contains("submarine") || n.contains("aircraft");

        boolean isNatureOrPark = n.contains("national park") || n.contains("wildlife") || n.contains("sanctuary")
                || n.contains("safari") || n.contains("zoo") || n.contains("waterfall") || n.contains("falls")
                || n.contains("beach") || n.contains("cove") || n.contains("lake") || n.contains("dam")
                || n.contains("backwater") || n.contains("valley") || n.contains("peak") || n.contains("viewpoint")
                || n.contains("sunset point") || n.contains("hill station") || n.contains("pass")
                || n.contains("botanical garden") || n.contains("deer park") || n.contains("rock garden")
                || n.contains("mughal garden") || n.contains("amrit udyan") || n.contains("garden of five senses")
                || n.contains("lodhi garden") || n.contains("garden") || n.contains("park") || n.contains("dunes")
                || n.contains("canyon") || n.contains("gorge") || n.contains("island") || n.contains("plantation")
                || n.contains("promenade") || n.contains("ridge");

        boolean isCultureOrCraft = n.contains("craft") || n.contains("village") || n.contains("bazaar")
                || n.contains("market") || n.contains("chowk") || n.contains("latin quarter")
                || n.contains("corridor") || n.contains("culture") || n.contains("fountain")
                || n.contains("laser show") || n.contains("sound and light");

        boolean isRecognizedLandmark = n.contains("india gate") || n.contains("red fort") || n.contains("qutub minar")
                || n.contains("charminar") || n.contains("taj mahal") || n.contains("gateway of india")
                || n.contains("hawa mahal") || n.contains("amber palace") || n.contains("statue of unity")
                || n.contains("victoria memorial") || n.contains("rashtrapati bhavan") || n.contains("raj ghat")
                || n.contains("khari baoli") || n.contains("chandni chowk") || n.contains("marine drive")
                || n.contains("howrah bridge") || n.contains("mall road") || n.contains("the ridge");

        return isHeritage || isSpiritual || isMuseum || isNatureOrPark || isCultureOrCraft || isRecognizedLandmark;
    }

    public boolean isLocalityNoise(String name) {
        return isNonTouristOrNoise(name, null, null);
    }

    private void enrichWithWikipedia(Map<String, Object> target, String title) {
        if (title == null || title.trim().isEmpty()) return;
        try {
            String safeTitle = title.trim().replaceAll("\\s+", "_");
            URI uri = URI.create("https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(safeTitle, StandardCharsets.UTF_8));
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = res.getBody();
            if (body != null) {
                if (body.containsKey("extract") && !target.containsKey("description")) {
                    target.put("description", body.get("extract"));
                }
                if (body.containsKey("thumbnail") && !target.containsKey("image")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> thumb = (Map<String, Object>) body.get("thumbnail");
                    if (thumb != null && thumb.containsKey("source")) {
                        target.put("image", thumb.get("source"));
                    }
                }
                if (body.containsKey("coordinates") && (!target.containsKey("lat") || target.get("lat") == null)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> coords = (Map<String, Object>) body.get("coordinates");
                    if (coords != null) {
                        double lat = Double.parseDouble(coords.get("lat").toString());
                        double lon = Double.parseDouble(coords.get("lon").toString());
                        target.put("lat", lat);
                        target.put("lon", lon);
                        target.put("latitude", lat);
                        target.put("longitude", lon);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private String cleanDisplayName(String displayName) {
        if (displayName == null) return "";
        String[] parts = displayName.split(",");
        if (parts.length > 3) {
            return parts[0].trim() + ", " + parts[1].trim() + ", " + parts[parts.length - 2].trim() + ", India";
        }
        return displayName;
    }

    private String extractCity(Map<String, Object> addr) {
        if (addr.containsKey("city")) return (String) addr.get("city");
        if (addr.containsKey("town")) return (String) addr.get("town");
        if (addr.containsKey("suburb")) return (String) addr.get("suburb");
        if (addr.containsKey("village")) return (String) addr.get("village");
        if (addr.containsKey("county")) return (String) addr.get("county");
        if (addr.containsKey("state_district")) return (String) addr.get("state_district");
        return null;
    }

    private String formatCategory(String raw) {
        if (raw == null) return "Sightseeing";
        String l = raw.toLowerCase();
        if (l.contains("university") || l.contains("college") || l.contains("institute") || l.contains("education") || l.contains("campus") || l.contains("school") || l.contains("academy") || l.contains("polytechnic")) return "Education / University";
        if (l.contains("worship") || l.contains("temple") || l.contains("spiritual") || l.contains("mosque") || l.contains("church") || l.contains("mandir") || l.contains("gurdwara") || l.contains("dargah") || l.contains("religious")) return "Temples / Spiritual";
        if (l.contains("museum") || l.contains("art") || l.contains("gallery") || l.contains("memorial") || l.contains("planetarium") || l.contains("submarine")) return "Museums";
        if (l.contains("monument") || l.contains("historic") || l.contains("caves") || l.contains("tomb") || l.contains("archaeological") || l.contains("fort") || l.contains("palace") || l.contains("castle") || l.contains("qila")) return "Historical / Heritage";
        if (l.contains("beach") || l.contains("coast") || l.contains("shore")) return "Beaches";
        if (l.contains("mountain") || l.contains("hill") || l.contains("valley") || l.contains("peak")) return "Nature / Hill Stations";
        if (l.contains("viewpoint") || l.contains("view point") || l.contains("overlook")) return "Viewpoints / Attractions";
        if (l.contains("waterfall") || l.contains("falls")) return "Nature / Hill Stations";
        if (l.contains("lake") || l.contains("dam") || l.contains("barrage") || l.contains("water")) return "Lakes";
        if (l.contains("park") || l.contains("garden") || l.contains("leisure") || l.contains("botanical")) return "Parks & Gardens";
        if (l.contains("wildlife") || l.contains("zoo") || l.contains("sanctuary") || l.contains("safari") || l.contains("aquarium")) return "Wildlife / Zoo";
        if (l.contains("adventure") || l.contains("trek") || l.contains("sports")) return "Adventure";
        if (l.contains("food") || l.contains("restaurant") || l.contains("cafe") || l.contains("dining")) return "Food & Dining";
        return "Sightseeing & Attractions";
    }

    public String inferCategoryFromTitle(String title) {
        if (title == null || title.trim().isEmpty()) return null;
        String t = title.toLowerCase().trim();

        if (t.contains("university") || t.contains("college") || t.contains("institute") || t.contains("vidya parishad")
                || t.contains("gitam") || t.contains("campus") || t.contains("iit") || t.contains("iim")
                || t.contains("nit") || t.contains("academy") || t.contains("polytechnic")) {
            return "Education / University";
        }
        if (t.contains("temple") || t.contains("mandir") || t.contains("masjid") || t.contains("mosque")
                || t.contains("church") || t.contains("cathedral") || t.contains("basilica") || t.contains("gurudwara")
                || t.contains("dargah") || t.contains("ashram") || t.contains("monastery") || t.contains("shrine")
                || t.contains("jyotirlinga") || t.contains("ghat")) {
            return "Temples / Spiritual";
        }
        if (t.contains("fort") || t.contains("palace") || t.contains("mahal") || t.contains("qila") || t.contains("killa") || t.contains("citadel")) {
            return "Forts and Palaces";
        }
        if (t.contains("monument") || t.contains("memorial") || t.contains("tomb") || t.contains("caves") || t.contains("cave")
                || t.contains("stupa") || t.contains("baoli") || t.contains("stepwell") || t.contains("minar") || t.contains("minaret")
                || t.contains("pillar") || t.contains("ruins") || t.contains("archaeological") || t.contains("heritage")
                || t.contains("observatory") || t.contains("jantar mantar") || t.contains("india gate") || t.contains("raj ghat")
                || t.contains("rashtrapati bhavan")) {
            return "Historical Places";
        }
        if (t.contains("museum") || t.contains("gallery") || t.contains("planetarium")) {
            return "Museums";
        }
        if (t.contains("beach") || t.contains("cove") || t.contains("coast")) {
            return "Beaches";
        }
        if (t.contains("falls") || t.contains("waterfall")) {
            return "Waterfalls";
        }
        if (t.contains("lake") || t.contains("dam") || t.contains("backwater") || t.contains("barrage") || t.contains("tso")) {
            return "Lakes";
        }
        if (t.contains("national park") || t.contains("wildlife") || t.contains("sanctuary") || t.contains("safari") || t.contains("zoo")) {
            return "Nature and Wildlife";
        }
        if (t.contains("park") || t.contains("garden") || t.contains("botanical") || t.contains("udyan")) {
            return "Parks";
        }
        if (t.contains("valley") || t.contains("peak") || t.contains("hill") || t.contains("point") || t.contains("pass") || t.contains("ridge") || t.contains("ghat")) {
            return "Hill Stations";
        }
        if (t.contains("rafting") || t.contains("adventure") || t.contains("trek") || t.contains("ropeway") || t.contains("cable car")) {
            return "Adventure";
        }
        if (t.contains("bazaar") || t.contains("chowk") || t.contains("market") || t.contains("culture")) {
            return "Culture";
        }

        // Return null instead of falsely classifying unknown names as Historical Places
        return null;
    }

    public boolean matchesCategory(String titleOrCat, String category) {
        if (category == null || category.trim().isEmpty() || "ALL".equalsIgnoreCase(category)) return true;
        if (titleOrCat == null || titleOrCat.trim().isEmpty()) return false;
        String c = category.toUpperCase().trim();
        String t = titleOrCat.toUpperCase().trim();
        if (t.equals(c) || t.contains(c) || c.contains(t)) return true;

        if (c.contains("HISTORIC") || c.contains("HERITAGE")) {
            return t.contains("HISTORIC") || t.contains("HERITAGE") || t.contains("MONUMENT") || t.contains("MEMORIAL")
                    || t.contains("TOMB") || t.contains("CAVE") || t.contains("STUPA") || t.contains("BAOLI")
                    || t.contains("STEPWELL") || t.contains("MINAR") || t.contains("ARCHAEOLOGICAL")
                    || t.contains("RUINS") || t.contains("FORT") || t.contains("PALACE") || t.contains("QILA")
                    || t.contains("INDIA GATE") || t.contains("RAJ GHAT") || t.contains("JANTAR MANTAR")
                    || t.contains("RASHTRAPATI BHAVAN");
        }
        if (c.contains("FORT") || c.contains("PALACE")) {
            return t.contains("FORT") || t.contains("PALACE") || t.contains("MAHAL") || t.contains("CASTLE") || t.contains("QILA") || t.contains("CITADEL");
        }
        if (c.contains("TEMPLE") || c.contains("RELIGIOUS") || c.contains("SPIRITUAL")) {
            return t.contains("TEMPLE") || t.contains("MANDIR") || t.contains("CHURCH") || t.contains("MOSQUE") || t.contains("MASJID")
                    || t.contains("SHRINE") || t.contains("GURUDWARA") || t.contains("DARGAH") || t.contains("BASILICA") || t.contains("ASHRAM")
                    || t.contains("CATHEDRAL") || t.contains("MONASTERY") || t.contains("GHAT");
        }
        if (c.contains("MUSEUM")) {
            return t.contains("MUSEUM") || t.contains("GALLERY") || t.contains("PLANETARIUM");
        }
        if (c.contains("BEACH")) {
            return t.contains("BEACH") || t.contains("COAST") || t.contains("COVE");
        }
        if (c.contains("WATERFALL")) {
            return t.contains("WATERFALL") || t.contains("FALLS");
        }
        if (c.contains("LAKE")) {
            return t.contains("LAKE") || t.contains("DAM") || t.contains("BACKWATER") || t.contains("TSO") || t.contains("BARRAGE");
        }
        if (c.contains("PARK")) {
            return t.contains("PARK") || t.contains("GARDEN") || t.contains("BOTANICAL") || t.contains("UDYAN");
        }
        if (c.contains("WILDLIFE") || c.contains("NATURE")) {
            return t.contains("WILDLIFE") || t.contains("SANCTUARY") || t.contains("NATIONAL PARK") || t.contains("ZOO") || t.contains("SAFARI");
        }
        if (c.contains("HILL") || c.contains("MOUNTAIN")) {
            return t.contains("HILL") || t.contains("VALLEY") || t.contains("PEAK") || t.contains("PASS") || t.contains("RIDGE") || t.contains("POINT") || t.contains("LOOKOUT");
        }
        if (c.contains("ADVENTURE")) {
            return t.contains("RAFTING") || t.contains("SAFARI") || t.contains("TREK") || t.contains("ADVENTURE") || t.contains("PARAGLIDING") || t.contains("ROPEWAY");
        }

        return false;
    }

    /**
     * Searches legitimate tourist attractions within a destination for manual itinerary addition or place swapping.
     * Computes distance from the previous attraction and estimated travel duration.
     */
    public List<Map<String, Object>> searchDestinationAttractions(String destination, String query, Double prevLat, Double prevLon) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (destination == null || destination.trim().isEmpty()) {
            return results;
        }

        String destTrim = destination.trim();
        List<Attraction> list = attractionRepository.findByCityNameIgnoreCaseAndIsAvailableTrue(destTrim);
        if (list == null || list.isEmpty()) {
            list = attractionRepository.searchAttractions(destTrim);
        }

        String q = (query != null) ? query.trim().toLowerCase() : "";
        Set<Long> seenIds = new HashSet<>();

        for (Attraction a : list) {
            if (!q.isEmpty()) {
                boolean match = (a.getName() != null && a.getName().toLowerCase().contains(q))
                        || (a.getDescription() != null && a.getDescription().toLowerCase().contains(q))
                        || (a.getCategory() != null && a.getCategory().getName().toLowerCase().contains(q))
                        || (a.getSubcategory() != null && a.getSubcategory().toLowerCase().contains(q))
                        || (a.getTags() != null && a.getTags().toLowerCase().contains(q));
                if (!match) continue;
            }

            seenIds.add(a.getId());
            results.add(mapAttractionToDto(a, destTrim, prevLat, prevLon));
        }

        // If specific search query provided and fewer results, search general attractions
        if (results.size() < 4 && !q.isEmpty()) {
            List<Attraction> extra = attractionRepository.searchAttractions(q);
            for (Attraction a : extra) {
                if (!seenIds.contains(a.getId())) {
                    seenIds.add(a.getId());
                    results.add(mapAttractionToDto(a, destTrim, prevLat, prevLon));
                }
            }
        }

        return results;
    }

    private Map<String, Object> mapAttractionToDto(Attraction a, String destFallback, Double prevLat, Double prevLon) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("title", a.getName());
        m.put("category", a.getCategory() != null ? a.getCategory().getName() : "Attraction");
        m.put("subcategory", a.getSubcategory());
        m.put("description", a.getDescription());
        m.put("latitude", a.getLatitude());
        m.put("longitude", a.getLongitude());
        m.put("durationHours", a.getDurationHours() != null ? a.getDurationHours() : 2.0);
        m.put("entryFeeInr", a.getEntryFeeInr() != null ? a.getEntryFeeInr() : 0.0);
        m.put("costInr", a.getEntryFeeInr() != null ? a.getEntryFeeInr() : 0.0);
        m.put("indoorOutdoor", a.getIndoorOutdoor() != null ? a.getIndoorOutdoor() : "Outdoor");
        m.put("rating", a.getRating() != null ? a.getRating() : 4.5);
        m.put("reviewCount", a.getReviewsCount() != null ? a.getReviewsCount() : 1200);
        m.put("openingTime", a.getOpeningTime() != null ? a.getOpeningTime() : "09:00");
        m.put("closingTime", a.getClosingTime() != null ? a.getClosingTime() : "18:00");
        m.put("openingHours", (a.getOpeningTime() != null ? a.getOpeningTime() : "09:00") + " - " + (a.getClosingTime() != null ? a.getClosingTime() : "18:00"));
        m.put("location", a.getCity() != null ? a.getCity().getName() : destFallback);
        m.put("address", (a.getCity() != null ? a.getCity().getName() + ", " : "") + (a.getCity() != null && a.getCity().getState() != null ? a.getCity().getState().getName() : "India"));

        if (prevLat != null && prevLon != null && a.getLatitude() != null && a.getLongitude() != null) {
            double distKm = calculateHaversineKm(prevLat, prevLon, a.getLatitude(), a.getLongitude());
            m.put("distanceKm", Math.round(distKm * 10.0) / 10.0);
            int transitMins = (int) Math.max(5, Math.round((distKm / 30.0) * 60) + 5);
            m.put("travelTimeMinutes", transitMins);
        } else {
            m.put("distanceKm", null);
            m.put("travelTimeMinutes", null);
        }

        return m;
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
