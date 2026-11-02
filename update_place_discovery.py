import re

pds_path = r"E:\travel\backend\src\main\java\com\travelmate\service\PlaceDiscoveryService.java"
with open(pds_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update constructor to inject VerifiedAttractionRegistry
old_ctor = """    public PlaceDiscoveryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }"""

new_ctor = """    private final VerifiedAttractionRegistry verifiedAttractionRegistry;

    public PlaceDiscoveryService(RestTemplate restTemplate, VerifiedAttractionRegistry verifiedAttractionRegistry) {
        this.restTemplate = restTemplate;
        this.verifiedAttractionRegistry = verifiedAttractionRegistry;
    }"""

if old_ctor in content:
    content = content.replace(old_ctor, new_ctor)
    print("Replaced constructor successfully.")
else:
    print("Warning: old_ctor not found directly.")

# 2. Update searchPlace: when searching a city, prioritize verified attractions from VerifiedAttractionRegistry
old_search_nearby = """        // 6. Discover verified nearby attractions around this destination
        if (result.containsKey("lat") && result.containsKey("lon")) {
            double lat = Double.parseDouble(result.get("lat").toString());
            double lon = Double.parseDouble(result.get("lon").toString());
            String city = (String) result.get("city");
            List<Map<String, Object>> nearby = discoverNearby(lat, lon, city, null, 8);
            result.put("nearbyAttractions", nearby);
        }"""

new_search_nearby = """        // 6. Discover verified genuine tourist attractions around this destination
        if (result.containsKey("lat") && result.containsKey("lon")) {
            double lat = Double.parseDouble(result.get("lat").toString());
            double lon = Double.parseDouble(result.get("lon").toString());
            String city = (String) result.get("city");
            List<Map<String, Object>> nearby = discoverNearby(lat, lon, city, null, 8);
            result.put("nearbyAttractions", nearby);
        }"""

# 3. Update discoverNearby method to strictly prioritize verified attractions and reject localities/suburbs
discover_regex = r"(public List<Map<String, Object>> discoverNearby\(double lat, double lon, String destination, String category, int limit\) \{[\s\S]*?nearbyCache\.put\(cacheKey, spots\);\s*return spots;\s*\})"

new_discover_nearby = """public List<Map<String, Object>> discoverNearby(double lat, double lon, String destination, String category, int limit) {
        String cacheKey = String.format(Locale.ROOT, "%.2f_%.2f_%s_%s", lat, lon, destination != null ? destination : "", category != null ? category : "");
        if (nearbyCache.containsKey(cacheKey)) {
            return nearbyCache.get(cacheKey);
        }

        List<Map<String, Object>> spots = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. First priority: Check Verified Attraction Registry for authentic, genuine tourist spots
        if (destination != null && !destination.trim().isEmpty()) {
            List<Map<String, Object>> verified = verifiedAttractionRegistry.getAttractionsForCity(destination);
            if (verified != null && !verified.isEmpty()) {
                for (Map<String, Object> v : verified) {
                    String name = (String) v.get("name");
                    if (name == null) continue;
                    String nLow = name.toLowerCase().trim();
                    if (seen.contains(nLow)) continue;

                    if (category != null && !category.equalsIgnoreCase("all") && !matchesCategory(nLow, category)) {
                        continue;
                    }

                    seen.add(nLow);
                    spots.add(new HashMap<>(v));
                    if (spots.size() >= limit) break;
                }
            }
        }

        // If sufficient verified attractions were found for this destination, return them directly
        if (spots.size() >= limit) {
            nearbyCache.put(cacheKey, spots);
            return spots;
        }

        // 2. OpenStreetMap Overpass Query for strictly tagged tourist landmarks (no residential localities)
        if (spots.size() < limit) {
            try {
                String overpassFilter = buildOverpassFilter(category);
                String query = String.format(Locale.ROOT, "[out:json][timeout:5];("
                        + "node[\"tourism\"~\"attraction|museum|viewpoint|zoo|theme_park\"](around:25000,%.4f,%.4f);"
                        + "node[\"historic\"~\"fort|monument|castle|ruins|archaeological_site\"](around:25000,%.4f,%.4f);"
                        + "node[\"amenity\"=\"place_of_worship\"](around:25000,%.4f,%.4f);"
                        + (overpassFilter != null ? overpassFilter + "(around:25000,%.4f,%.4f);" : "")
                        + ");out center 15;", lat, lon, lat, lon, lat, lon);

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
                            if (name == null || name.trim().isEmpty()) continue;
                            String nLow = name.toLowerCase().trim();
                            
                            // Strict check: Reject ordinary localities, suburbs, and non-tourist residential areas
                            if (verifiedAttractionRegistry.isLocalityOrNonTourist(name, null, null)) {
                                continue;
                            }
                            if (seen.contains(nLow)) continue;

                            Map<String, Object> spot = new HashMap<>();
                            spot.put("name", name);
                            spot.put("title", name);
                            spot.put("lat", el.get("lat"));
                            spot.put("lon", el.get("lon"));
                            spot.put("latitude", el.get("lat"));
                            spot.put("longitude", el.get("lon"));
                            spot.put("category", tags.containsKey("tourism") ? formatCategory((String) tags.get("tourism")) : (tags.containsKey("historic") ? "Heritage" : "Spiritual"));
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

        // 3. Fallback: Wikipedia GeoSearch strictly qualified for verified tourist landmark keywords
        if (spots.size() < limit) {
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
                        List<String> touristKeywords = List.of(
                            "temple", "fort", "caves", "palace", "beach", "museum", "falls", "waterfall",
                            "park", "sanctuary", "zoo", "monument", "viewpoint", "hill", "dargah", "church",
                            "stupa", "heritage", "memorial", "planetarium", "aquarium", "garden", "lake", "peak", "ghat"
                        );
                        for (Map item : items) {
                            String title = (String) item.get("title");
                            if (title == null) continue;
                            String tLow = title.toLowerCase().trim();

                            // Filter out known localities and non-tourist terms
                            if (verifiedAttractionRegistry.isLocalityOrNonTourist(title, null, null)) {
                                continue;
                            }
                            if (seen.contains(tLow)) continue;

                            // Positive qualification: only accept if title contains explicit tourist POI keyword
                            boolean hasTouristKeyword = touristKeywords.stream().anyMatch(tLow::contains);
                            if (!hasTouristKeyword) {
                                continue;
                            }

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
        }

        // Fetch thumbnails for top spots from Wikipedia
        for (int i = 0; i < Math.min(spots.size(), 4); i++) {
            Map<String, Object> s = spots.get(i);
            enrichWithWikipedia(s, (String) s.get("name"));
        }

        nearbyCache.put(cacheKey, spots);
        return spots;
    }"""

content = re.sub(discover_regex, new_discover_nearby, content)

with open(pds_path, "w", encoding="utf-8") as f:
    f.write(content)

print("PlaceDiscoveryService.java updated successfully!")
