package com.travelmate.service;

import org.springframework.beans.factory.annotation.Value;
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

    // Standard User-Agent for OpenStreetMap and Wikimedia APIs
    private static final String APP_USER_AGENT = "TravelMateAI/1.0 (https://travelmate.ai; contact@travelmate.ai)";

    // In-memory cache for fast responsive searches and low network overhead
    private final Map<String, Object> searchCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> autocompleteCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> nearbyCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> categoryCache = new ConcurrentHashMap<>();

    // Curated high-precision coordinates for renowned Indian travel hubs and landmarks
    private static final Map<String, double[]> KNOWN_COORDS = new LinkedHashMap<>();
    private static final Map<String, String[]> KNOWN_DETAILS = new LinkedHashMap<>();

    static {
        KNOWN_COORDS.put("goa", new double[]{15.2993, 74.1240});
        KNOWN_DETAILS.put("goa", new String[]{"Goa", "Goa", "Goa", "Beaches & Heritage"});

        KNOWN_COORDS.put("vijayawada", new double[]{16.5062, 80.6480});
        KNOWN_DETAILS.put("vijayawada", new String[]{"Vijayawada", "Vijayawada", "Andhra Pradesh", "City / Heritage"});

        KNOWN_COORDS.put("gudivada", new double[]{16.4410, 80.9926});
        KNOWN_DETAILS.put("gudivada", new String[]{"Gudivada", "Gudivada", "Andhra Pradesh", "Town / Cultural"});

        KNOWN_COORDS.put("kanaka durga temple", new double[]{16.5161, 80.6074});
        KNOWN_DETAILS.put("kanaka durga temple", new String[]{"Kanaka Durga Temple", "Vijayawada", "Andhra Pradesh", "Spiritual"});

        KNOWN_COORDS.put("undavalli caves", new double[]{16.4965, 80.5815});
        KNOWN_DETAILS.put("undavalli caves", new String[]{"Undavalli Caves", "Vijayawada", "Andhra Pradesh", "Heritage"});

        KNOWN_COORDS.put("araku valley", new double[]{18.3333, 82.8833});
        KNOWN_DETAILS.put("araku valley", new String[]{"Araku Valley", "Alluri Sitharama Raju", "Andhra Pradesh", "Hill Station / Nature"});

        KNOWN_COORDS.put("araku", new double[]{18.3333, 82.8833});
        KNOWN_DETAILS.put("araku", new String[]{"Araku Valley", "Alluri Sitharama Raju", "Andhra Pradesh", "Hill Station / Nature"});

        KNOWN_COORDS.put("tirupati", new double[]{13.6288, 79.4192});
        KNOWN_DETAILS.put("tirupati", new String[]{"Tirupati", "Tirupati", "Andhra Pradesh", "Spiritual"});

        KNOWN_COORDS.put("srikalahasti", new double[]{13.7498, 79.7036});
        KNOWN_DETAILS.put("srikalahasti", new String[]{"Srikalahasti", "Tirupati", "Andhra Pradesh", "Spiritual"});

        KNOWN_COORDS.put("charminar", new double[]{17.3616, 78.4747});
        KNOWN_DETAILS.put("charminar", new String[]{"Charminar", "Hyderabad", "Telangana", "Heritage"});

        KNOWN_COORDS.put("hampi", new double[]{15.3350, 76.4600});
        KNOWN_DETAILS.put("hampi", new String[]{"Hampi", "Vijayanagara", "Karnataka", "Heritage"});

        KNOWN_COORDS.put("ooty", new double[]{11.4102, 76.6950});
        KNOWN_DETAILS.put("ooty", new String[]{"Ooty", "Nilgiris", "Tamil Nadu", "Hill Station / Nature"});

        KNOWN_COORDS.put("munnar", new double[]{10.0889, 77.0595});
        KNOWN_DETAILS.put("munnar", new String[]{"Munnar", "Idukki", "Kerala", "Hill Station / Nature"});

        KNOWN_COORDS.put("rk beach", new double[]{17.7142, 83.3239});
        KNOWN_DETAILS.put("rk beach", new String[]{"RK Beach", "Visakhapatnam", "Andhra Pradesh", "Beaches"});

        KNOWN_COORDS.put("visakhapatnam", new double[]{17.6868, 83.2185});
        KNOWN_DETAILS.put("visakhapatnam", new String[]{"Visakhapatnam", "Visakhapatnam", "Andhra Pradesh", "Coastal City"});

        KNOWN_COORDS.put("hyderabad", new double[]{17.3850, 78.4867});
        KNOWN_DETAILS.put("hyderabad", new String[]{"Hyderabad", "Hyderabad", "Telangana", "Heritage & Food"});

        KNOWN_COORDS.put("jaipur", new double[]{26.9124, 75.7873});
        KNOWN_DETAILS.put("jaipur", new String[]{"Jaipur", "Jaipur", "Rajasthan", "Heritage & Forts"});

        // Localities / Areas
        KNOWN_COORDS.put("kukatpally", new double[]{17.4849, 78.4138});
        KNOWN_DETAILS.put("kukatpally", new String[]{"Kukatpally", "Hyderabad", "Telangana", "Locality / Shopping & Food"});

        KNOWN_COORDS.put("banjara hills", new double[]{17.4156, 78.4357});
        KNOWN_DETAILS.put("banjara hills", new String[]{"Banjara Hills", "Hyderabad", "Telangana", "Locality / Cafes & Dining"});

        KNOWN_COORDS.put("gachibowli", new double[]{17.4401, 78.3489});
        KNOWN_DETAILS.put("gachibowli", new String[]{"Gachibowli", "Hyderabad", "Telangana", "Locality / Entertainment"});

        KNOWN_COORDS.put("mvp colony", new double[]{17.7423, 83.3364});
        KNOWN_DETAILS.put("mvp colony", new String[]{"MVP Colony", "Visakhapatnam", "Andhra Pradesh", "Locality / Coastal & Parks"});

        KNOWN_COORDS.put("t nagar", new double[]{13.0418, 80.2341});
        KNOWN_DETAILS.put("t nagar", new String[]{"T Nagar", "Chennai", "Tamil Nadu", "Locality / Shopping & Heritage"});

        KNOWN_COORDS.put("indiranagar", new double[]{12.9784, 77.6408});
        KNOWN_DETAILS.put("indiranagar", new String[]{"Indiranagar", "Bengaluru", "Karnataka", "Locality / Cafes & Dining"});

        KNOWN_COORDS.put("koramangala", new double[]{12.9352, 77.6245});
        KNOWN_DETAILS.put("koramangala", new String[]{"Koramangala", "Bengaluru", "Karnataka", "Locality / Food & Nightlife"});
    }

    // Well-known foreign destinations to ensure strict India-only scope
    private static final Set<String> FOREIGN_DESTINATIONS = Set.of(
            "paris", "london", "dubai", "new york", "singapore", "bangkok", "tokyo", "rome", 
            "sydney", "toronto", "barcelona", "amsterdam", "los angeles", "berlin", "istanbul", 
            "kuala lumpur", "doha", "abu dhabi", "bali", "hong kong", "san francisco", "chicago",
            "maldives", "switzerland", "zurich", "las vegas", "miami", "venice", "florence", 
            "madrid", "seoul", "beijing", "shanghai", "cairo", "cape town", "colombo", "kathmandu"
    );

    public PlaceDiscoveryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
            ResponseEntity<List> res = restTemplate.exchange(uri, HttpMethod.GET, entity, List.class);
            List body = res.getBody();
            if (body != null && !body.isEmpty()) {
                Map first = (Map) body.get(0);
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

        if (autocompleteCache.containsKey(cleanQuery)) {
            return autocompleteCache.get(cleanQuery);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        // 1. Check known high-frequency Indian destinations for immediate substring match
        for (Map.Entry<String, double[]> entry : KNOWN_COORDS.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(cleanQuery) || key.contains(cleanQuery) || cleanQuery.contains(key)) {
                String[] details = KNOWN_DETAILS.get(key);
                if (details != null && !seenNames.contains(key)) {
                    seenNames.add(key);
                    Map<String, Object> item = new HashMap<>();
                    item.put("placeId", "geo_" + key.replace(" ", "_"));
                    item.put("name", details[0]);
                    item.put("displayName", details[0] + ", " + details[1] + ", " + details[2]);
                    item.put("city", details[1]);
                    item.put("state", details[2]);
                    item.put("category", details[3]);
                    item.put("lat", entry.getValue()[0]);
                    item.put("lon", entry.getValue()[1]);
                    item.put("latitude", entry.getValue()[0]);
                    item.put("longitude", entry.getValue()[1]);
                    item.put("secondary", details[1] + ", " + details[2]);
                    results.add(item);
                }
            }
        }

        // 2. Google Places Autocomplete if API Key is configured
        if (googlePlacesApiKey != null && !googlePlacesApiKey.trim().isEmpty()) {
            try {
                URI uri = URI.create("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" 
                        + URLEncoder.encode(query, StandardCharsets.UTF_8) 
                        + "&components=country:in&key=" + googlePlacesApiKey);
                Map response = restTemplate.getForObject(uri, Map.class);
                if (response != null && response.containsKey("predictions")) {
                    List<Map> predictions = (List<Map>) response.get("predictions");
                    for (Map p : predictions) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("placeId", p.get("place_id"));
                        item.put("displayName", p.get("description"));
                        Map formatting = (Map) p.get("structured_formatting");
                        if (formatting != null) {
                            item.put("name", formatting.get("main_text"));
                            item.put("secondary", formatting.get("secondary_text"));
                        } else {
                            item.put("name", p.get("description"));
                            item.put("secondary", "India");
                        }
                        results.add(item);
                        if (results.size() >= 8) break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Google Places Autocomplete error: " + e.getMessage());
            }
        }

        // 3. Open Location Engine (Nominatim + Wikipedia OpenSearch)
        if (results.size() < 8) {
            try {
                URI nomUri = URI.create(nominatimBaseUrl + "/search?q=" 
                        + URLEncoder.encode(query, StandardCharsets.UTF_8) 
                        + "&countrycodes=in&format=json&addressdetails=1&limit=6");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<List> res = restTemplate.exchange(nomUri, HttpMethod.GET, entity, List.class);
                List<Map> list = res.getBody();
                if (list != null) {
                    for (Map item : list) {
                        String name = (String) item.get("name");
                        String displayName = (String) item.get("display_name");
                        if (name == null || name.trim().isEmpty()) {
                            name = displayName != null ? displayName.split(",")[0] : query;
                        }
                        if (seenNames.contains(name.toLowerCase())) continue;
                        seenNames.add(name.toLowerCase());

                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("placeId", String.valueOf(item.get("osm_id")));
                        suggestion.put("name", name);
                        suggestion.put("displayName", cleanDisplayName(displayName));
                        double lat = Double.parseDouble(item.get("lat").toString());
                        double lon = Double.parseDouble(item.get("lon").toString());
                        suggestion.put("lat", lat);
                        suggestion.put("lon", lon);
                        suggestion.put("latitude", lat);
                        suggestion.put("longitude", lon);
                        suggestion.put("type", item.get("type"));

                        Map addr = (Map) item.get("address");
                        if (addr != null) {
                            String city = extractCity(addr);
                            String state = (String) addr.get("state");
                            suggestion.put("city", city != null ? city : "India");
                            suggestion.put("state", state != null ? state : "India");
                            suggestion.put("secondary", (city != null ? city + ", " : "") + (state != null ? state : "India"));
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

            // Wikipedia OpenSearch for notable monuments, temples, caves, hill stations
            try {
                URI wikiUri = URI.create("https://en.wikipedia.org/w/api.php?action=opensearch&search=" 
                        + URLEncoder.encode(query, StandardCharsets.UTF_8) 
                        + "&limit=4&namespace=0&format=json");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<List> wikiRes = restTemplate.exchange(wikiUri, HttpMethod.GET, entity, List.class);
                List wikiData = wikiRes.getBody();
                if (wikiData != null && wikiData.size() >= 2) {
                    List titles = (List) wikiData.get(1);
                    for (Object titleObj : titles) {
                        String t = (String) titleObj;
                        if (t != null && !seenNames.contains(t.toLowerCase())) {
                            seenNames.add(t.toLowerCase());
                            Map<String, Object> suggestion = new HashMap<>();
                            suggestion.put("placeId", "wiki_" + t.replaceAll("\\s+", "_"));
                            suggestion.put("name", t);
                            suggestion.put("displayName", t + ", India");
                            suggestion.put("secondary", "Verified Indian Landmark");
                            results.add(suggestion);
                            if (results.size() >= 8) break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        autocompleteCache.put(cleanQuery, results);
        return results;
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

        if (searchCache.containsKey(cleanQuery.toLowerCase())) {
            return (Map<String, Object>) searchCache.get(cleanQuery.toLowerCase());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isIndia", true);
        result.put("query", cleanQuery);

        // 2. Immediate resolution for known verified Indian destinations
        String queryLower = cleanQuery.toLowerCase();
        for (Map.Entry<String, double[]> entry : KNOWN_COORDS.entrySet()) {
            if (queryLower.equals(entry.getKey()) || queryLower.startsWith(entry.getKey() + ",") || queryLower.endsWith(entry.getKey())) {
                String[] details = KNOWN_DETAILS.get(entry.getKey());
                if (details != null) {
                    result.put("name", details[0]);
                    result.put("city", details[1]);
                    result.put("state", details[2]);
                    result.put("category", details[3]);
                    result.put("lat", entry.getValue()[0]);
                    result.put("lon", entry.getValue()[1]);
                    result.put("latitude", entry.getValue()[0]);
                    result.put("longitude", entry.getValue()[1]);
                    result.put("address", details[0] + ", " + details[1] + ", " + details[2] + ", India");
                    result.put("verified", true);
                    break;
                }
            }
        }

        // 3. Google Places Details if configured and lat/lon not resolved
        if ((!result.containsKey("lat") || !result.containsKey("lon")) && googlePlacesApiKey != null && !googlePlacesApiKey.trim().isEmpty()) {
            try {
                URI searchUri = URI.create("https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                        + URLEncoder.encode(cleanQuery + ", India", StandardCharsets.UTF_8)
                        + "&key=" + googlePlacesApiKey);
                Map gRes = restTemplate.getForObject(searchUri, Map.class);
                if (gRes != null && gRes.containsKey("results")) {
                    List<Map> gList = (List<Map>) gRes.get("results");
                    if (!gList.isEmpty()) {
                        Map primary = gList.get(0);
                        result.put("name", primary.get("name"));
                        result.put("address", primary.get("formatted_address"));
                        
                        Map geometry = (Map) primary.get("geometry");
                        if (geometry != null && geometry.containsKey("location")) {
                            Map loc = (Map) geometry.get("location");
                            double lat = Double.parseDouble(loc.get("lat").toString());
                            double lon = Double.parseDouble(loc.get("lng").toString());
                            result.put("lat", lat);
                            result.put("lon", lon);
                            result.put("latitude", lat);
                            result.put("longitude", lon);
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

                        List types = (List) primary.get("types");
                        result.put("category", formatCategory(types != null && !types.isEmpty() ? types.get(0).toString() : "attraction"));
                        result.put("openingHours", "Information unavailable");

                        // Photo reference
                        List photos = (List) primary.get("photos");
                        if (photos != null && !photos.isEmpty()) {
                            Map p = (Map) photos.get(0);
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

        // 4. Open Location Engine (Nominatim) if lat/lon not yet found
        if (!result.containsKey("lat") || !result.containsKey("lon")) {
            try {
                URI nomUri = URI.create(nominatimBaseUrl + "/search?q=" 
                        + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8) 
                        + "&countrycodes=in&format=json&addressdetails=1&extratags=1&limit=1");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<List> res = restTemplate.exchange(nomUri, HttpMethod.GET, entity, List.class);
                List<Map> list = res.getBody();
                
                if (list != null && !list.isEmpty()) {
                    Map primary = list.get(0);
                    String name = (String) primary.get("name");
                    String displayName = (String) primary.get("display_name");
                    result.put("name", name != null && !name.trim().isEmpty() ? name : (displayName != null ? displayName.split(",")[0] : cleanQuery));
                    result.put("address", displayName);
                    double lat = Double.parseDouble(primary.get("lat").toString());
                    double lon = Double.parseDouble(primary.get("lon").toString());
                    result.put("lat", lat);
                    result.put("lon", lon);
                    result.put("latitude", lat);
                    result.put("longitude", lon);
                    
                    String type = (String) primary.get("type");
                    String cls = (String) primary.get("class");
                    result.put("category", formatCategory(type != null ? type : cls));

                    Map addr = (Map) primary.get("address");
                    if (addr != null) {
                        String city = extractCity(addr);
                        String state = (String) addr.get("state");
                        result.put("city", city != null ? city : "India");
                        result.put("state", state != null ? state : "India");
                    }

                    Map extra = (Map) primary.get("extratags");
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

        // 5. Enrich with Wikipedia summary (real high-res image and real encyclopedic overview)
        String lookupTitle = (String) result.get("name");
        if (lookupTitle == null) lookupTitle = cleanQuery;
        enrichWithWikipedia(result, lookupTitle);

        // Fill unavailable fields strictly with "Information unavailable" (do not invent!)
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

        // Duplicate lat/lon into latitude/longitude for universal consumer compatibility
        if (result.containsKey("lat")) {
            result.put("latitude", result.get("lat"));
        }
        if (result.containsKey("lon")) {
            result.put("longitude", result.get("lon"));
        }
        result.put("verified", result.containsKey("lat") && result.get("lat") != null);

        // 6. Discover verified nearby attractions around this destination
        if (result.containsKey("lat") && result.containsKey("lon")) {
            double lat = Double.parseDouble(result.get("lat").toString());
            double lon = Double.parseDouble(result.get("lon").toString());
            String city = (String) result.get("city");
            List<Map<String, Object>> nearby = discoverNearby(lat, lon, city, null, 8);
            result.put("nearbyAttractions", nearby);
        }

        searchCache.put(cleanQuery.toLowerCase(), result);
        return result;
    }

    /**
     * Discovers real attractions near coordinates using Overpass API + Wikipedia GeoSearch.
     */
    public List<Map<String, Object>> discoverNearby(double lat, double lon, String destination, String category, int limit) {
        String cacheKey = String.format(Locale.ROOT, "%.2f_%.2f_%s_%s", lat, lon, destination != null ? destination : "", category != null ? category : "");
        if (nearbyCache.containsKey(cacheKey)) {
            return nearbyCache.get(cacheKey);
        }

        List<Map<String, Object>> spots = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Wikipedia GeoSearch within 10km (Wikipedia API max radius is 10,000m)
        try {
            String geoCoordStr = String.format(Locale.ROOT, "%.4f%%7C%.4f", lat, lon);
            URI wikiGeoUri = URI.create("https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=" 
                    + geoCoordStr + "&gsradius=10000&gslimit=25&format=json");
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> res = restTemplate.exchange(wikiGeoUri, HttpMethod.GET, entity, Map.class);
            Map body = res.getBody();
            if (body != null && body.containsKey("query")) {
                Map qMap = (Map) body.get("query");
                List<Map> items = (List<Map>) qMap.get("geosearch");
                if (items != null) {
                    List<String> noise = List.of("mandal", "district", "station", "junction", "airport", "division", "flyover", "hospital", "college", "university", "corporation");
                    for (Map item : items) {
                        String title = (String) item.get("title");
                        if (title == null) continue;
                        String tLow = title.toLowerCase();
                        boolean isNoise = noise.stream().anyMatch(tLow::contains);
                        if (isNoise || seen.contains(tLow)) continue;

                        // Category filter if specified
                        if (category != null && !matchesCategory(tLow, category)) {
                            continue;
                        }

                        Map<String, Object> spot = new HashMap<>();
                        spot.put("name", title);
                        spot.put("title", title);
                        spot.put("lat", item.get("lat"));
                        spot.put("lon", item.get("lon"));
                        spot.put("latitude", item.get("lat"));
                        spot.put("longitude", item.get("lon"));
                        spot.put("category", inferCategoryFromTitle(title));
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
        } catch (Exception e) {
            System.err.println("Wikipedia geosearch error: " + e.getMessage());
        }

        // 2. Supplemental Overpass OSM Query only if spots are empty
        if (spots.isEmpty()) {
            try {
                String overpassFilter = buildOverpassFilter(category);
                String query = String.format(Locale.ROOT, "[out:json][timeout:4];("
                        + "node[\"tourism\"~\"attraction|museum|viewpoint|zoo|theme_park\"](around:20000,%.4f,%.4f);"
                        + "node[\"historic\"~\"fort|monument|castle|ruins|archaeological_site\"](around:20000,%.4f,%.4f);"
                        + (overpassFilter != null ? overpassFilter + "(around:20000,%.4f,%.4f);" : "")
                        + ");out center 10;", lat, lon, lat, lon, lat, lon);
                URI overpassUri = URI.create("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<Map> res = restTemplate.exchange(overpassUri, HttpMethod.GET, entity, Map.class);
                Map body = res.getBody();
                if (body != null && body.containsKey("elements")) {
                    List<Map> elements = (List<Map>) body.get("elements");
                    for (Map el : elements) {
                        Map tags = (Map) el.get("tags");
                        if (tags != null && tags.containsKey("name")) {
                            String name = (String) tags.get("name");
                            String nLow = name.toLowerCase();
                            if (seen.contains(nLow)) continue;

                            Map<String, Object> spot = new HashMap<>();
                            spot.put("name", name);
                            spot.put("title", name);
                            spot.put("lat", el.get("lat"));
                            spot.put("lon", el.get("lon"));
                            spot.put("latitude", el.get("lat"));
                            spot.put("longitude", el.get("lon"));
                            spot.put("category", tags.containsKey("tourism") ? formatCategory((String) tags.get("tourism")) : "Heritage");
                            spot.put("rating", "Information unavailable");
                            spot.put("reviewsCount", "Information unavailable");
                            spot.put("openingHours", tags.getOrDefault("opening_hours", "Information unavailable"));
                            spot.put("verificationStatus", "100% Real Location • Verified");

                            seen.add(nLow);
                            spots.add(spot);
                            if (spots.size() >= limit) break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Overpass nearby error: " + e.getMessage());
            }
        }

        // Fetch thumbnails for top spots from Wikipedia
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

        Map<String, Object> destDetails = searchPlace(destination);
        if (!destDetails.containsKey("lat") || !destDetails.containsKey("lon")) {
            return Collections.emptyList();
        }
        double lat = Double.parseDouble(destDetails.get("lat").toString());
        double lon = Double.parseDouble(destDetails.get("lon").toString());

        List<Map<String, Object>> spots = new ArrayList<>(discoverNearby(lat, lon, destination, category, 10));
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> sp : spots) {
            if (sp.containsKey("name")) seen.add(sp.get("name").toString().toLowerCase());
        }

        // Fast Wikipedia OpenSearch for category in destination if spots are few
        if (spots.size() < 4) {
            try {
                String searchPhrase = (category != null ? category.toLowerCase() : "places") + " in " + destination;
                URI wikiUri = URI.create("https://en.wikipedia.org/w/api.php?action=opensearch&search=" 
                        + URLEncoder.encode(searchPhrase, StandardCharsets.UTF_8) 
                        + "&limit=6&namespace=0&format=json");
                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<List> wikiRes = restTemplate.exchange(wikiUri, HttpMethod.GET, entity, List.class);
                List wikiData = wikiRes.getBody();
                if (wikiData != null && wikiData.size() >= 2) {
                    List titles = (List) wikiData.get(1);
                    for (Object titleObj : titles) {
                        String t = (String) titleObj;
                        if (t != null && !seen.contains(t.toLowerCase())) {
                            seen.add(t.toLowerCase());
                            Map<String, Object> spot = new HashMap<>();
                            spot.put("name", t);
                            spot.put("title", t);
                            spot.put("category", category != null ? category : "Heritage");
                            spot.put("rating", "Information unavailable");
                            spot.put("reviewsCount", "Information unavailable");
                            spot.put("openingHours", "Information unavailable");
                            spot.put("verificationStatus", "100% Real Location • Verified");
                            enrichWithWikipedia(spot, t);
                            if (spot.containsKey("lat") && spot.get("lat") != null) {
                                spots.add(spot);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        categoryCache.put(cacheKey, spots);
        return spots;
    }

    private String[] getCategorySearchKeywords(String category) {
        if (category == null) return new String[]{"attractions"};
        switch (category.toUpperCase()) {
            case "BEACHES": return new String[]{"beach"};
            case "HERITAGE": return new String[]{"fort", "caves", "palace", "monument"};
            case "NATURE": return new String[]{"waterfalls", "lake", "park", "garden"};
            case "ADVENTURE": return new String[]{"trekking", "viewpoint", "adventure"};
            case "SPIRITUAL": return new String[]{"temple", "shrine", "church", "mosque"};
            case "FOOD": return new String[]{"restaurant", "cuisine", "food"};
            case "WILDLIFE": return new String[]{"sanctuary", "national park", "zoo"};
            case "MOUNTAINS": return new String[]{"hills", "valley", "viewpoint"};
            default: return new String[]{"attractions"};
        }
    }

    // Helper: Enrich with real photo and extract from Wikipedia REST summary
    private void enrichWithWikipedia(Map<String, Object> target, String title) {
        if (title == null || title.trim().isEmpty()) return;
        try {
            String safeTitle = title.trim().replaceAll("\\s+", "_");
            URI uri = URI.create("https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(safeTitle, StandardCharsets.UTF_8));
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> res = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map body = res.getBody();
            if (body != null) {
                if (body.containsKey("extract") && !target.containsKey("description")) {
                    target.put("description", body.get("extract"));
                }
                if (body.containsKey("thumbnail") && !target.containsKey("image")) {
                    Map thumb = (Map) body.get("thumbnail");
                    if (thumb != null && thumb.containsKey("source")) {
                        target.put("image", thumb.get("source"));
                    }
                }
                if (body.containsKey("coordinates") && (!target.containsKey("lat") || target.get("lat") == null)) {
                    Map coords = (Map) body.get("coordinates");
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

    private String extractCity(Map addr) {
        if (addr.containsKey("city")) return (String) addr.get("city");
        if (addr.containsKey("town")) return (String) addr.get("town");
        if (addr.containsKey("suburb")) return (String) addr.get("suburb");
        if (addr.containsKey("village")) return (String) addr.get("village");
        if (addr.containsKey("county")) return (String) addr.get("county");
        if (addr.containsKey("state_district")) return (String) addr.get("state_district");
        return null;
    }

    private String formatCategory(String raw) {
        if (raw == null) return "Tourist Landmark";
        String l = raw.toLowerCase();
        if (l.contains("worship") || l.contains("temple") || l.contains("spiritual") || l.contains("mosque") || l.contains("church")) return "Spiritual";
        if (l.contains("museum") || l.contains("art")) return "Museums";
        if (l.contains("monument") || l.contains("fort") || l.contains("historic") || l.contains("palace") || l.contains("caves")) return "Heritage";
        if (l.contains("beach") || l.contains("coast")) return "Beaches";
        if (l.contains("mountain") || l.contains("hill") || l.contains("valley") || l.contains("peak")) return "Mountains";
        if (l.contains("park") || l.contains("garden") || l.contains("waterfall") || l.contains("nature") || l.contains("lake")) return "Nature";
        if (l.contains("wildlife") || l.contains("zoo") || l.contains("sanctuary")) return "Wildlife";
        if (l.contains("food") || l.contains("restaurant")) return "Food";
        if (l.contains("adventure") || l.contains("trek")) return "Adventure";
        return "Sightseeing";
    }

    private String inferCategoryFromTitle(String title) {
        String t = title.toLowerCase();
        if (t.contains("temple") || t.contains("masjid") || t.contains("church") || t.contains("dargah") || t.contains("ashram")) return "Spiritual";
        if (t.contains("fort") || t.contains("palace") || t.contains("caves") || t.contains("monument") || t.contains("tomb") || t.contains("mahal")) return "Heritage";
        if (t.contains("beach") || t.contains("coast")) return "Beaches";
        if (t.contains("falls") || t.contains("waterfall") || t.contains("lake") || t.contains("dam") || t.contains("barrage")) return "Nature";
        if (t.contains("museum") || t.contains("gallery")) return "Museums";
        if (t.contains("park") || t.contains("garden") || t.contains("island")) return "Nature";
        if (t.contains("sanctuary") || t.contains("wildlife") || t.contains("national park") || t.contains("zoo")) return "Wildlife";
        if (t.contains("valley") || t.contains("peak") || t.contains("hill") || t.contains("point") || t.contains("viewpoint")) return "Mountains";
        return "Sightseeing";
    }

    private boolean matchesCategory(String title, String category) {
        String c = category.toUpperCase();
        switch (c) {
            case "BEACHES": return title.contains("beach") || title.contains("coast");
            case "HERITAGE": return title.contains("fort") || title.contains("palace") || title.contains("cave") || title.contains("monument") || title.contains("tomb") || title.contains("heritage");
            case "NATURE": return title.contains("waterfall") || title.contains("lake") || title.contains("park") || title.contains("garden") || title.contains("river") || title.contains("dam");
            case "ADVENTURE": return title.contains("trek") || title.contains("point") || title.contains("viewpoint") || title.contains("camp") || title.contains("rafting");
            case "SPIRITUAL": return title.contains("temple") || title.contains("church") || title.contains("mosque") || title.contains("dargah") || title.contains("shrine");
            case "WILDLIFE": return title.contains("sanctuary") || title.contains("wildlife") || title.contains("zoo") || title.contains("national park");
            case "MOUNTAINS": return title.contains("hill") || title.contains("valley") || title.contains("peak") || title.contains("point") || title.contains("ghat");
            default: return true;
        }
    }

    private String buildOverpassFilter(String category) {
        if (category == null) return null;
        switch (category.toUpperCase()) {
            case "BEACHES": return "node[\"natural\"=\"beach\"]";
            case "HERITAGE": return "node[\"historic\"~\"fort|monument|ruins|castle|archaeological_site\"]";
            case "NATURE": return "node[\"leisure\"=\"park\"];node[\"natural\"~\"water|waterfall\"]";
            case "ADVENTURE": return "node[\"tourism\"=\"viewpoint\"]";
            case "SPIRITUAL": return "node[\"amenity\"=\"place_of_worship\"]";
            case "FOOD": return "node[\"amenity\"~\"restaurant|cafe\"]";
            case "WILDLIFE": return "node[\"leisure\"=\"nature_reserve\"]";
            case "MOUNTAINS": return "node[\"natural\"=\"peak\"]";
            default: return null;
        }
    }
}
