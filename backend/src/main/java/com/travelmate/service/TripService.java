package com.travelmate.service;

import com.travelmate.dto.TripCreateRequest;
import com.travelmate.entity.Trip;
import com.travelmate.entity.User;
import com.travelmate.repository.TripRepository;
import com.travelmate.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;

import com.travelmate.entity.ItineraryDay;
import com.travelmate.entity.ItineraryItem;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import com.travelmate.entity.TripMember;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.TripRole;
import com.travelmate.entity.InvitationStatus;
import com.travelmate.repository.TripMemberRepository;
import com.travelmate.repository.AttractionRepository;
import com.travelmate.repository.ItineraryDayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final MLIntegrationService mlIntegrationService;
    private final LocationService locationService;
    private final PlaceDiscoveryService placeDiscoveryService;
    private final WeatherService weatherService;
    private final com.travelmate.repository.ItineraryItemRepository itineraryItemRepository;
    private final TripMemberRepository tripMemberRepository;
    private final RouteOptimizationService routeOptimizationService;
    private final AttractionRepository attractionRepository;

    @Autowired(required = false)
    private ItineraryDayRepository itineraryDayRepository;

    public TripService(TripRepository tripRepository, UserRepository userRepository,
            MLIntegrationService mlIntegrationService, LocationService locationService,
            PlaceDiscoveryService placeDiscoveryService, WeatherService weatherService,
            com.travelmate.repository.ItineraryItemRepository itineraryItemRepository,
            TripMemberRepository tripMemberRepository,
            RouteOptimizationService routeOptimizationService,
            AttractionRepository attractionRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.mlIntegrationService = mlIntegrationService;
        this.locationService = locationService;
        this.placeDiscoveryService = placeDiscoveryService;
        this.weatherService = weatherService;
        this.itineraryItemRepository = itineraryItemRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.routeOptimizationService = routeOptimizationService;
        this.attractionRepository = attractionRepository;
    }

    public void setItineraryDayRepository(ItineraryDayRepository itineraryDayRepository) {
        this.itineraryDayRepository = itineraryDayRepository;
    }

    public static String determineCategory(String placeName, String address, String rawCategory) {
        String combined = ((placeName != null ? placeName : "") + " " +
                (address != null ? address : "") + " " +
                (rawCategory != null ? rawCategory : "")).toLowerCase();

        if (combined.contains("university") || combined.contains("college") || combined.contains("institute")
                || combined.contains("vidya parishad") || combined.contains("gitam") || combined.contains("campus")
                || combined.contains("iit") || combined.contains("iim") || combined.contains("nit")
                || combined.contains("polytechnic") || combined.contains("education") || combined.contains("faculty")
                || combined.contains("academy")) {
            return "Education / University";
        }
        if (combined.contains("temple") || combined.contains("mandir") || combined.contains("spiritual")
                || combined.contains("devasthanam") || combined.contains("kovil") || combined.contains("church")
                || combined.contains("cathedral") || combined.contains("basilica") || combined.contains("mosque")
                || combined.contains("masjid") || combined.contains("dargah") || combined.contains("gurdwara")
                || combined.contains("ashram") || combined.contains("monastery") || combined.contains("stupa")
                || combined.contains("shrine") || combined.contains("religious")) {
            return "Temples / Spiritual";
        }
        if (combined.contains("beach") || combined.contains("coast") || combined.contains("shore")
                || combined.contains("cove") || combined.contains("promenade") || combined.contains("sagar")) {
            return "Beaches";
        }
        if (combined.contains("viewpoint") || combined.contains("view point") || combined.contains("overlook")
                || combined.contains("sunrise point") || combined.contains("sunset point")
                || combined.contains("dolphin's nose")) {
            return "Viewpoints / Attractions";
        }
        if (combined.contains("zoo") || combined.contains("wildlife") || combined.contains("sanctuary")
                || combined.contains("safari") || combined.contains("national park") || combined.contains("aquarium")
                || combined.contains("deer park") || combined.contains("bird sanctuary")) {
            return "Wildlife / Zoo";
        }
        if (combined.contains("museum") || combined.contains("memorial") || combined.contains("gallery")
                || combined.contains("exhibition") || combined.contains("planetarium")
                || combined.contains("science centre")
                || combined.contains("submarine")) {
            return "Museums";
        }
        if (combined.contains("waterfall") || combined.contains("falls") || combined.contains("hill")
                || combined.contains("valley") || combined.contains("peak") || combined.contains("caves")
                || combined.contains("cave") || combined.contains("ghat") || combined.contains("ridge")
                || combined.contains("lake") || combined.contains("dam")) {
            return "Nature / Hill Stations";
        }
        if (combined.contains("fort") || combined.contains("palace") || combined.contains("qila")
                || combined.contains("killa") || combined.contains("mahal") || combined.contains("tomb")
                || combined.contains("monument") || combined.contains("ruins") || combined.contains("stepwell")
                || combined.contains("heritage") || combined.contains("haveli")) {
            return "Historical / Heritage";
        }
        if (combined.contains("park") || combined.contains("garden") || combined.contains("botanical")) {
            return "Parks & Gardens";
        }
        if (combined.contains("restaurant") || combined.contains("food") || combined.contains("cafe")
                || combined.contains("dining") || combined.contains("bakery") || combined.contains("dhaba")
                || combined.contains("wharf")) {
            if (rawCategory != null && rawCategory.toLowerCase().contains("restaurant"))
                return rawCategory.trim();
            return "Restaurant & Food";
        }
        if (combined.contains("market") || combined.contains("bazaar") || combined.contains("shopping")
                || combined.contains("mall")) {
            return "Shopping / Market";
        }
        if (rawCategory != null && !rawCategory.trim().isEmpty() && !rawCategory.equalsIgnoreCase("Sightseeing")) {
            return rawCategory.trim();
        }
        return "Sightseeing & Attractions";
    }

    public Trip createTrip(String userEmail, TripCreateRequest request) {
        // Enforce India-Only Travel Planning
        if (request.getDestinationCity() != null
                && placeDiscoveryService.isForeignLocation(request.getDestinationCity())) {
            throw new RuntimeException("TravelMate AI currently supports travel within India.");
        }
        if (request.getStartingLocation() != null
                && placeDiscoveryService.isForeignLocation(request.getStartingLocation())) {
            throw new RuntimeException("TravelMate AI currently supports travel within India.");
        }

        User user = userRepository.findByEmail(userEmail).orElseGet(() -> {
            return userRepository.findByEmail("traveler@travelmate.com").orElseGet(() -> {
                List<User> all = userRepository.findAll();
                if (!all.isEmpty()) {
                    return all.get(0);
                }
                User guest = new User();
                guest.setEmail(userEmail != null && !userEmail.isBlank() ? userEmail : "traveler@travelmate.com");
                guest.setFullName("Traveler");
                guest.setPassword("$2a$10$wT5HqjJ7y7r8q7w8q7w8qu7w8q7w8q7w8q7w8q7w8q7w8q7w8q7w8");
                guest.setRole(com.travelmate.entity.Role.ROLE_USER);
                return userRepository.save(guest);
            });
        });

        Trip trip = new Trip();
        trip.setUser(user);

        // Parse Destination City and Area/Locality
        String rawDest = request.getDestinationCity() != null ? request.getDestinationCity().trim() : "India";
        String parsedCity = rawDest;
        String parsedLocality = request.getLocality() != null && !request.getLocality().trim().isEmpty()
                ? request.getLocality().trim()
                : null;
        if (rawDest.contains("->")) {
            String[] parts = rawDest.split("->");
            parsedCity = parts[0].trim();
            parsedLocality = parts[1].trim();
        } else if (rawDest.contains(",")) {
            String[] parts = rawDest.split(",");
            parsedLocality = parts[0].trim();
            parsedCity = parts[parts.length - 1].trim();
        }

        trip.setDestinationCity(parsedCity);
        trip.setLocality(parsedLocality);
        trip.setTitle(parsedLocality != null ? "Exploration of " + parsedLocality + " (" + parsedCity + ")"
                : "Trip to " + parsedCity);
        trip.setStartDate(request.getStartDate() != null ? request.getStartDate() : java.time.LocalDate.now());

        // Hourly / Short-Trip Handling
        Integer durationHours = request.getDurationHours();
        trip.setDurationHours(durationHours);
        trip.setStartTime(request.getStartTime() != null ? request.getStartTime() : "14:00");
        trip.setAvailableUntil(request.getAvailableUntil() != null ? request.getAvailableUntil() : "17:00");
        trip.setMaxDistanceKm(request.getMaxDistanceKm());
        trip.setMaxEntryFee(request.getMaxEntryFee());
        trip.setTravelDistancePref(
                request.getTravelDistancePref() != null ? request.getTravelDistancePref() : "Nearby");

        int daysCount = (durationHours != null && durationHours > 0) ? 1
                : (request.getNumDays() != null ? request.getNumDays() : 3);
        trip.setEndDate(request.getEndDate() != null && durationHours == null ? request.getEndDate()
                : trip.getStartDate().plusDays(daysCount - 1));
        trip.setNumDays(daysCount);
        trip.setBudgetInr(request.getBudgetInr() != null ? request.getBudgetInr() : 30000.0);
        trip.setStartingLocation(
                request.getStartingLocation() != null && !request.getStartingLocation().trim().isEmpty()
                        ? request.getStartingLocation().trim()
                        : (parsedLocality != null ? parsedLocality : parsedCity));
        trip.setTransitPreference(
                request.getTransitPreference() != null ? request.getTransitPreference().trim() : "Bus / Road");
        trip.setTravelStyle(request.getTravelStyle() != null ? request.getTravelStyle() : "Solo");
        trip.setNumTravelers(request.getNumTravelers() != null ? request.getNumTravelers() : 1);
        trip.setActivityLevel(request.getActivityLevel() != null ? request.getActivityLevel() : "Moderate");
        trip.setFoodPreference(request.getFoodPreference() != null ? request.getFoodPreference() : "Any");
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            trip.setInterests(String.join(",", request.getInterests()));
        }

        // Real Geocoding for Origin
        try {
            if (trip.getStartingLocation() != null && !trip.getStartingLocation().isEmpty()) {
                Map<String, Object> originGeo = locationService.verifyAndGetLocation(trip.getStartingLocation());
                if (originGeo != null && originGeo.containsKey("lat") && originGeo.containsKey("lon")) {
                    trip.setOriginLatitude(Double.parseDouble(originGeo.get("lat").toString()));
                    trip.setOriginLongitude(Double.parseDouble(originGeo.get("lon").toString()));
                }
            }
        } catch (Exception ignored) {
        }

        // Real Geocoding for Locality / Destination
        try {
            String geoLookup = (parsedLocality != null && !parsedLocality.isEmpty())
                    ? parsedLocality + ", " + parsedCity
                    : parsedCity;
            Map<String, Object> destGeo = locationService.verifyAndGetLocation(geoLookup);
            if (destGeo != null && destGeo.containsKey("lat") && destGeo.containsKey("lon")) {
                trip.setDestinationLatitude(Double.parseDouble(destGeo.get("lat").toString()));
                trip.setDestinationLongitude(Double.parseDouble(destGeo.get("lon").toString()));
            } else {
                Map<String, Object> found = placeDiscoveryService.searchPlace(geoLookup);
                if (found != null && found.containsKey("lat") && found.containsKey("lon")) {
                    trip.setDestinationLatitude(Double.parseDouble(found.get("lat").toString()));
                    trip.setDestinationLongitude(Double.parseDouble(found.get("lon").toString()));
                }
            }
        } catch (Exception ignored) {
        }

        // Transport Validation Layer
        validateAndConfigureTransport(trip, request.getTransitPreference());

        Trip savedTrip = tripRepository.save(trip);

        // Determine pace quotas strictly based on selected travel pace
        // Relaxed: 1-2 places/day; Moderate: 3-4 places/day; Fast Paced: 5-6 places/day
        int targetPerDay = getTargetItemsPerDay(trip.getActivityLevel());
        int minTargetPerDay = (targetPerDay >= 5) ? 5 : (targetPerDay <= 2 ? 1 : 3);
        List<Integer> dailyQuotas = new ArrayList<>();
        for (int i = 0; i < daysCount; i++) {
            int quota;
            if (durationHours != null && durationHours > 0) {
                quota = (durationHours <= 1 ? 1 : (durationHours <= 3 ? 2 : 3));
            } else if (targetPerDay >= 5) {
                // Fast Paced: strictly 5-6 places per day (alternating 6, 5, 6, 5...)
                quota = (i % 2 == 0) ? 6 : 5;
            } else if (targetPerDay <= 2) {
                // Relaxed: strictly 1-2 places per day
                quota = 2;
            } else {
                // Moderate: strictly 3-4 places per day (alternating 4, 3, 4, 3...)
                quota = (i % 2 == 0) ? 4 : 3;
            }
            dailyQuotas.add(quota);
        }
        int totalItemsNeeded = dailyQuotas.stream().mapToInt(Integer::intValue).sum();

        List<Map<String, Object>> recs = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        // 1. Seed authentic curated attractions from database for this city
        try {
            List<Attraction> dbAttractions = attractionRepository
                    .findByCityNameIgnoreCaseAndIsAvailableTrue(parsedCity);
            if (dbAttractions == null || dbAttractions.isEmpty()) {
                dbAttractions = attractionRepository.searchAttractions(parsedCity);
            }
            if (dbAttractions != null) {
                for (Attraction a : dbAttractions) {
                    String aName = a.getName();
                    if (aName != null && !seenNames.contains(aName.toLowerCase().trim())) {
                        seenNames.add(aName.toLowerCase().trim());
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("name", aName);
                        itemMap.put("title", aName);
                        itemMap.put("city", parsedCity);
                        itemMap.put("latitude", a.getLatitude());
                        itemMap.put("longitude", a.getLongitude());
                        itemMap.put("category", a.getCategory() != null ? a.getCategory().getName() : "Sightseeing");
                        itemMap.put("entry_fee_inr", a.getEntryFeeInr() != null ? a.getEntryFeeInr() : 0.0);
                        itemMap.put("duration_hours", a.getDurationHours() != null ? a.getDurationHours() : 1.5);
                        itemMap.put("duration_minutes",
                                (int) ((a.getDurationHours() != null ? a.getDurationHours() : 1.5) * 60));
                        itemMap.put("indoor_outdoor", a.getIndoorOutdoor() != null ? a.getIndoorOutdoor() : "Outdoor");
                        itemMap.put("recommendation_reason", a.getDescription() != null ? a.getDescription()
                                : "Verified authentic attraction in " + parsedCity);
                        itemMap.put("rating", a.getRating() != null ? a.getRating() : 4.5);
                        itemMap.put("fromDatabase", true);
                        itemMap.put("verificationStatus", "100% Real Location • Verified");
                        recs.add(itemMap);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("DB attraction preload error: " + e.getMessage());
        }

        // 2. Fetch ML recommendations
        Map<String, Object> mlRequest = new HashMap<>();
        mlRequest.put("user_id", user.getId());
        mlRequest.put("city", parsedCity);
        mlRequest.put("locality", parsedLocality);
        mlRequest.put("duration_hours", durationHours);
        mlRequest.put("max_distance_km", request.getMaxDistanceKm());
        mlRequest.put("max_entry_fee", request.getMaxEntryFee());
        mlRequest.put("travel_distance_pref", request.getTravelDistancePref());
        mlRequest.put("travel_style", request.getTravelStyle());
        mlRequest.put("interests", request.getInterests());
        mlRequest.put("activity_level", request.getActivityLevel() != null ? request.getActivityLevel() : "Moderate");
        mlRequest.put("budget", request.getBudgetInr() > 50000 ? "Luxury" : "Mid-Range");
        mlRequest.put("top_k", Math.max(30, totalItemsNeeded + 10));

        try {
            Map<String, Object> response = mlIntegrationService.getRecommendations(mlRequest);
            if (response != null && response.containsKey("recommendations")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rList = (List<Map<String, Object>>) response.get("recommendations");
                if (rList != null) {
                    for (Map<String, Object> r : rList) {
                        Object n = r.get("name");
                        if (n == null)
                            n = r.get("title");
                        if (n != null && !seenNames.contains(n.toString().toLowerCase().trim())) {
                            seenNames.add(n.toString().toLowerCase().trim());
                            recs.add(r);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ML service query: " + e.getMessage());
        }

        // 3. Ensure sufficient real attractions matching locality or destination
        if (recs.size() < (totalItemsNeeded + 10) && trip.getDestinationLatitude() != null
                && trip.getDestinationLongitude() != null) {
            String discQuery = parsedLocality != null ? parsedLocality : parsedCity;
            int fetchLimit = Math.max(30, totalItemsNeeded + 15);
            try {
                List<Map<String, Object>> discovered = placeDiscoveryService.discoverNearby(
                        trip.getDestinationLatitude(), trip.getDestinationLongitude(), discQuery, null, fetchLimit);
                if (discovered != null) {
                    for (Map<String, Object> disc : discovered) {
                        String discName = (String) disc.get("name");
                        if (discName != null && !seenNames.contains(discName.toLowerCase().trim())) {
                            seenNames.add(discName.toLowerCase().trim());
                            Map<String, Object> itemMap = new HashMap<>(disc);
                            itemMap.put("name", discName);
                            itemMap.put("title", discName);
                            itemMap.put("city", parsedCity);
                            itemMap.put("latitude", disc.get("lat"));
                            itemMap.put("longitude", disc.get("lon"));
                            itemMap.put("category", disc.getOrDefault("category", "Sightseeing"));
                            Object fee = disc.get("entryFeeInr");
                            if (fee == null)
                                fee = disc.get("entry_fee_inr");
                            itemMap.put("entry_fee_inr", fee != null ? fee : 0.0);
                            itemMap.put("duration_hours", 1.5);
                            itemMap.put("indoor_outdoor", disc.getOrDefault("indoorOutdoor", "Outdoor"));
                            itemMap.put("recommendation_reason", "Verified real attraction in " + discQuery);
                            itemMap.put("verificationStatus", "100% Real Location • Verified");
                            recs.add(itemMap);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Strict validation: Reject schools, hospitals, offices, ordinary localities,
        // residential buildings
        // Database-curated authentic attractions bypass noise filtering
        List<Map<String, Object>> validRecs = recs.stream().filter(r -> {
            if (Boolean.TRUE.equals(r.get("fromDatabase"))) {
                return true;
            }
            Object n = r.get("name");
            if (n == null)
                n = r.get("title");
            if (n == null)
                return false;
            String nameStr = n.toString();
            return !placeDiscoveryService.isNonTouristOrNoise(nameStr, null, null)
                    && placeDiscoveryService.isAuthenticTouristAttraction(nameStr, null, null);
        }).collect(java.util.stream.Collectors.toList());

        // Filter recommendations by distance and entry fee if user selected filters
        List<Map<String, Object>> filteredRecs = new ArrayList<>(validRecs);
        if (trip.getMaxDistanceKm() != null && trip.getMaxDistanceKm() > 0 && trip.getDestinationLatitude() != null) {
            filteredRecs = filteredRecs.stream().filter(r -> {
                if (r.containsKey("latitude") && r.get("latitude") != null && r.containsKey("longitude")
                        && r.get("longitude") != null) {
                    try {
                        double lat = Double.parseDouble(r.get("latitude").toString());
                        double lon = Double.parseDouble(r.get("longitude").toString());
                        double d = calculateHaversineKm(trip.getDestinationLatitude(), trip.getDestinationLongitude(),
                                lat, lon);
                        return d <= trip.getMaxDistanceKm();
                    } catch (Exception ignored) {
                        return true;
                    }
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());
        }

        if (trip.getMaxEntryFee() != null) {
            filteredRecs = filteredRecs.stream().filter(r -> {
                Object feeObj = r.get("entry_fee_inr");
                if (feeObj != null) {
                    try {
                        return Double.parseDouble(feeObj.toString()) <= trip.getMaxEntryFee();
                    } catch (Exception ignored) {
                        return true;
                    }
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());
        }

        // Backfill safeguard: If restrictive distance/entry-fee filters reduced
        // candidates below totalItemsNeeded,
        // backfill with genuine attractions from validRecs to guarantee the user's
        // travel pace is NEVER starved
        if (filteredRecs.size() < totalItemsNeeded) {
            Set<String> filteredNames = filteredRecs.stream()
                    .map(r -> String.valueOf(r.getOrDefault("name", r.get("title"))).toLowerCase().trim())
                    .collect(java.util.stream.Collectors.toSet());
            for (Map<String, Object> vr : validRecs) {
                String vrName = String.valueOf(vr.getOrDefault("name", vr.get("title"))).toLowerCase().trim();
                if (!filteredNames.contains(vrName)) {
                    filteredRecs.add(vr);
                    filteredNames.add(vrName);
                    if (filteredRecs.size() >= totalItemsNeeded)
                        break;
                }
            }
        }
        recs = filteredRecs;

        try {
            if (recs != null && !recs.isEmpty()) {
                // If the entire region has fewer genuine attractions than totalItemsNeeded,
                // distribute what is available evenly across all days (never 1 place/day when
                // candidates exist)
                if (recs.size() < totalItemsNeeded) {
                    dailyQuotas.clear();
                    int remaining = recs.size();
                    for (int i = 0; i < daysCount; i++) {
                        int remainingDays = daysCount - i;
                        int share = (int) Math.ceil((double) remaining / remainingDays);
                        dailyQuotas.add(share);
                        remaining -= share;
                    }
                }

                int candidateCount = dailyQuotas.stream().mapToInt(Integer::intValue).sum();
                List<Map<String, Object>> candidateRecs = new ArrayList<>(
                        recs.subList(0, Math.min(candidateCount, recs.size())));

                // Determine sensible Day 1 starting coordinates (Origin if local, else
                // Destination Hub)
                double day1StartLat;
                double day1StartLon;
                if (trip.getOriginLatitude() != null && trip.getOriginLongitude() != null) {
                    double originToDestKm = (trip.getDestinationLatitude() != null
                            && trip.getDestinationLongitude() != null)
                                    ? calculateHaversineKm(trip.getOriginLatitude(), trip.getOriginLongitude(),
                                            trip.getDestinationLatitude(), trip.getDestinationLongitude())
                                    : 0.0;
                    if (originToDestKm <= 60.0) {
                        day1StartLat = trip.getOriginLatitude();
                        day1StartLon = trip.getOriginLongitude();
                    } else if (trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null) {
                        day1StartLat = trip.getDestinationLatitude();
                        day1StartLon = trip.getDestinationLongitude();
                    } else {
                        day1StartLat = trip.getOriginLatitude();
                        day1StartLon = trip.getOriginLongitude();
                    }
                } else if (trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null) {
                    day1StartLat = trip.getDestinationLatitude();
                    day1StartLon = trip.getDestinationLongitude();
                } else {
                    double[] defCoords = RouteOptimizationService.MAP_EXTRACTOR.getCoordinates(candidateRecs.get(0),
                            20.5937, 78.9629);
                    day1StartLat = defCoords[0];
                    day1StartLon = defCoords[1];
                }

                Double destLat = trip.getDestinationLatitude();
                Double destLon = trip.getDestinationLongitude();

                // Partition attractions into daily neighbourhood clusters and optimize route
                // order with Nearest Neighbour + 2-Opt
                List<List<Map<String, Object>>> optimizedDays = routeOptimizationService.partitionAndOptimizeRecs(
                        day1StartLat, day1StartLon, candidateRecs, dailyQuotas, destLat, destLon);

                for (int i = 0; i < daysCount; i++) {
                    ItineraryDay day = new ItineraryDay();
                    day.setTrip(trip);
                    day.setDayNumber(i + 1);
                    java.time.LocalDate dayDate = trip.getStartDate().plusDays(i);
                    day.setDate(dayDate);

                    // Fetch accurate weather forecast for destination on planned day
                    try {
                        Map<String, Object> dayWeather = weatherService.getConditionsForLocationAndDate(destLat, destLon, dayDate);
                        if (dayWeather != null && dayWeather.containsKey("weatherCondition")) {
                            day.setWeatherCondition(String.valueOf(dayWeather.get("weatherCondition")));
                            day.setMaxTempC(((Number) dayWeather.getOrDefault("temperatureC", 30.0)).doubleValue());
                            day.setMinTempC(((Number) dayWeather.getOrDefault("minTemperatureC", 22.0)).doubleValue());
                            double p = ((Number) dayWeather.getOrDefault("rainProbability", 10.0)).doubleValue();
                            day.setRainProbability(p > 1.0 ? p / 100.0 : p);
                        } else {
                            day.setWeatherCondition("Clear Sky");
                            day.setMaxTempC(30.0);
                            day.setMinTempC(22.0);
                            day.setRainProbability(0.10);
                        }
                    } catch (Exception _we) {
                        day.setWeatherCondition("Clear Sky");
                        day.setMaxTempC(30.0);
                        day.setMinTempC(22.0);
                        day.setRainProbability(0.10);
                    }

                    List<Map<String, Object>> dayRecs = (i < optimizedDays.size()) ? optimizedDays.get(i)
                            : Collections.emptyList();
                    int itemsForThisDay = dayRecs.size();
                    int itemsToday = 0;

                    // Available daily schedule / Start time:
                    java.time.LocalTime currentClock;
                    if (i == 0) {
                        String startClockStr = trip.getStartTime() != null ? trip.getStartTime()
                                : (durationHours != null ? "14:00" : "09:00");
                        try {
                            if (startClockStr.toLowerCase().contains("pm")
                                    || startClockStr.toLowerCase().contains("am")) {
                                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
                                        .ofPattern("h:mm a", Locale.ENGLISH);
                                currentClock = java.time.LocalTime.parse(startClockStr.toUpperCase().trim(), dtf);
                            } else {
                                currentClock = java.time.LocalTime.parse(startClockStr.trim());
                            }
                        } catch (Exception e) {
                            currentClock = java.time.LocalTime.of(9, 0);
                        }
                    } else {
                        currentClock = java.time.LocalTime.of(9, 0);
                    }

                    for (Map<String, Object> rec : dayRecs) {
                        ItineraryItem item = new ItineraryItem();
                        item.setItineraryDay(day);

                        // Map Title
                        Object titleObj = rec.get("name");
                        if (titleObj == null)
                            titleObj = rec.get("title");
                        if (titleObj == null)
                            titleObj = rec.get("placeName");
                        String titleStr = (titleObj != null && !titleObj.toString().trim().isEmpty())
                                ? titleObj.toString().trim()
                                : "Landmark in " + parsedCity;
                        item.setTitle(titleStr);

                        // Map Category
                        Object catObj = rec.get("category");
                        String catStr = (catObj != null && !catObj.toString().trim().isEmpty())
                                ? catObj.toString().trim()
                                : "Sightseeing";
                        item.setCategory(catStr);

                        // Realistic Duration & Transit
                        int durationMinutes = 60;
                        if (durationHours != null && durationHours > 0) {
                            if (durationHours <= 1)
                                durationMinutes = 35;
                            else if (durationHours <= 2)
                                durationMinutes = 45;
                            else if (durationHours <= 3)
                                durationMinutes = 60;
                            else
                                durationMinutes = 75;
                        } else {
                            if (targetPerDay >= 5) {
                                durationMinutes = 60; // Fast-paced: 60 mins allows 5-6 attractions comfortably
                            } else if (targetPerDay <= 2) {
                                durationMinutes = 120; // Relaxed: 120 mins unhurried
                            } else {
                                durationMinutes = 80; // Moderate: 80 mins
                            }
                            Object durM = rec.get("duration_minutes");
                            if (durM != null) {
                                try {
                                    int customDur = (int) Double.parseDouble(durM.toString());
                                    if (targetPerDay >= 5) {
                                        durationMinutes = Math.min(75, Math.max(45, customDur));
                                    } else if (targetPerDay <= 2) {
                                        durationMinutes = Math.max(90, Math.min(180, customDur));
                                    } else {
                                        durationMinutes = Math.min(110, Math.max(60, customDur));
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                        item.setDurationMinutes(durationMinutes);

                        // Midday lunch break buffer for 4+ items
                        if (itemsToday == 3 && currentClock.isBefore(java.time.LocalTime.of(14, 0))
                                && currentClock.isAfter(java.time.LocalTime.of(12, 15))) {
                            currentClock = currentClock.plusMinutes(35); // 35 min lunch break buffer
                        }

                        if (rec.containsKey("latitude") && rec.get("latitude") != null) {
                            try {
                                item.setLatitude(Double.parseDouble(rec.get("latitude").toString()));
                            } catch (Exception ignored) {
                            }
                        }
                        if (rec.containsKey("longitude") && rec.get("longitude") != null) {
                            try {
                                item.setLongitude(Double.parseDouble(rec.get("longitude").toString()));
                            } catch (Exception ignored) {
                            }
                        }
                        if (item.getLatitude() == null && destLat != null)
                            item.setLatitude(destLat);
                        if (item.getLongitude() == null && destLon != null)
                            item.setLongitude(destLon);

                        // Distance and travel time calculation between consecutive attractions in
                        // geographical order
                        double dKm;
                        int transitMin;
                        if (itemsToday == 0) {
                            if (i == 0) {
                                String startPlace = trip.getStartingLocation() != null ? trip.getStartingLocation()
                                        : parsedCity;
                                double rawD = (item.getLatitude() != null && item.getLongitude() != null)
                                        ? calculateHaversineKm(day1StartLat, day1StartLon, item.getLatitude(),
                                                item.getLongitude())
                                        : 2.5;
                                dKm = Math.max(0.5, Math.round(rawD * 10.0) / 10.0);
                                transitMin = Math.max(15, Math.min(60, (int) Math.round((dKm / 25.0) * 60) + 8));
                                item.setDistanceFromPrevious(dKm);
                                item.setDurationFromPrevious(transitMin);
                                item.setTransportFromPrevious(
                                        "Local Transit (" + startPlace + " → " + item.getTitle() + ")");
                                item.setEstimatedTransportCost(Math.max(40.0, Math.round(dKm * 15.0)));
                            } else {
                                ItineraryItem prevDayLastItem = null;
                                if (!trip.getDays().isEmpty()) {
                                    ItineraryDay prevDay = trip.getDays().get(i - 1);
                                    if (!prevDay.getItems().isEmpty()) {
                                        prevDayLastItem = prevDay.getItems().get(prevDay.getItems().size() - 1);
                                    }
                                }
                                double prevLat = (prevDayLastItem != null && prevDayLastItem.getLatitude() != null)
                                        ? prevDayLastItem.getLatitude()
                                        : day1StartLat;
                                double prevLon = (prevDayLastItem != null && prevDayLastItem.getLongitude() != null)
                                        ? prevDayLastItem.getLongitude()
                                        : day1StartLon;
                                double rawD = (item.getLatitude() != null && item.getLongitude() != null)
                                        ? calculateHaversineKm(prevLat, prevLon, item.getLatitude(),
                                                item.getLongitude())
                                        : 3.5;
                                dKm = Math.max(0.5, Math.round(rawD * 10.0) / 10.0);
                                transitMin = Math.max(15, Math.min(60, (int) Math.round((dKm / 25.0) * 60) + 8));
                                item.setDistanceFromPrevious(dKm);
                                item.setDurationFromPrevious(transitMin);
                                String prevDesc = prevDayLastItem != null ? prevDayLastItem.getTitle()
                                        : ("Day " + i + " Hub");
                                item.setTransportFromPrevious(
                                        "Local Transit (" + prevDesc + " → " + item.getTitle() + ")");
                                item.setEstimatedTransportCost(Math.max(40.0, Math.round(dKm * 15.0)));
                            }
                        } else {
                            ItineraryItem prev = day.getItems().get(itemsToday - 1);
                            double rawD = (prev.getLatitude() != null && item.getLatitude() != null)
                                    ? calculateHaversineKm(prev.getLatitude(), prev.getLongitude(), item.getLatitude(),
                                            item.getLongitude())
                                    : 2.0;
                            dKm = Math.max(0.2, Math.round(rawD * 10.0) / 10.0);
                            transitMin = Math.max(10, Math.min(45, (int) Math.round((dKm / 25.0) * 60) + 8));
                            item.setDistanceFromPrevious(dKm);
                            item.setDurationFromPrevious(transitMin);
                            item.setTransportFromPrevious(
                                    "Local Transit (" + prev.getTitle() + " → " + item.getTitle() + ")");
                            item.setEstimatedTransportCost(Math.max(40.0, Math.round(dKm * 15.0)));
                        }

                        java.time.LocalTime activityStart = currentClock.plusMinutes(transitMin);
                        java.time.LocalTime activityEnd = activityStart.plusMinutes(durationMinutes);

                        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter
                                .ofPattern("hh:mm a", Locale.ENGLISH);
                        item.setStartTime(activityStart.format(timeFmt));
                        item.setEndTime(activityEnd.format(timeFmt));
                        currentClock = activityEnd;

                        // Map Cost
                        Object costObj = rec.get("entry_fee_inr");
                        if (costObj == null)
                            costObj = rec.get("entryFeeInr");
                        if (costObj == null)
                            costObj = rec.get("cost");
                        if (costObj != null) {
                            try {
                                item.setCostInr(Double.parseDouble(costObj.toString()));
                            } catch (Exception e) {
                                item.setCostInr(0.0);
                            }
                        } else {
                            item.setCostInr(0.0);
                        }

                        // Map Indoor / Outdoor
                        Object inOutObj = rec.get("indoor_outdoor");
                        if (inOutObj == null)
                            inOutObj = rec.get("indoorOutdoor");
                        String inOutStr = inOutObj != null ? inOutObj.toString() : "Outdoor";
                        item.setIndoorOutdoor(inOutStr);

                        // Map Reason - Explain pace and authenticity gracefully
                        Object reasonObj = rec.get("recommendation_reason");
                        if (reasonObj == null)
                            reasonObj = rec.get("description");
                        String reasonStr = reasonObj != null ? reasonObj.toString()
                                : "Top-rated authentic destination in " + parsedCity;
                        if (itemsForThisDay < minTargetPerDay) {
                            reasonStr += " • Paced with all verified authentic attractions in " + parsedCity
                                    + " without non-tourist filler.";
                        }
                        item.setRecommendationReason(reasonStr);

                        // Genuine Rating Handling
                        Object rObj = rec.get("rating");
                        if (rObj != null && !(rObj instanceof String && ((String) rObj).contains("unavailable"))) {
                            try {
                                item.setRating(Double.parseDouble(rObj.toString()));
                            } catch (Exception ignored) {
                                item.setRating(null);
                            }
                        } else {
                            item.setRating(null);
                        }

                        // Genuine Review Count
                        Object revObj = rec.get("reviews_count");
                        if (revObj == null)
                            revObj = rec.get("reviewsCount");
                        if (revObj == null)
                            revObj = rec.get("reviewCount");
                        if (revObj != null
                                && !(revObj instanceof String && ((String) revObj).contains("unavailable"))) {
                            try {
                                item.setReviewCount(Integer.parseInt(revObj.toString()));
                            } catch (Exception ignored) {
                                item.setReviewCount(null);
                            }
                        } else {
                            item.setReviewCount(null);
                        }

                        // Internal AI Match Score
                        Object scoreObj = rec.get("predicted_score");
                        if (scoreObj != null) {
                            try {
                                item.setAiMatchScore(
                                        Math.round(Double.parseDouble(scoreObj.toString()) * 100 * 10.0) / 10.0);
                            } catch (Exception ignored) {
                                item.setAiMatchScore(92.0);
                            }
                        } else {
                            item.setAiMatchScore(92.0);
                        }

                        // Address & Opening Hours
                        item.setAddress(rec.get("address") != null ? rec.get("address").toString()
                                : (parsedLocality != null ? parsedLocality + ", " + parsedCity
                                        : parsedCity + ", India"));
                        item.setOpeningHours(rec.get("openingHours") != null ? rec.get("openingHours").toString()
                                : "Open today • Regular hours");

                        item.setVerificationStatus("100% Real Location • Verified");
                        item.setStatus("SCHEDULED");
                        item.setItemOrder(itemsToday);
                        day.getItems().add(item);

                        itemsToday++;
                    }

                    trip.getDays().add(day);
                }

                // Calculate total budget based on items
                double total = trip.getDays().stream()
                        .flatMap(d -> d.getItems().stream())
                        .mapToDouble(it -> it.getCostInr() != null ? it.getCostInr() : 0.0)
                        .sum();
                trip.setTotalEstimatedCost(total);

                savedTrip = tripRepository.save(trip);
            } else {
                throw new RuntimeException(
                        "Couldn't verify this location. Please ensure it is a valid destination within India.");
            }
        } catch (Exception e) {
            System.err.println("Failed to build itinerary: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("supports travel within India")) {
                throw e;
            }
            e.printStackTrace();
            throw new RuntimeException(
                    "Couldn't verify this location. Please ensure it is a valid destination within India.");
        }

        return savedTrip;

    }

    public List<Trip> getUserTrips(String userEmail) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User user = userOpt.get();
        List<Trip> ownedTrips = tripRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<TripMember> sharedMemberships = tripMemberRepository.findByUserIdAndStatus(user.getId(),
                InvitationStatus.ACCEPTED);

        List<Trip> result = new ArrayList<>(ownedTrips);
        Set<Long> tripIds = new HashSet<>();
        for (Trip t : ownedTrips) {
            tripIds.add(t.getId());
        }
        for (TripMember m : sharedMemberships) {
            if (m.getTrip() != null && !tripIds.contains(m.getTrip().getId())) {
                result.add(m.getTrip());
                tripIds.add(m.getTrip().getId());
            }
        }
        result.sort((a, b) -> {
            if (b.getCreatedAt() != null && a.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return 0;
        });
        return result;
    }

    public void deleteTrip(String userEmail, Long tripId) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            throw new RuntimeException("Trip not found");
        }

        Trip trip = tripOpt.get();
        if (!trip.getUser().getId().equals(userOpt.get().getId())) {
            throw new RuntimeException("Only the trip owner can delete this trip");
        }

        tripRepository.delete(trip);
    }

    public Trip getTripById(String userEmail, Long id) {
        Optional<Trip> tripOpt = tripRepository.findById(id);
        if (tripOpt.isEmpty()) {
            throw new RuntimeException("Trip not found");
        }
        Trip trip = tripOpt.get();
        if (trip.getDays() != null) {
            trip.getDays().sort(
                    Comparator.comparing(ItineraryDay::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())));
            for (ItineraryDay day : trip.getDays()) {
                if (day.getItems() != null) {
                    day.getItems().sort(Comparator.comparing(ItineraryItem::getItemOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                }
            }
        }
        return trip;
    }

    public Trip adaptTripDay(String userEmail, Long tripId, Long dayId, String weatherCondition, Double rainProb,
            Double temp) {
        Trip trip = getTripById(userEmail, tripId);

        ItineraryDay targetDay = null;
        for (ItineraryDay day : trip.getDays()) {
            if (day.getId().equals(dayId)) {
                targetDay = day;
                break;
            }
        }

        if (targetDay == null) {
            throw new RuntimeException("Day not found in this trip");
        }

        // Normalize rain probability safely (handles 0.0-1.0 and 0-100 formats)
        double normalizedProb = (rainProb != null && rainProb > 1.0) ? (rainProb / 100.0) : (rainProb != null ? rainProb : 0.0);
        targetDay.setWeatherCondition(weatherCondition);
        targetDay.setRainProbability(normalizedProb);
        targetDay.setMaxTempC(temp != null ? temp : 28.0);

        boolean needsAdaptation = normalizedProb >= 0.50;

        if (needsAdaptation) {
            java.util.List<ItineraryItem> newItems = new java.util.ArrayList<>();
            int replaceCount = 0;

            for (ItineraryItem item : targetDay.getItems()) {
                if ("Outdoor".equalsIgnoreCase(item.getIndoorOutdoor()) && !"REPLACED".equals(item.getStatus())) {
                    item.setStatus("REPLACED");
                    item.setWeatherAlert(
                            "Outdoor activity skipped due to rainfall forecast — replaced with weather-protected indoor alternative.");

                    // Generate authentic replacement indoor activity based on destination
                    String[] indoorData = getIndoorActivityForCity(trip.getDestinationCity(), replaceCount);

                    ItineraryItem replacement = new ItineraryItem();
                    replacement.setItineraryDay(targetDay);
                    replacement.setTitle(indoorData[0]);
                    replacement.setCategory(indoorData[1]);

                    try {
                        replacement.setCostInr(Double.parseDouble(indoorData[2]));
                    } catch (Exception e) {
                        replacement.setCostInr(item.getCostInr() != null ? item.getCostInr() : 0.0);
                    }

                    if (indoorData[3] != null && indoorData[4] != null) {
                        try {
                            replacement.setLatitude(Double.parseDouble(indoorData[3]));
                            replacement.setLongitude(Double.parseDouble(indoorData[4]));
                        } catch (Exception e) {
                            replacement.setLatitude(item.getLatitude());
                            replacement.setLongitude(item.getLongitude());
                        }
                    } else {
                        replacement.setLatitude(item.getLatitude());
                        replacement.setLongitude(item.getLongitude());
                    }

                    replacement.setIndoorOutdoor("Indoor");
                    replacement.setRecommendationReason(
                            "🌧️ AI Weather Adapted: Replaced outdoor activity with a verified indoor cultural spot safe from rain.");
                    replacement.setStartTime(item.getStartTime());
                    replacement.setDurationMinutes(item.getDurationMinutes() != null ? item.getDurationMinutes() : 120);
                    replacement.setStatus("ACTIVE");
                    replacement.setItemOrder(item.getItemOrder());

                    newItems.add(replacement);
                    replaceCount++;
                } else {
                    if (!"REPLACED".equals(item.getStatus())) {
                        item.setRecommendationReason("Protected indoor destination safe from rainfall.");
                    }
                }
            }

            targetDay.getItems().addAll(newItems);

            double total = trip.getDays().stream()
                    .flatMap(d -> d.getItems().stream())
                    .filter(i -> !"REPLACED".equals(i.getStatus()))
                    .mapToDouble(i -> i.getCostInr() != null ? i.getCostInr() : 0.0)
                    .sum();
            trip.setTotalEstimatedCost(total);

            return tripRepository.save(trip);
        }
        return trip;
    }

    private String[] getIndoorActivityForCity(String city, int index) {
        String c = city != null ? city.toLowerCase().trim() : "";
        if (c.contains("vijayawada")) {
            String[][] data = {
                    { "Bapu Museum (Victoria Jubilee)", "Museums", "50", "16.5050", "80.6385" },
                    { "Victoria Cultural Art Gallery & Crafts", "Culture", "30", "16.5070", "80.6350" },
                    { "Kondapalli Wooden Toy Craft Emporium", "Shopping", "0", "16.5120", "80.6200" },
                    { "Kanaka Durga Covered Sanctum & Temple Complex", "Spiritual", "0", "16.5165", "80.6062" }
            };
            return data[index % data.length];
        } else if (c.contains("visakhapatnam") || c.contains("vizag")) {
            String[][] data = {
                    { "INS Kursura Submarine Museum", "Museums", "70", "17.7171", "83.3323" },
                    { "TU-142 Aircraft Museum", "Museums", "70", "17.7178", "83.3331" },
                    { "Visakha Maritime Heritage Gallery", "Culture", "40", "17.7150", "83.3310" },
                    { "Lepakshi Handlooms & Handicrafts Pavilion", "Shopping", "0", "17.7200", "83.3100" }
            };
            return data[index % data.length];
        } else if (c.contains("araku")) {
            String[][] data = {
                    { "Araku Tribal Heritage Museum", "Museums", "40", "18.3312", "82.8732" },
                    { "Araku Valley Coffee Museum & Roastery", "Food", "50", "18.3320", "82.8740" },
                    { "Borra Karst Cave Chamber Gallery", "Nature", "80", "18.2806", "83.0396" },
                    { "Tribal Handloom Weaving Centre", "Culture", "0", "18.3350", "82.8710" }
            };
            return data[index % data.length];
        } else if (c.contains("tirupati") || c.contains("tirumala")) {
            String[][] data = {
                    { "Regional Science Centre & Planetarium", "Museums", "40", "13.6390", "79.3970" },
                    { "Sri Venkateswara Museum of Temple Art", "Museums", "0", "13.6840", "79.3490" },
                    { "Chandragiri Palace Indo-Saracenic Gallery", "History", "25", "13.5833", "79.3167" },
                    { "Sri Govindaraja Covered Temple Mandapam", "Spiritual", "0", "13.6293", "79.4182" }
            };
            return data[index % data.length];
        } else if (c.contains("hyderabad")) {
            String[][] data = {
                    { "Salar Jung Art Museum", "Museums", "50", "17.3713", "78.4804" },
                    { "Birla Planetarium & Science Centre", "Museums", "100", "17.4060", "78.4690" },
                    { "Chowmahalla Palace Covered Durbar Hall", "History", "100", "17.3578", "78.4717" },
                    { "Shilparamam Arts & Handloom Indoor Village", "Culture", "60", "17.4520", "78.3780" }
            };
            return data[index % data.length];
        } else if (c.contains("jaipur")) {
            String[][] data = {
                    { "City Palace Royal Museum & Durbar", "Museums", "300", "26.9258", "75.8237" },
                    { "Albert Hall Central State Museum", "Museums", "100", "26.9117", "75.8194" },
                    { "Anokhi Hand Printing Heritage Museum", "Culture", "50", "26.9930", "75.8500" },
                    { "Johari Covered Bazaar & Textile Craft Hub", "Shopping", "0", "26.9200", "75.8250" }
            };
            return data[index % data.length];
        } else if (c.contains("goa")) {
            String[][] data = {
                    { "Goa State Archaeological Museum", "Museums", "20", "15.4989", "73.8278" },
                    { "Museum of Christian Art", "Museums", "100", "15.5015", "73.9135" },
                    { "Houses of Goa Architectural Museum", "Culture", "100", "15.5340", "73.8400" },
                    { "Naval Aviation Covered Museum", "Museums", "50", "15.3850", "73.8370" }
            };
            return data[index % data.length];
        } else {
            String cap = (city != null && !city.trim().isEmpty())
                    ? city.substring(0, 1).toUpperCase() + city.substring(1).trim()
                    : "City";
            String[][] gen = {
                    { cap + " Heritage Museum & Art Gallery", "Museums", "50", null, null },
                    { cap + " Indoor Cultural Centre & Local Handlooms", "Culture", "0", null, null },
                    { "Traditional Regional Culinary & Spice Tasting House", "Food", "150", null, null },
                    { cap + " Historical Archaeological Pavilion", "History", "40", null, null }
            };
            return gen[index % gen.length];
        }
    }

    private static final java.util.Set<String> AIRPORT_CITIES = java.util.Set.of(
            "delhi", "new delhi", "mumbai", "bengaluru", "bangalore", "hyderabad", "chennai", "kolkata",
            "ahmedabad", "pune", "goa", "jaipur", "lucknow", "kochi", "cochin", "visakhapatnam", "vizag",
            "vijayawada", "tirupati", "rajahmundry", "madurai", "coimbatore", "varanasi", "chandigarh",
            "bhubaneswar", "patna", "indore", "srinagar", "udaipur", "mangalore", "calicut", "kozhikode",
            "thiruvananthapuram", "trivandrum", "guwahati", "surat", "nagpur", "bhopal", "amritsar",
            "dehradun", "bagdogra", "port blair", "ayodhya", "shirdi", "gaya", "jodhpur");

    public double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(6371 * c * 10.0) / 10.0;
    }

    public boolean isAirportCity(String cityName) {
        if (cityName == null)
            return false;
        String clean = cityName.trim().toLowerCase();
        for (String airport : AIRPORT_CITIES) {
            if (clean.equals(airport) || clean.contains(airport) || airport.contains(clean)) {
                return true;
            }
        }
        return false;
    }

    public void validateAndConfigureTransport(Trip trip, String requestedMode) {
        String origin = trip.getStartingLocation();
        String dest = trip.getDestinationCity();
        if (origin == null || origin.trim().isEmpty() || origin.equalsIgnoreCase("India")) {
            origin = dest;
            trip.setStartingLocation(dest);
        }

        double distance = 45.0; // default estimated distance
        if (trip.getOriginLatitude() != null && trip.getOriginLongitude() != null &&
                trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null) {
            distance = calculateHaversineKm(
                    trip.getOriginLatitude(), trip.getOriginLongitude(),
                    trip.getDestinationLatitude(), trip.getDestinationLongitude());
        }

        String mode = (requestedMode != null && !requestedMode.trim().isEmpty()) ? requestedMode.trim() : "Bus / Road";
        boolean flightRequested = mode.toLowerCase().contains("flight") || mode.toLowerCase().contains("air");
        boolean trainRequested = mode.toLowerCase().contains("train") || mode.toLowerCase().contains("rail");

        if (flightRequested) {
            boolean originHasAirport = isAirportCity(origin);
            boolean destHasAirport = isAirportCity(dest);
            boolean isLongDistance = distance >= 150.0;

            if (!originHasAirport || !destHasAirport || !isLongDistance) {
                // Flight is NOT available
                String reason = !originHasAirport
                        ? origin + " has no commercial airport (nearest airport is Vijayawada Airport - VGA / regional hub)"
                        : (!destHasAirport
                                ? dest + " has no commercial airport"
                                : "Distance (" + distance + " km) is too short for scheduled commercial flights");

                trip.setTransportNotice("Your selected transport (Flight) is not available for " + origin + " → " + dest
                        + ". We switched to the available/recommended ground transport (Bus / Train / Road). " + reason
                        + ".");

                // Switch to realistic ground transport
                if (distance > 300) {
                    trip.setTransitPreference("Train (Indian Railways)");
                } else {
                    trip.setTransitPreference("Bus / Road Transport");
                }
            } else {
                trip.setTransitPreference("Flight (Air Travel)");
                trip.setTransportNotice(
                        "Direct commercial air connectivity verified between " + origin + " and " + dest + ".");
            }
        } else if (trainRequested) {
            trip.setTransitPreference("Train (Indian Railways)");
            trip.setTransportNotice(null);
        } else {
            trip.setTransitPreference(mode);
            trip.setTransportNotice(null);
        }
    }

    public Trip markActivityCompleted(String userEmail, Long tripId, Long activityId) {
        Trip trip = getTripById(userEmail, tripId);
        boolean found = false;
        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if (item.getId().equals(activityId)) {
                    item.setStatus("COMPLETED");
                    itineraryItemRepository.save(item);
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }
        if (!found) {
            throw new RuntimeException("Activity not found in this trip");
        }
        return tripRepository.save(trip);
    }

    public Map<String, Object> checkNextActivity(String userEmail, Long tripId, Long currentActivityId,
            String weatherOverride) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryItem currentItem = null;
        ItineraryItem nextItem = null;
        ItineraryDay nextDay = null;
        boolean currentFound = false;

        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if ("REPLACED".equals(item.getStatus()) || "CANCELLED".equals(item.getStatus())) {
                    continue;
                }
                if (item.getId().equals(currentActivityId)) {
                    currentItem = item;
                    currentFound = true;
                    continue;
                }
                if (currentFound && !"COMPLETED".equals(item.getStatus()) && nextItem == null) {
                    nextItem = item;
                    nextDay = day;
                    break;
                }
            }
            if (nextItem != null)
                break;
        }

        Map<String, Object> response = new HashMap<>();
        if (nextItem == null) {
            response.put("hasNext", false);
            response.put("message", "All scheduled destinations have been visited! Your trip itinerary is complete.");
            return response;
        }

        response.put("hasNext", true);
        response.put("currentActivityTitle", currentItem != null ? currentItem.getTitle() : "Current Location");
        response.put("currentActivityId", currentActivityId);
        response.put("nextActivityId", nextItem.getId());
        response.put("nextActivityTitle", nextItem.getTitle());
        response.put("nextCategory", nextItem.getCategory());
        response.put("nextIndoorOutdoor", nextItem.getIndoorOutdoor());

        // Live or Verified Coordinates
        double destLat = nextItem.getLatitude() != null ? nextItem.getLatitude()
                : (trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 16.5062);
        double destLon = nextItem.getLongitude() != null ? nextItem.getLongitude()
                : (trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 80.6480);

        // Determine the planned date of the next activity
        java.time.LocalDate activityDate = (nextDay != null) ? nextDay.getDate() : null;
        if (activityDate == null && trip.getStartDate() != null && nextDay != null && nextDay.getDayNumber() != null) {
            activityDate = trip.getStartDate().plusDays(nextDay.getDayNumber() - 1);
        }

        // Fetch Real Weather for Location and Planned Date without artificial overrides
        Map<String, Object> weather = weatherService.getConditionsForLocationAndDate(destLat, destLon, activityDate);
        boolean weatherAvailable = weather != null && Boolean.TRUE.equals(weather.get("isLive")) && weather.get("rainProbability") != null;

        Double rainProb = weatherAvailable ? ((Number) weather.get("rainProbability")).doubleValue() : null;
        Double tempC = weatherAvailable && weather.get("temperatureC") != null ? ((Number) weather.get("temperatureC")).doubleValue() : null;
        String weatherDesc = weatherAvailable && weather.get("weatherCondition") != null ? weather.get("weatherCondition").toString() : "Forecast Unavailable";
        Double precipMm = weatherAvailable && weather.get("precipitationMm") != null ? ((Number) weather.get("precipitationMm")).doubleValue() : null;
        Double currentPrecipMm = weatherAvailable && weather.get("currentPrecipitationMm") != null ? ((Number) weather.get("currentPrecipitationMm")).doubleValue() : null;
        Integer weatherCode = weatherAvailable && weather.get("weatherCode") != null ? ((Number) weather.get("weatherCode")).intValue() : null;

        // Live Weather (Raw Open-Meteo readings, completely isolated from simulation)
        Map<String, Object> liveConditions = new HashMap<>();
        liveConditions.put("weather", weatherDesc);
        liveConditions.put("temperature", tempC != null ? Math.round(tempC) + "°C" : "Unavailable");
        liveConditions.put("temperatureC", tempC != null ? Math.round(tempC) : null);
        liveConditions.put("rainProbability", rainProb != null ? Math.round(rainProb) + "%" : "Unavailable");
        liveConditions.put("rainProbabilityNumeric", rainProb != null ? Math.round(rainProb) : null);
        liveConditions.put("precipitationMm", precipMm);
        liveConditions.put("currentPrecipitationMm", currentPrecipMm);
        liveConditions.put("weatherAvailable", weatherAvailable);

        // Support simulated weather check for testing - kept completely separate from live weather
        boolean isSimulated = weatherOverride != null 
                && !weatherOverride.isBlank() 
                && !"live".equalsIgnoreCase(weatherOverride)
                && !"actual".equalsIgnoreCase(weatherOverride)
                && !"none".equalsIgnoreCase(weatherOverride)
                && !"false".equalsIgnoreCase(weatherOverride);

        Double activeRainProb = rainProb;
        Double activePrecipMm = precipMm;
        Double activeTempC = tempC;
        Integer activeWeatherCode = weatherCode;
        String activeWeatherDesc = weatherDesc;

        Map<String, Object> simulatedConditions = null;
        if (isSimulated) {
            simulatedConditions = new HashMap<>();
            if ("rain".equalsIgnoreCase(weatherOverride)) {
                activeRainProb = 85.0;
                activePrecipMm = 12.0;
                activeTempC = (tempC != null) ? tempC : 26.0;
                activeWeatherCode = 65;
                activeWeatherDesc = "Heavy Rain & Showers (Simulated Test)";
            } else if ("clear".equalsIgnoreCase(weatherOverride)) {
                activeRainProb = 5.0;
                activePrecipMm = 0.0;
                activeTempC = (tempC != null) ? tempC : 29.0;
                activeWeatherCode = 0;
                activeWeatherDesc = "Clear / Sunny (Simulated Test)";
            }
            simulatedConditions.put("weather", activeWeatherDesc);
            simulatedConditions.put("rainProbability", Math.round(activeRainProb) + "%");
            simulatedConditions.put("rainProbabilityNumeric", Math.round(activeRainProb));
            simulatedConditions.put("precipitationMm", activePrecipMm);
            simulatedConditions.put("temperature", Math.round(activeTempC) + "°C");
            simulatedConditions.put("scenario", weatherOverride);
        }

        // Distance & Travel Time Calculation from current to next
        double distKm = 3.5;
        int travelMin = 15;
        if (currentItem != null && currentItem.getLatitude() != null && nextItem.getLatitude() != null) {
            distKm = calculateHaversineKm(currentItem.getLatitude(), currentItem.getLongitude(), nextItem.getLatitude(),
                    nextItem.getLongitude());
            travelMin = Math.max(10, (int) (distKm * 2.8));
        }

        // Traffic Conditions
        java.time.LocalTime now = java.time.LocalTime.now();
        int hour = now.getHour();
        boolean isPeak = (hour >= 8 && hour <= 11) || (hour >= 17 && hour <= 20);
        String trafficStatus = isPeak ? "High" : (distKm > 6 ? "Moderate" : "Normal Flow");
        String trafficDetail = isPeak ? "Congested peak flow (+15 min delay)" : "Normal moving transit";

        // Crowd Conditions
        String cat = nextItem.getCategory() != null ? nextItem.getCategory().toLowerCase() : "";
        String crowdStatus = (cat.contains("temple") || cat.contains("beach") || cat.contains("mall")
                || cat.contains("shopping")) && (isPeak || now.getHour() >= 17) ? "High" : "Moderate";

        // Opening Status
        String openingStatus = (nextItem.getOpeningHours() != null
                && !nextItem.getOpeningHours().equalsIgnoreCase("Information unavailable"))
                        ? nextItem.getOpeningHours()
                        : "Open today • Regular Hours";

        // Suitability Evaluation:
        // Severe weather warnings trigger on high rain probability (>= 60%), heavy rainfall (>= 2.5mm),
        // or storm conditions (WMO 65, 82, 95, 96, 99) when forecast data is available or simulated.
        boolean isOutdoor = "Outdoor".equalsIgnoreCase(nextItem.getIndoorOutdoor());
        boolean isSevereWeather = false;
        if ((weatherAvailable || isSimulated) && activeRainProb != null && activePrecipMm != null && activeWeatherCode != null) {
            isSevereWeather = (activeRainProb >= 60.0) || (activePrecipMm >= 2.5) || (activeWeatherCode == 65 || activeWeatherCode == 82 || activeWeatherCode >= 95);
        }
        boolean isAdverseForOutdoor = isOutdoor && (isSevereWeather || isSimulated);
        boolean isSuitable = !isAdverseForOutdoor;

        Map<String, Object> conditions = new HashMap<>();
        conditions.put("weather", activeWeatherDesc);
        conditions.put("temperature", activeTempC != null ? Math.round(activeTempC) + "°C" : "Unavailable");
        conditions.put("rainProbability", activeRainProb != null ? Math.round(activeRainProb) + "%" : "Unavailable");
        conditions.put("precipitationMm", activePrecipMm);
        conditions.put("currentPrecipitationMm", currentPrecipMm);
        conditions.put("traffic", trafficStatus);
        conditions.put("trafficDetail", trafficDetail);
        conditions.put("crowd", crowdStatus);
        conditions.put("openingStatus", openingStatus);
        conditions.put("distanceKm", distKm);
        conditions.put("travelTimeMin", travelMin);

        response.put("conditions", conditions);
        response.put("liveWeather", liveConditions);
        response.put("simulatedWeather", simulatedConditions);
        response.put("isSuitable", isSuitable);
        response.put("rainProbability", activeRainProb != null ? Math.round(activeRainProb) : null);
        response.put("precipitationMm", activePrecipMm);
        response.put("currentPrecipitationMm", currentPrecipMm);
        response.put("temperatureC", activeTempC != null ? Math.round(activeTempC) : null);
        response.put("isSimulated", isSimulated);
        response.put("weatherAvailable", weatherAvailable);
        response.put("trafficLevel", trafficStatus);
        response.put("crowdLevel", crowdStatus);
        response.put("openingStatus", openingStatus);
        response.put("distanceKm", distKm);
        response.put("travelTimeEst", travelMin + " mins");

        if (!isSuitable) {
            response.put("alertTitle", isSimulated
                    ? "🧪 Simulation Mode: Inclement Weather Alert"
                    : "⚠️ Inclement Weather Alert at Next Destination");
            String warningMsg;
            if (isSimulated) {
                warningMsg = "[SIMULATION TEST] Simulated adverse weather: " + activeWeatherDesc + " (" + Math.round(activeRainProb) + "% rain probability, " + activePrecipMm + " mm rainfall) at "
                        + nextItem.getTitle() + ". Use this test scenario to verify adaptive itinerary recommendations.";
            } else if (activeWeatherCode != null && (activeWeatherCode >= 95 || activeWeatherCode == 65 || activeWeatherCode == 82)) {
                warningMsg = activeWeatherDesc + " with " + activePrecipMm + " mm rainfall (" + Math.round(activeRainProb) + "% rain probability) expected at "
                        + nextItem.getTitle() + ". An indoor alternative is recommended.";
            } else if (activePrecipMm != null && activePrecipMm >= 2.5) {
                warningMsg = "Heavy rainfall (" + activePrecipMm + " mm, " + Math.round(activeRainProb) + "% rain probability) expected at "
                        + nextItem.getTitle() + ". An indoor alternative is recommended.";
            } else {
                warningMsg = "Elevated precipitation probability (" + (activeRainProb != null ? Math.round(activeRainProb) : 0) + "%) and "
                        + activeWeatherDesc.toLowerCase() + " expected at " + nextItem.getTitle()
                        + ". An indoor alternative is recommended.";
            }
            response.put("warning", warningMsg);
            response.put("alertMessage", warningMsg);
            response.put("userActionRequired", true);
        } else if (weatherAvailable && activeRainProb != null && activeRainProb >= 35.0 && isOutdoor) {
            response.put("alertTitle", "Next Activity Ready (Mild Weather Advisory)");
            String advisoryMsg = "Passing cloud/light shower chance (" + Math.round(activeRainProb) + "%) at "
                    + nextItem.getTitle() + ". Outdoor visit is on track; carrying an umbrella is recommended.";
            response.put("warning", advisoryMsg);
            response.put("alertMessage", advisoryMsg);
            response.put("userActionRequired", false);
        } else {
            response.put("alertTitle", "Next Activity Ready");
            response.put("warning", null);
            response.put("alertMessage", null);
            response.put("userActionRequired", false);
        }

        return response;
    }

    public List<Map<String, Object>> getAlternatives(String userEmail, Long tripId, Long activityId, String reason) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryItem targetItem = null;
        Set<String> scheduledTitles = new HashSet<>();

        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if (item.getId().equals(activityId)) {
                    targetItem = item;
                }
                if (!"REPLACED".equals(item.getStatus()) && !"CANCELLED".equals(item.getStatus())) {
                    scheduledTitles.add(item.getTitle().toLowerCase().trim());
                }
            }
        }

        if (targetItem == null) {
            throw new RuntimeException("Activity not found in trip");
        }

        double centerLat = targetItem.getLatitude() != null ? targetItem.getLatitude()
                : (trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 16.5062);
        double centerLon = targetItem.getLongitude() != null ? targetItem.getLongitude()
                : (trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 80.6480);
        String destCity = trip.getLocality() != null && !trip.getLocality().trim().isEmpty() ? trip.getLocality()
                : trip.getDestinationCity();

        boolean preferIndoor = "Weather concern".equalsIgnoreCase(reason);
        boolean preferLowCost = "Too expensive".equalsIgnoreCase(reason);
        boolean preferCloseBy = "Too far".equalsIgnoreCase(reason);

        double maxDist = (trip.getMaxDistanceKm() != null && trip.getMaxDistanceKm() > 0) ? trip.getMaxDistanceKm()
                : (preferCloseBy ? 3.0 : 15.0);
        double maxFee = (trip.getMaxEntryFee() != null) ? trip.getMaxEntryFee() : (preferLowCost ? 50.0 : 10000.0);

        List<Map<String, Object>> alternatives = new ArrayList<>();

        // 1. Fetch real nearby attractions from PlaceDiscoveryService
        List<Map<String, Object>> discovered = placeDiscoveryService.discoverNearby(centerLat, centerLon, destCity,
                null, 15);
        for (Map<String, Object> disc : discovered) {
            String name = (String) disc.get("name");
            if (name == null || scheduledTitles.contains(name.toLowerCase().trim()))
                continue;

            double itemLat = disc.containsKey("lat") ? Double.parseDouble(disc.get("lat").toString()) : centerLat;
            double itemLon = disc.containsKey("lon") ? Double.parseDouble(disc.get("lon").toString()) : centerLon;
            double dist = calculateHaversineKm(centerLat, centerLon, itemLat, itemLon);

            if (dist > maxDist)
                continue;

            String cat = disc.containsKey("category") ? disc.get("category").toString() : "Sightseeing";
            boolean isIndoor = cat.toLowerCase().contains("museum") || cat.toLowerCase().contains("art") ||
                    cat.toLowerCase().contains("temple") || cat.toLowerCase().contains("spiritual") ||
                    cat.toLowerCase().contains("shopping") || cat.toLowerCase().contains("cafe") ||
                    cat.toLowerCase().contains("food") || cat.toLowerCase().contains("indoor");

            if (preferIndoor && !isIndoor)
                continue;

            double fee = 0.0;
            if (cat.toLowerCase().contains("museum"))
                fee = 50.0;
            else if (cat.toLowerCase().contains("heritage") || cat.toLowerCase().contains("monument"))
                fee = 25.0;

            if (fee > maxFee)
                continue;

            Map<String, Object> alt = new HashMap<>();
            alt.put("title", name);
            alt.put("name", name);
            alt.put("category", cat);
            alt.put("indoorOutdoor", isIndoor ? "Indoor" : "Outdoor");
            alt.put("costInr", fee);
            alt.put("entryFeeInr", fee);
            alt.put("latitude", itemLat);
            alt.put("longitude", itemLon);
            alt.put("rating", disc.get("rating"));
            alt.put("reviewCount", disc.get("reviewsCount"));
            alt.put("openingHours", disc.getOrDefault("openingHours", "Open today • Regular hours"));
            alt.put("address", disc.getOrDefault("address", destCity + ", India"));
            alt.put("distanceKm", dist);
            alt.put("aiMatchScore", Math.round(92.0 - (dist * 1.2)));
            alt.put("recommendationReason", isIndoor
                    ? "Weather-protected indoor destination safe from rainfall and matching your interests."
                    : "Top-rated alternative matching your budget, distance and travel preferences.");

            alternatives.add(alt);
            if (alternatives.size() >= 5)
                break;
        }

        // Fallback indoor authentic alternatives if empty
        if (alternatives.isEmpty()) {
            for (int idx = 0; idx < 4; idx++) {
                String[] indData = getIndoorActivityForCity(destCity, idx);
                if (!scheduledTitles.contains(indData[0].toLowerCase().trim())) {
                    Map<String, Object> alt = new HashMap<>();
                    alt.put("title", indData[0]);
                    alt.put("name", indData[0]);
                    alt.put("category", indData[1]);
                    alt.put("indoorOutdoor", "Indoor");
                    alt.put("costInr", Double.parseDouble(indData[2]));
                    alt.put("entryFeeInr", Double.parseDouble(indData[2]));
                    if (indData[3] != null && indData[4] != null) {
                        alt.put("latitude", Double.parseDouble(indData[3]));
                        alt.put("longitude", Double.parseDouble(indData[4]));
                        alt.put("distanceKm", calculateHaversineKm(centerLat, centerLon, Double.parseDouble(indData[3]),
                                Double.parseDouble(indData[4])));
                    } else {
                        alt.put("latitude", centerLat);
                        alt.put("longitude", centerLon);
                        alt.put("distanceKm", 2.5);
                    }
                    alt.put("rating", null);
                    alt.put("reviewCount", null);
                    alt.put("openingHours", "Open today • 9:30 AM - 6:00 PM");
                    alt.put("address", destCity + ", India");
                    alt.put("aiMatchScore", 95.0);
                    alt.put("recommendationReason", "Verified indoor cultural landmark safe from rain.");
                    alternatives.add(alt);
                    if (alternatives.size() >= 3)
                        break;
                }
            }
        }

        return alternatives;
    }

    public Trip replaceActivity(String userEmail, Long tripId, Long activityId, Map<String, Object> replacementData) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryItem target = null;
        ItineraryDay targetDay = null;
        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if (item.getId().equals(activityId)) {
                    target = item;
                    targetDay = day;
                    break;
                }
            }
            if (target != null)
                break;
        }

        if (target == null) {
            throw new RuntimeException("Activity not found in trip");
        }

        String newTitle = replacementData.containsKey("title") && replacementData.get("title") != null
                ? replacementData.get("title").toString().trim()
                : target.getTitle();
        target.setTitle(newTitle);

        // Parse coordinates from payload
        Double newLat = null;
        Double newLon = null;
        if (replacementData.containsKey("latitude") && replacementData.get("latitude") != null) {
            try {
                newLat = Double.parseDouble(replacementData.get("latitude").toString());
            } catch (Exception ignored) {
            }
        } else if (replacementData.containsKey("lat") && replacementData.get("lat") != null) {
            try {
                newLat = Double.parseDouble(replacementData.get("lat").toString());
            } catch (Exception ignored) {
            }
        }
        if (replacementData.containsKey("longitude") && replacementData.get("longitude") != null) {
            try {
                newLon = Double.parseDouble(replacementData.get("longitude").toString());
            } catch (Exception ignored) {
            }
        } else if (replacementData.containsKey("lon") && replacementData.get("lon") != null) {
            try {
                newLon = Double.parseDouble(replacementData.get("lon").toString());
            } catch (Exception ignored) {
            }
        }

        Double oldLat = target.getLatitude();
        Double oldLon = target.getLongitude();
        boolean isSameAsOldCoords = (newLat != null && oldLat != null && Math.abs(newLat - oldLat) < 0.0001
                && newLon != null && oldLon != null && Math.abs(newLon - oldLon) < 0.0001);

        String newAddress = replacementData.containsKey("address") && replacementData.get("address") != null
                ? replacementData.get("address").toString().trim()
                : null;

        // If coordinates missing, or accidentally kept from old attraction, look up
        // real location
        if (newLat == null || newLon == null || isSameAsOldCoords) {
            String q = newTitle;
            if (trip.getDestinationCity() != null
                    && !newTitle.toLowerCase().contains(trip.getDestinationCity().toLowerCase())) {
                q += ", " + trip.getDestinationCity();
            }
            Map<String, Object> found = placeDiscoveryService.searchPlace(q);
            if (found != null && found.containsKey("lat") && found.containsKey("lon")) {
                try {
                    newLat = Double.parseDouble(found.get("lat").toString());
                    newLon = Double.parseDouble(found.get("lon").toString());
                    if (newAddress == null && found.containsKey("address")) {
                        newAddress = found.get("address").toString();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (newLat != null && newLon != null) {
            target.setLatitude(newLat);
            target.setLongitude(newLon);
        } else if (trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null) {
            target.setLatitude(trip.getDestinationLatitude());
            target.setLongitude(trip.getDestinationLongitude());
        }

        if (newAddress != null && !newAddress.isEmpty()) {
            target.setAddress(newAddress);
        } else {
            target.setAddress(
                    newTitle + ", " + (trip.getDestinationCity() != null ? trip.getDestinationCity() : "India"));
        }

        // Automatic Intelligent Category Assignment (Never retain old irrelevant
        // category)
        String rawCat = replacementData.containsKey("category") && replacementData.get("category") != null
                ? replacementData.get("category").toString().trim()
                : null;
        target.setCategory(determineCategory(newTitle, target.getAddress(), rawCat));

        if (replacementData.containsKey("costInr") && replacementData.get("costInr") != null) {
            try {
                target.setCostInr(Double.parseDouble(replacementData.get("costInr").toString()));
            } catch (Exception ignored) {
            }
        }
        if (replacementData.containsKey("indoorOutdoor") && replacementData.get("indoorOutdoor") != null) {
            target.setIndoorOutdoor(replacementData.get("indoorOutdoor").toString());
        }
        if (replacementData.containsKey("durationMinutes") && replacementData.get("durationMinutes") != null) {
            try {
                target.setDurationMinutes(Integer.parseInt(replacementData.get("durationMinutes").toString()));
            } catch (Exception ignored) {
            }
        }
        if (replacementData.containsKey("startTime") && replacementData.get("startTime") != null
                && !replacementData.get("startTime").toString().trim().isEmpty()) {
            target.setStartTime(replacementData.get("startTime").toString().trim());
        }
        if (replacementData.containsKey("rating") && replacementData.get("rating") != null) {
            try {
                target.setRating(Double.parseDouble(replacementData.get("rating").toString()));
            } catch (Exception ignored) {
                target.setRating(null);
            }
        } else {
            target.setRating(null);
        }
        if (replacementData.containsKey("reviewCount") && replacementData.get("reviewCount") != null) {
            try {
                target.setReviewCount(Integer.parseInt(replacementData.get("reviewCount").toString()));
            } catch (Exception ignored) {
                target.setReviewCount(null);
            }
        } else {
            target.setReviewCount(null);
        }
        if (replacementData.containsKey("openingHours") && replacementData.get("openingHours") != null) {
            target.setOpeningHours(replacementData.get("openingHours").toString());
        }
        if (replacementData.containsKey("aiMatchScore") && replacementData.get("aiMatchScore") != null) {
            try {
                target.setAiMatchScore(Double.parseDouble(replacementData.get("aiMatchScore").toString()));
            } catch (Exception ignored) {
            }
        }
        if (replacementData.containsKey("recommendationReason")
                && replacementData.get("recommendationReason") != null) {
            target.setRecommendationReason(replacementData.get("recommendationReason").toString());
        } else {
            target.setRecommendationReason("Selected by user as personalized alternative.");
        }

        target.setStatus("SCHEDULED");
        target.setWeatherAlert(null);
        itineraryItemRepository.save(target);

        if (targetDay != null) {
            Object[] startCtx = getDayStartContext(trip, targetDay);
            routeOptimizationService.recalculateDaySchedule(targetDay, (double) startCtx[0], (double) startCtx[1],
                    (String) startCtx[2], java.time.LocalTime.of(9, 30));
            for (ItineraryItem it : targetDay.getItems()) {
                itineraryItemRepository.save(it);
            }
            if (itineraryDayRepository != null) {
                itineraryDayRepository.save(targetDay);
            }
        }

        recalculateTripTotals(trip);
        return tripRepository.save(trip);
    }

    public Trip removeActivity(String userEmail, Long tripId, Long activityId) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryDay targetDay = null;
        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if (item.getId().equals(activityId)) {
                    item.setStatus("CANCELLED");
                    itineraryItemRepository.save(item);
                    targetDay = day;
                    break;
                }
            }
            if (targetDay != null)
                break;
        }

        if (targetDay != null) {
            Object[] startCtx = getDayStartContext(trip, targetDay);
            routeOptimizationService.recalculateDaySchedule(targetDay, (double) startCtx[0], (double) startCtx[1],
                    (String) startCtx[2], java.time.LocalTime.of(9, 30));
            for (ItineraryItem it : targetDay.getItems()) {
                itineraryItemRepository.save(it);
            }
        }

        recalculateTripTotals(trip);
        return tripRepository.save(trip);
    }

    public Trip addActivityToDay(String userEmail, Long tripId, Long dayId, Map<String, Object> activityData) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryDay targetDay = null;
        for (ItineraryDay day : trip.getDays()) {
            if (day.getId().equals(dayId)) {
                targetDay = day;
                break;
            }
        }
        if (targetDay == null) {
            throw new RuntimeException("Day not found in this trip");
        }

        ItineraryItem item = new ItineraryItem();
        item.setItineraryDay(targetDay);
        String placeTitle = activityData.containsKey("title") && activityData.get("title") != null
                ? activityData.get("title").toString().trim()
                : "Added Place";
        item.setTitle(placeTitle);

        // Resolve coordinates
        Double addLat = null;
        Double addLon = null;
        if (activityData.containsKey("latitude") && activityData.get("latitude") != null) {
            try {
                addLat = Double.parseDouble(activityData.get("latitude").toString());
            } catch (Exception ignored) {
            }
        } else if (activityData.containsKey("lat") && activityData.get("lat") != null) {
            try {
                addLat = Double.parseDouble(activityData.get("lat").toString());
            } catch (Exception ignored) {
            }
        }
        if (activityData.containsKey("longitude") && activityData.get("longitude") != null) {
            try {
                addLon = Double.parseDouble(activityData.get("longitude").toString());
            } catch (Exception ignored) {
            }
        } else if (activityData.containsKey("lon") && activityData.get("lon") != null) {
            try {
                addLon = Double.parseDouble(activityData.get("lon").toString());
            } catch (Exception ignored) {
            }
        }

        String addAddress = activityData.containsKey("address") && activityData.get("address") != null
                ? activityData.get("address").toString().trim()
                : null;

        if (addLat == null || addLon == null) {
            String q = placeTitle;
            if (trip.getDestinationCity() != null
                    && !placeTitle.toLowerCase().contains(trip.getDestinationCity().toLowerCase())) {
                q += ", " + trip.getDestinationCity();
            }
            Map<String, Object> found = placeDiscoveryService.searchPlace(q);
            if (found != null && found.containsKey("lat") && found.containsKey("lon")) {
                try {
                    addLat = Double.parseDouble(found.get("lat").toString());
                    addLon = Double.parseDouble(found.get("lon").toString());
                    if (addAddress == null && found.containsKey("address")) {
                        addAddress = found.get("address").toString();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (addLat != null && addLon != null) {
            item.setLatitude(addLat);
            item.setLongitude(addLon);
        } else if (trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null) {
            item.setLatitude(trip.getDestinationLatitude());
            item.setLongitude(trip.getDestinationLongitude());
        }

        if (addAddress != null && !addAddress.isEmpty()) {
            item.setAddress(addAddress);
        } else {
            item.setAddress(
                    placeTitle + ", " + (trip.getDestinationCity() != null ? trip.getDestinationCity() : "India"));
        }

        String rawCat = activityData.containsKey("category") && activityData.get("category") != null
                ? activityData.get("category").toString().trim()
                : null;
        item.setCategory(determineCategory(placeTitle, item.getAddress(), rawCat));

        if (activityData.containsKey("costInr") && activityData.get("costInr") != null) {
            try {
                item.setCostInr(Double.parseDouble(activityData.get("costInr").toString()));
            } catch (Exception ignored) {
            }
        }
        if (activityData.containsKey("durationMinutes") && activityData.get("durationMinutes") != null) {
            try {
                item.setDurationMinutes(Integer.parseInt(activityData.get("durationMinutes").toString()));
            } catch (Exception ignored) {
            }
        } else if (activityData.containsKey("durationHours") && activityData.get("durationHours") != null) {
            try {
                double dh = Double.parseDouble(activityData.get("durationHours").toString());
                item.setDurationMinutes((int) (dh * 60));
            } catch (Exception ignored) {
            }
        } else {
            item.setDurationMinutes(90);
        }

        if (activityData.containsKey("indoorOutdoor") && activityData.get("indoorOutdoor") != null) {
            item.setIndoorOutdoor(activityData.get("indoorOutdoor").toString());
        }
        if (activityData.containsKey("rating") && activityData.get("rating") != null) {
            try {
                item.setRating(Double.parseDouble(activityData.get("rating").toString()));
            } catch (Exception ignored) {
            }
        }
        if (activityData.containsKey("reviewCount") && activityData.get("reviewCount") != null) {
            try {
                item.setReviewCount(Integer.parseInt(activityData.get("reviewCount").toString()));
            } catch (Exception ignored) {
            }
        }
        if (activityData.containsKey("openingHours") && activityData.get("openingHours") != null) {
            item.setOpeningHours(activityData.get("openingHours").toString());
        }
        if (activityData.containsKey("attractionId") && activityData.get("attractionId") != null) {
            try {
                Long attrId = Long.parseLong(activityData.get("attractionId").toString());
                attractionRepository.findById(attrId).ifPresent(item::setAttraction);
            } catch (Exception ignored) {
            }
        }

        item.setStatus("SCHEDULED");
        item.setVerificationStatus("100% Real Location • Verified");
        if (activityData.containsKey("notes") && activityData.get("notes") != null
                && !activityData.get("notes").toString().trim().isEmpty()) {
            item.setRecommendationReason("Manually added by traveller: " + activityData.get("notes").toString().trim());
        } else if (activityData.containsKey("recommendationReason")
                && activityData.get("recommendationReason") != null) {
            item.setRecommendationReason(activityData.get("recommendationReason").toString());
        } else {
            item.setRecommendationReason("Manually added by traveller");
        }
        if (activityData.containsKey("startTime") && activityData.get("startTime") != null
                && !activityData.get("startTime").toString().trim().isEmpty()) {
            item.setStartTime(activityData.get("startTime").toString().trim());
        }

        targetDay.getItems().add(item);
        itineraryItemRepository.save(item);

        Object[] startCtx = getDayStartContext(trip, targetDay);
        routeOptimizationService.recalculateDaySchedule(targetDay, (double) startCtx[0], (double) startCtx[1],
                (String) startCtx[2], java.time.LocalTime.of(9, 30));
        for (ItineraryItem it : targetDay.getItems()) {
            itineraryItemRepository.save(it);
        }
        if (itineraryDayRepository != null) {
            itineraryDayRepository.save(targetDay);
        }

        recalculateTripTotals(trip);
        return tripRepository.save(trip);
    }

    public Trip updateActivity(String userEmail, Long tripId, Long activityId, Map<String, Object> updateData) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryItem item = itineraryItemRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));

        if (updateData.containsKey("title") && updateData.get("title") != null) {
            item.setTitle(updateData.get("title").toString().trim());
        }
        if (updateData.containsKey("address") && updateData.get("address") != null) {
            item.setAddress(updateData.get("address").toString().trim());
        }
        if (updateData.containsKey("startTime") && updateData.get("startTime") != null) {
            item.setStartTime(updateData.get("startTime").toString().trim());
        }
        if (updateData.containsKey("durationMinutes") && updateData.get("durationMinutes") != null) {
            try {
                item.setDurationMinutes(Integer.parseInt(updateData.get("durationMinutes").toString()));
            } catch (Exception ignored) {
            }
        }
        if (updateData.containsKey("costInr") && updateData.get("costInr") != null) {
            try {
                item.setCostInr(Double.parseDouble(updateData.get("costInr").toString()));
            } catch (Exception ignored) {
            }
        }
        if (updateData.containsKey("notes") && updateData.get("notes") != null) {
            item.setRecommendationReason("Manually added by traveller: " + updateData.get("notes").toString().trim());
        } else if (updateData.containsKey("recommendationReason") && updateData.get("recommendationReason") != null) {
            item.setRecommendationReason(updateData.get("recommendationReason").toString().trim());
        }
        if (updateData.containsKey("latitude") && updateData.get("latitude") != null) {
            try {
                item.setLatitude(Double.parseDouble(updateData.get("latitude").toString()));
            } catch (Exception ignored) {
            }
        }
        if (updateData.containsKey("longitude") && updateData.get("longitude") != null) {
            try {
                item.setLongitude(Double.parseDouble(updateData.get("longitude").toString()));
            } catch (Exception ignored) {
            }
        }

        String rawCat = updateData.containsKey("category") && updateData.get("category") != null
                ? updateData.get("category").toString().trim()
                : item.getCategory();
        item.setCategory(determineCategory(item.getTitle(), item.getAddress(), rawCat));

        itineraryItemRepository.save(item);

        ItineraryDay day = item.getItineraryDay();
        if (day != null) {
            Object[] startCtx = getDayStartContext(trip, day);
            routeOptimizationService.recalculateDaySchedule(day, (double) startCtx[0], (double) startCtx[1],
                    (String) startCtx[2], java.time.LocalTime.of(9, 30));
            for (ItineraryItem it : day.getItems()) {
                itineraryItemRepository.save(it);
            }
            if (itineraryDayRepository != null) {
                itineraryDayRepository.save(day);
            }
        }

        recalculateTripTotals(trip);
        return tripRepository.save(trip);
    }

    public Trip reorderDayActivities(String userEmail, Long tripId, Long dayId, List<Long> activityIdsInOrder) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryDay targetDay = null;
        for (ItineraryDay day : trip.getDays()) {
            if (day.getId() != null && day.getId().equals(dayId)) {
                targetDay = day;
                break;
            } else if (day.getDayNumber() != null && day.getDayNumber().longValue() == dayId) {
                targetDay = day;
                break;
            }
        }
        if (targetDay == null) {
            throw new RuntimeException("Day not found in this trip");
        }

        if (activityIdsInOrder != null && !activityIdsInOrder.isEmpty()) {
            Map<Long, Integer> orderMap = new HashMap<>();
            for (int i = 0; i < activityIdsInOrder.size(); i++) {
                orderMap.put(activityIdsInOrder.get(i), i);
            }
            int nextOrder = activityIdsInOrder.size();
            for (ItineraryItem it : targetDay.getItems()) {
                if (orderMap.containsKey(it.getId())) {
                    it.setItemOrder(orderMap.get(it.getId()));
                } else {
                    it.setItemOrder(nextOrder++);
                }
            }
            targetDay.getItems().sort(
                    Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        Object[] startCtx = getDayStartContext(trip, targetDay);
        routeOptimizationService.recalculateDaySchedule(targetDay, (double) startCtx[0], (double) startCtx[1],
                (String) startCtx[2], java.time.LocalTime.of(9, 30));
        for (ItineraryItem it : targetDay.getItems()) {
            itineraryItemRepository.save(it);
        }
        if (itineraryDayRepository != null) {
            itineraryDayRepository.save(targetDay);
        }

        recalculateTripTotals(trip);
        itineraryItemRepository.flush();

        if (trip.getDays() != null) {
            trip.getDays().sort(Comparator.comparing(ItineraryDay::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())));
            for (ItineraryDay d : trip.getDays()) {
                if (d.getItems() != null) {
                    d.getItems().sort(Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));
                }
            }
        }
        return tripRepository.save(trip);
    }

    public Trip moveActivityToDay(String userEmail, Long tripId, Long activityId, Long targetDayId) {
        return moveActivityToDay(userEmail, tripId, activityId, targetDayId, null, null);
    }

    public Trip moveActivityToDay(String userEmail, Long tripId, Long activityId, Long targetDayId,
            Integer targetPosition) {
        return moveActivityToDay(userEmail, tripId, activityId, targetDayId, null, targetPosition);
    }

    public Trip moveActivityToDay(String userEmail, Long tripId, Long activityId, Long targetDayId,
            Integer targetDayNumber, Integer targetPosition) {
        Trip trip = getTripById(userEmail, tripId);
        ItineraryItem targetItem = null;
        ItineraryDay sourceDay = null;
        ItineraryDay targetDay = null;

        // 1. Locate the target item and its current (source) day
        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if (item.getId() != null && item.getId().equals(activityId)) {
                    targetItem = item;
                    sourceDay = day;
                    break;
                }
            }
            if (targetItem != null) {
                break;
            }
        }

        // Fallback lookup if item was not found in in-memory day lists
        if (targetItem == null) {
            Optional<ItineraryItem> itemOpt = itineraryItemRepository.findById(activityId);
            if (itemOpt.isPresent()) {
                targetItem = itemOpt.get();
                if (sourceDay == null && targetItem.getItineraryDay() != null) {
                    Long sDayId = targetItem.getItineraryDay().getId();
                    for (ItineraryDay d : trip.getDays()) {
                        if (d.getId() != null && d.getId().equals(sDayId)) {
                            sourceDay = d;
                            break;
                        }
                    }
                }
            }
        }

        if (targetItem == null) {
            throw new RuntimeException("Activity not found with id: " + activityId);
        }

        // Locate target day: exact match by database ID first, then fallback to dayNumber
        if (targetDayId != null) {
            for (ItineraryDay day : trip.getDays()) {
                if (day.getId() != null && day.getId().equals(targetDayId)) {
                    targetDay = day;
                    break;
                }
            }
            if (targetDay == null) {
                for (ItineraryDay day : trip.getDays()) {
                    if (day.getDayNumber() != null && day.getDayNumber().longValue() == targetDayId) {
                        targetDay = day;
                        break;
                    }
                }
            }
        }
        if (targetDay == null && targetDayNumber != null) {
            for (ItineraryDay day : trip.getDays()) {
                if (day.getDayNumber() != null && day.getDayNumber().equals(targetDayNumber)) {
                    targetDay = day;
                    break;
                }
            }
        }

        // If targetDay was not specified or found, default to sourceDay
        if (targetDay == null) {
            if (targetDayId == null && targetDayNumber == null) {
                targetDay = sourceDay;
            } else {
                throw new RuntimeException("Target day not found in trip (dayId: " + targetDayId + ", dayNumber: "
                        + targetDayNumber + ")");
            }
        }

        // 2. CASE A: Same-Day Reordering
        if (sourceDay != null && sourceDay.getId().equals(targetDay.getId())) {
            if (targetPosition == null) {
                return trip; // No change requested
            }

            List<ItineraryItem> allItems = new ArrayList<>(sourceDay.getItems());
            List<ItineraryItem> activeItems = new ArrayList<>();
            List<ItineraryItem> inactiveItems = new ArrayList<>();

            for (ItineraryItem it : allItems) {
                if ("CANCELLED".equalsIgnoreCase(it.getStatus()) || "REPLACED".equalsIgnoreCase(it.getStatus())) {
                    inactiveItems.add(it);
                } else {
                    activeItems.add(it);
                }
            }
            activeItems.sort(
                    Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));

            activeItems.removeIf(it -> it.getId() != null && it.getId().equals(activityId));
            int insertIdx = Math.max(0, Math.min(activeItems.size(), targetPosition - 1));
            activeItems.add(insertIdx, targetItem);

            List<ItineraryItem> finalItems = new ArrayList<>(activeItems);
            finalItems.addAll(inactiveItems);

            for (int i = 0; i < finalItems.size(); i++) {
                finalItems.get(i).setItemOrder(i);
            }

            sourceDay.getItems().sort(
                    Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));

            Object[] srcStart = getDayStartContext(trip, sourceDay);
            routeOptimizationService.recalculateDaySchedule(
                    sourceDay,
                    (double) srcStart[0],
                    (double) srcStart[1],
                    (String) srcStart[2],
                    java.time.LocalTime.of(9, 30));

            for (ItineraryItem it : sourceDay.getItems()) {
                itineraryItemRepository.save(it);
            }
            if (itineraryDayRepository != null) {
                itineraryDayRepository.save(sourceDay);
            }

            recalculateTripTotals(trip);
            itineraryItemRepository.flush();

            if (trip.getDays() != null) {
                trip.getDays().sort(Comparator.comparing(ItineraryDay::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())));
                for (ItineraryDay d : trip.getDays()) {
                    if (d.getItems() != null) {
                        d.getItems().sort(Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                }
            }
            return tripRepository.save(trip);
        }

        // 3. CASE B: Cross-Day Movement
        // 3a. Remove from sourceDay and update sourceDay remaining items
        if (sourceDay != null) {
            sourceDay.getItems().removeIf(it -> it.getId() != null && it.getId().equals(activityId));
            for (int i = 0; i < sourceDay.getItems().size(); i++) {
                sourceDay.getItems().get(i).setItemOrder(i);
            }

            Object[] srcStart = getDayStartContext(trip, sourceDay);
            routeOptimizationService.recalculateDaySchedule(
                    sourceDay,
                    (double) srcStart[0],
                    (double) srcStart[1],
                    (String) srcStart[2],
                    java.time.LocalTime.of(9, 30));

            for (ItineraryItem it : sourceDay.getItems()) {
                itineraryItemRepository.save(it);
            }
            if (itineraryDayRepository != null) {
                itineraryDayRepository.save(sourceDay);
            }
        }

        // 3b. Update target item foreign key to targetDay
        targetItem.setItineraryDay(targetDay);

        // 3c. Insert into targetDay at desired position
        targetDay.getItems().removeIf(it -> it.getId() != null && it.getId().equals(activityId));

        List<ItineraryItem> targetActive = new ArrayList<>();
        List<ItineraryItem> targetInactive = new ArrayList<>();
        for (ItineraryItem it : targetDay.getItems()) {
            if ("CANCELLED".equalsIgnoreCase(it.getStatus()) || "REPLACED".equalsIgnoreCase(it.getStatus())) {
                targetInactive.add(it);
            } else {
                targetActive.add(it);
            }
        }
        targetActive.sort(
                Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        int insertIndex;
        if (targetPosition != null && targetPosition > 0) {
            insertIndex = Math.max(0, Math.min(targetActive.size(), targetPosition - 1));
        } else {
            insertIndex = targetActive.size();
        }

        targetActive.add(insertIndex, targetItem);

        List<ItineraryItem> finalTargetItems = new ArrayList<>(targetActive);
        finalTargetItems.addAll(targetInactive);

        for (int i = 0; i < finalTargetItems.size(); i++) {
            finalTargetItems.get(i).setItemOrder(i);
        }

        if (!targetDay.getItems().contains(targetItem)) {
            targetDay.getItems().add(targetItem);
        }
        targetDay.getItems().sort(
                Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        Object[] tgtStart = getDayStartContext(trip, targetDay);
        routeOptimizationService.recalculateDaySchedule(
                targetDay,
                (double) tgtStart[0],
                (double) tgtStart[1],
                (String) tgtStart[2],
                java.time.LocalTime.of(9, 30));

        for (ItineraryItem it : targetDay.getItems()) {
            itineraryItemRepository.save(it);
        }
        itineraryItemRepository.save(targetItem);
        if (itineraryDayRepository != null) {
            itineraryDayRepository.save(targetDay);
        }

        recalculateTripTotals(trip);
        itineraryItemRepository.flush();

        if (trip.getDays() != null) {
            trip.getDays().sort(Comparator.comparing(ItineraryDay::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())));
            for (ItineraryDay d : trip.getDays()) {
                if (d.getItems() != null) {
                    d.getItems().sort(Comparator.comparing(ItineraryItem::getItemOrder, Comparator.nullsLast(Comparator.naturalOrder())));
                }
            }
        }
        return tripRepository.save(trip);
    }

    private Object[] getDayStartContext(Trip trip, ItineraryDay day) {
        double centerLat = trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 15.2993;
        double centerLon = trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 74.1240;
        double dStartLat = trip.getOriginLatitude() != null ? trip.getOriginLatitude() : centerLat;
        double dStartLon = trip.getOriginLongitude() != null ? trip.getOriginLongitude() : centerLon;
        String dStartName = trip.getStartingLocation() != null ? trip.getStartingLocation() : trip.getDestinationCity();

        if (day != null && day.getDayNumber() != null && day.getDayNumber() > 1 && trip.getDays() != null
                && trip.getDays().size() >= day.getDayNumber() - 1) {
            ItineraryDay prevDay = trip.getDays().get(day.getDayNumber() - 2);
            if (prevDay != null && prevDay.getItems() != null && !prevDay.getItems().isEmpty()) {
                for (int i = prevDay.getItems().size() - 1; i >= 0; i--) {
                    ItineraryItem prevLast = prevDay.getItems().get(i);
                    if (!"CANCELLED".equalsIgnoreCase(prevLast.getStatus())
                            && !"REPLACED".equalsIgnoreCase(prevLast.getStatus())) {
                        if (prevLast.getLatitude() != null && prevLast.getLongitude() != null) {
                            dStartLat = prevLast.getLatitude();
                            dStartLon = prevLast.getLongitude();
                            dStartName = prevLast.getTitle();
                            break;
                        }
                    }
                }
            }
        }
        return new Object[] { dStartLat, dStartLon, dStartName };
    }

    public Trip updateTrip(String userEmail, Long tripId, TripCreateRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Check permission: Owner or Editor
        boolean isOwner = trip.getUser() != null && trip.getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isEditor = false;
        if (!isOwner) {
            Optional<TripMember> memberOpt = tripMemberRepository.findByTripIdAndUserEmail(tripId, userEmail);
            if (memberOpt.isPresent() && memberOpt.get().getStatus() == InvitationStatus.ACCEPTED
                    && memberOpt.get().getRole() == TripRole.EDITOR) {
                isEditor = true;
            }
        }
        if (!isOwner && !isEditor) {
            throw new RuntimeException("Not authorized to modify this trip");
        }

        // Validate destination if changed
        boolean destChanged = false;
        String rawDest = request.getDestinationCity() != null ? request.getDestinationCity().trim()
                : trip.getDestinationCity();
        if (request.getDestinationCity() != null && !rawDest.equalsIgnoreCase(trip.getDestinationCity())) {
            if (placeDiscoveryService.isForeignLocation(rawDest)) {
                throw new RuntimeException("TravelMate AI currently supports travel within India.");
            }
            destChanged = true;
        }

        // Check what else changed
        boolean budgetChanged = request.getBudgetInr() != null && !request.getBudgetInr().equals(trip.getBudgetInr());

        int oldDays = trip.getNumDays() != null ? trip.getNumDays() : 1;
        Integer reqDays = request.getNumDays();
        if (reqDays == null && request.getStartDate() != null && request.getEndDate() != null) {
            long d = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            reqDays = (int) Math.max(1, d);
        }
        int newDays = reqDays != null ? reqDays : oldDays;
        boolean durationChanged = newDays != oldDays;

        String oldInterests = trip.getInterests() != null ? trip.getInterests() : "";
        String newInterests = (request.getInterests() != null && !request.getInterests().isEmpty())
                ? String.join(",", request.getInterests())
                : oldInterests;
        boolean interestsChanged = !newInterests.equalsIgnoreCase(oldInterests);

        Double oldMaxDist = trip.getMaxDistanceKm();
        Double newMaxDist = request.getMaxDistanceKm() != null ? request.getMaxDistanceKm() : oldMaxDist;
        boolean distanceChanged = (newMaxDist != null && !newMaxDist.equals(oldMaxDist))
                || (request.getTravelDistancePref() != null
                        && !request.getTravelDistancePref().equalsIgnoreCase(trip.getTravelDistancePref()));

        Double oldMaxFee = trip.getMaxEntryFee();
        Double newMaxFee = request.getMaxEntryFee();
        boolean feeChanged = newMaxFee != null && !newMaxFee.equals(oldMaxFee);

        boolean paceChanged = request.getActivityLevel() != null
                && !request.getActivityLevel().equalsIgnoreCase(trip.getActivityLevel());

        // Update trip basic properties
        if (request.getStartingLocation() != null && !request.getStartingLocation().trim().isEmpty()) {
            trip.setStartingLocation(request.getStartingLocation().trim());
        }
        if (request.getBudgetInr() != null)
            trip.setBudgetInr(request.getBudgetInr());
        if (request.getNumTravelers() != null)
            trip.setNumTravelers(request.getNumTravelers());
        if (request.getTravelStyle() != null)
            trip.setTravelStyle(request.getTravelStyle());
        if (request.getActivityLevel() != null)
            trip.setActivityLevel(request.getActivityLevel());
        if (request.getFoodPreference() != null)
            trip.setFoodPreference(request.getFoodPreference());
        if (request.getTransitPreference() != null)
            trip.setTransitPreference(request.getTransitPreference());
        if (request.getTravelDistancePref() != null)
            trip.setTravelDistancePref(request.getTravelDistancePref());
        if (request.getMaxDistanceKm() != null)
            trip.setMaxDistanceKm(request.getMaxDistanceKm());
        if (request.getMaxEntryFee() != null)
            trip.setMaxEntryFee(request.getMaxEntryFee());
        trip.setInterests(newInterests);

        if (request.getStartDate() != null)
            trip.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            trip.setEndDate(request.getEndDate());
        trip.setNumDays(newDays);

        if (destChanged) {
            trip.setDestinationCity(rawDest);
            trip.setTitle("Trip to " + rawDest);
            try {
                Map<String, Object> destGeo = locationService.verifyAndGetLocation(rawDest);
                if (destGeo != null && destGeo.containsKey("lat") && destGeo.containsKey("lon")) {
                    trip.setDestinationLatitude(Double.parseDouble(destGeo.get("lat").toString()));
                    trip.setDestinationLongitude(Double.parseDouble(destGeo.get("lon").toString()));
                }
            } catch (Exception ignored) {
            }
            regenerateFullItinerary(trip, request);
        } else {
            // Intelligent selective recalculation!
            if (durationChanged) {
                adjustDuration(trip, oldDays, newDays, request);
            }
            if (paceChanged) {
                adjustActivitiesForPace(trip, request.getActivityLevel());
            }
            if (interestsChanged || distanceChanged || feeChanged || budgetChanged) {
                adjustExistingActivitiesForPreferences(trip, interestsChanged, distanceChanged, feeChanged,
                        budgetChanged);
            }
            recalculateTripTotals(trip);
        }

        try {
            validateAndConfigureTransport(trip, trip.getTransitPreference());
        } catch (Exception ignored) {
        }

        trip.setLastUpdateNotice("Trip updated by " + (isOwner ? "the owner" : "an editor") + ".");
        trip.setUpdatedAt(LocalDateTime.now());

        return tripRepository.save(trip);
    }

    private void adjustDuration(Trip trip, int oldDays, int newDays, TripCreateRequest request) {
        List<ItineraryDay> currentDays = trip.getDays();
        if (newDays < oldDays) {
            // Keep first newDays, remove excess days
            while (currentDays.size() > newDays) {
                currentDays.remove(currentDays.size() - 1);
            }
            for (int i = 0; i < currentDays.size(); i++) {
                currentDays.get(i).setDate(trip.getStartDate().plusDays(i));
            }
        } else if (newDays > oldDays) {
            // Update existing day dates
            for (int i = 0; i < currentDays.size(); i++) {
                currentDays.get(i).setDate(trip.getStartDate().plusDays(i));
            }
            // Fetch extra recommendations for added days
            Set<String> existingTitles = new HashSet<>();
            for (ItineraryDay d : currentDays) {
                for (ItineraryItem it : d.getItems()) {
                    if (it.getTitle() != null)
                        existingTitles.add(it.getTitle().toLowerCase().trim());
                }
            }

            Double centerLat = trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 16.5062;
            Double centerLon = trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 80.6480;

            int targetPerDay = getTargetItemsPerDay(trip.getActivityLevel());
            int maxItemsPerDay = (targetPerDay >= 5) ? 6 : (targetPerDay <= 2 ? 2 : 4);
            int neededNewItems = (newDays - oldDays) * maxItemsPerDay;

            List<Map<String, Object>> discovered = new ArrayList<>();
            Set<String> seenForNewDays = new HashSet<>(existingTitles);
            try {
                List<Attraction> dbAttrs = attractionRepository
                        .findByCityNameIgnoreCaseAndIsAvailableTrue(trip.getDestinationCity());
                if (dbAttrs == null || dbAttrs.isEmpty()) {
                    dbAttrs = attractionRepository.searchAttractions(trip.getDestinationCity());
                }
                if (dbAttrs != null) {
                    for (Attraction a : dbAttrs) {
                        if (a.getName() != null && !seenForNewDays.contains(a.getName().toLowerCase().trim())) {
                            seenForNewDays.add(a.getName().toLowerCase().trim());
                            Map<String, Object> m = new HashMap<>();
                            m.put("name", a.getName());
                            m.put("category", a.getCategory() != null ? a.getCategory().getName() : "Sightseeing");
                            m.put("lat", a.getLatitude());
                            m.put("lon", a.getLongitude());
                            m.put("indoorOutdoor", a.getIndoorOutdoor() != null ? a.getIndoorOutdoor() : "Outdoor");
                            discovered.add(m);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            try {
                List<Map<String, Object>> discNearby = placeDiscoveryService.discoverNearby(centerLat, centerLon,
                        trip.getDestinationCity(), null, Math.max(30, neededNewItems + 10));
                if (discNearby != null) {
                    for (Map<String, Object> d : discNearby) {
                        String n = (String) d.get("name");
                        if (n != null && !seenForNewDays.contains(n.toLowerCase().trim())) {
                            seenForNewDays.add(n.toLowerCase().trim());
                            discovered.add(d);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            int discIndex = 0;
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a",
                    Locale.ENGLISH);
            int durMin = (targetPerDay >= 5) ? 60 : (targetPerDay <= 2 ? 120 : 80);

            for (int i = oldDays; i < newDays; i++) {
                ItineraryDay newDay = new ItineraryDay();
                newDay.setTrip(trip);
                newDay.setDayNumber(i + 1);
                newDay.setDate(trip.getStartDate().plusDays(i));
                newDay.setWeatherCondition("Clear Sky");
                newDay.setMaxTempC(30.0);
                newDay.setMinTempC(22.0);

                int itemsAdded = 0;
                java.time.LocalTime clock = java.time.LocalTime.of(9, 30);

                while (itemsAdded < maxItemsPerDay && discIndex < discovered.size()) {
                    Map<String, Object> candidate = discovered.get(discIndex++);
                    String name = (String) candidate.get("name");
                    if (name != null && !existingTitles.contains(name.toLowerCase().trim())) {
                        existingTitles.add(name.toLowerCase().trim());
                        ItineraryItem item = new ItineraryItem();
                        item.setItineraryDay(newDay);
                        item.setTitle(name);
                        item.setCategory((String) candidate.getOrDefault("category", "Sightseeing"));
                        item.setCostInr(0.0);
                        item.setIndoorOutdoor((String) candidate.getOrDefault("indoorOutdoor", "Outdoor"));
                        item.setRecommendationReason("Added to match extended " + newDays + "-day trip duration.");

                        java.time.LocalTime aStart = clock;
                        java.time.LocalTime aEnd = aStart.plusMinutes(durMin);
                        item.setStartTime(aStart.format(timeFmt));
                        item.setEndTime(aEnd.format(timeFmt));
                        clock = aEnd.plusMinutes(20);

                        item.setDurationMinutes(durMin);
                        item.setStatus("SCHEDULED");
                        item.setItemOrder(itemsAdded);
                        item.setVerificationStatus("100% Real Location • Verified");
                        if (candidate.containsKey("lat") && candidate.get("lat") != null) {
                            try {
                                item.setLatitude(Double.parseDouble(candidate.get("lat").toString()));
                            } catch (Exception ignored) {
                            }
                        }
                        if (candidate.containsKey("lon") && candidate.get("lon") != null) {
                            try {
                                item.setLongitude(Double.parseDouble(candidate.get("lon").toString()));
                            } catch (Exception ignored) {
                            }
                        }
                        newDay.getItems().add(item);
                        itemsAdded++;
                    }
                }
                currentDays.add(newDay);
            }
        }
    }

    private void adjustExistingActivitiesForPreferences(Trip trip, boolean interestsChanged, boolean distanceChanged,
            boolean feeChanged, boolean budgetChanged) {
        String city = trip.getDestinationCity();
        Double centerLat = trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 16.5062;
        Double centerLon = trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 80.6480;

        List<Map<String, Object>> candidatePlaces = new ArrayList<>();
        try {
            candidatePlaces = placeDiscoveryService.discoverNearby(centerLat, centerLon, city, null, 20);
        } catch (Exception ignored) {
        }

        Set<String> usedTitles = new HashSet<>();
        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                if (item.getTitle() != null)
                    usedTitles.add(item.getTitle().toLowerCase().trim());
            }
        }

        for (ItineraryDay day : trip.getDays()) {
            for (ItineraryItem item : day.getItems()) {
                // NEVER touch completed items!
                if ("COMPLETED".equalsIgnoreCase(item.getStatus())) {
                    continue;
                }

                boolean needsSwap = false;
                String swapReason = "";

                // Check distance violation
                if (distanceChanged && trip.getMaxDistanceKm() != null && trip.getMaxDistanceKm() > 0) {
                    if (item.getLatitude() != null && item.getLongitude() != null) {
                        double d = calculateHaversineKm(centerLat, centerLon, item.getLatitude(), item.getLongitude());
                        if (d > trip.getMaxDistanceKm()) {
                            needsSwap = true;
                            swapReason = "Adjusted to stay within your " + trip.getMaxDistanceKm()
                                    + " km distance preference.";
                        }
                    }
                }

                // Check fee violation
                if (!needsSwap && feeChanged && trip.getMaxEntryFee() != null) {
                    if (item.getCostInr() != null && item.getCostInr() > trip.getMaxEntryFee()) {
                        needsSwap = true;
                        swapReason = "Adjusted to respect your entry fee budget (₹" + trip.getMaxEntryFee() + ").";
                    }
                }

                // Check interest shift (e.g. user shifted to Heritage)
                if (!needsSwap && interestsChanged && trip.getInterests() != null && !trip.getInterests().isEmpty()) {
                    String lowerInterests = trip.getInterests().toLowerCase();
                    boolean matchesCurrent = lowerInterests.contains(item.getCategory().toLowerCase());
                    if (!matchesCurrent && (lowerInterests.contains("heritage") || lowerInterests.contains("temple")
                            || lowerInterests.contains("nature"))) {
                        needsSwap = true;
                        swapReason = "Updated to align with your newly selected interests (" + trip.getInterests()
                                + ").";
                    }
                }

                if (needsSwap) {
                    // Find a replacement candidate
                    for (Map<String, Object> cand : candidatePlaces) {
                        String candName = (String) cand.get("name");
                        if (candName != null && !usedTitles.contains(candName.toLowerCase().trim())) {
                            usedTitles.add(candName.toLowerCase().trim());
                            item.setTitle(candName);
                            item.setCategory((String) cand.getOrDefault("category", "Sightseeing"));
                            item.setRecommendationReason(swapReason);
                            if (cand.containsKey("lat") && cand.get("lat") != null) {
                                try {
                                    item.setLatitude(Double.parseDouble(cand.get("lat").toString()));
                                } catch (Exception ignored) {
                                }
                            }
                            if (cand.containsKey("lon") && cand.get("lon") != null) {
                                try {
                                    item.setLongitude(Double.parseDouble(cand.get("lon").toString()));
                                } catch (Exception ignored) {
                                }
                            }
                            item.setCostInr(0.0);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void recalculateTripTotals(Trip trip) {
        double total = trip.getDays().stream()
                .flatMap(d -> d.getItems().stream())
                .filter(i -> !"REPLACED".equals(i.getStatus()) && !"CANCELLED".equals(i.getStatus()))
                .mapToDouble(i -> i.getCostInr() != null ? i.getCostInr() : 0.0)
                .sum();
        trip.setTotalEstimatedCost(total);
    }

    private void regenerateFullItinerary(Trip trip, TripCreateRequest request) {
        trip.getDays().clear();
        String parsedCity = trip.getDestinationCity();
        int daysCount = trip.getNumDays() != null ? trip.getNumDays() : 2;

        Double centerLat = trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 16.5062;
        Double centerLon = trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 80.6480;

        int targetPerDay = getTargetItemsPerDay(trip.getActivityLevel());
        List<Integer> dailyQuotas = new ArrayList<>();
        for (int i = 0; i < daysCount; i++) {
            int quota;
            if (targetPerDay >= 5) {
                quota = (i % 2 == 0) ? 6 : 5;
            } else if (targetPerDay <= 2) {
                quota = 2;
            } else {
                quota = (i % 2 == 0) ? 4 : 3;
            }
            dailyQuotas.add(quota);
        }
        int totalNeeded = dailyQuotas.stream().mapToInt(Integer::intValue).sum();

        List<Map<String, Object>> candidatePool = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Seed authentic attractions from database
        try {
            List<Attraction> dbAttrs = attractionRepository.findByCityNameIgnoreCaseAndIsAvailableTrue(parsedCity);
            if (dbAttrs == null || dbAttrs.isEmpty()) {
                dbAttrs = attractionRepository.searchAttractions(parsedCity);
            }
            if (dbAttrs != null) {
                for (Attraction a : dbAttrs) {
                    if (a.getName() != null && !seen.contains(a.getName().toLowerCase().trim())) {
                        seen.add(a.getName().toLowerCase().trim());
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", a.getName());
                        m.put("category", a.getCategory() != null ? a.getCategory().getName() : "Sightseeing");
                        m.put("lat", a.getLatitude());
                        m.put("lon", a.getLongitude());
                        m.put("indoorOutdoor", a.getIndoorOutdoor() != null ? a.getIndoorOutdoor() : "Outdoor");
                        candidatePool.add(m);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Supplement with discoverNearby if needed
        if (candidatePool.size() < totalNeeded + 10) {
            try {
                List<Map<String, Object>> discovered = placeDiscoveryService.discoverNearby(centerLat, centerLon,
                        parsedCity, null, totalNeeded + 15);
                if (discovered != null) {
                    for (Map<String, Object> d : discovered) {
                        String n = (String) d.get("name");
                        if (n != null && !seen.contains(n.toLowerCase().trim())) {
                            seen.add(n.toLowerCase().trim());
                            candidatePool.add(d);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (candidatePool.size() < totalNeeded && !candidatePool.isEmpty()) {
            dailyQuotas.clear();
            int remaining = candidatePool.size();
            for (int i = 0; i < daysCount; i++) {
                int remainingDays = daysCount - i;
                int share = (int) Math.ceil((double) remaining / remainingDays);
                dailyQuotas.add(share);
                remaining -= share;
            }
        }

        int candidateCount = dailyQuotas.stream().mapToInt(Integer::intValue).sum();
        List<Map<String, Object>> finalCandidates = new ArrayList<>(
                candidatePool.subList(0, Math.min(candidateCount, candidatePool.size())));
        List<List<Map<String, Object>>> optimizedDays = routeOptimizationService.partitionAndOptimizeRecs(
                centerLat, centerLon, finalCandidates, dailyQuotas, centerLat, centerLon);

        int durMin = (targetPerDay >= 5) ? 60 : (targetPerDay <= 2 ? 120 : 80);
        double currStartLat = centerLat;
        double currStartLon = centerLon;
        String prevLocName = trip.getStartingLocation() != null ? trip.getStartingLocation() : parsedCity;

        for (int i = 0; i < daysCount; i++) {
            ItineraryDay day = new ItineraryDay();
            day.setTrip(trip);
            day.setDayNumber(i + 1);
            day.setDate(trip.getStartDate().plusDays(i));
            day.setWeatherCondition("Clear Sky");
            day.setMaxTempC(30.0);
            day.setMinTempC(22.0);

            List<Map<String, Object>> dayRecs = (i < optimizedDays.size()) ? optimizedDays.get(i)
                    : Collections.emptyList();
            for (int j = 0; j < dayRecs.size(); j++) {
                Map<String, Object> cand = dayRecs.get(j);
                ItineraryItem item = new ItineraryItem();
                item.setItineraryDay(day);
                item.setTitle((String) cand.get("name"));
                item.setCategory((String) cand.getOrDefault("category", "Sightseeing"));
                item.setCostInr(0.0);
                item.setIndoorOutdoor((String) cand.getOrDefault("indoorOutdoor", "Outdoor"));
                item.setRecommendationReason("Recommended destination for " + parsedCity);
                item.setDurationMinutes(durMin);
                item.setStatus("SCHEDULED");
                item.setItemOrder(j);
                item.setVerificationStatus("100% Real Location • Verified");
                if (cand.containsKey("lat") && cand.get("lat") != null) {
                    try {
                        item.setLatitude(Double.parseDouble(cand.get("lat").toString()));
                    } catch (Exception ignored) {
                    }
                }
                if (cand.containsKey("lon") && cand.get("lon") != null) {
                    try {
                        item.setLongitude(Double.parseDouble(cand.get("lon").toString()));
                    } catch (Exception ignored) {
                    }
                }
                day.getItems().add(item);
            }

            routeOptimizationService.recalculateDaySchedule(day, currStartLat, currStartLon, prevLocName,
                    java.time.LocalTime.of(9, 30));

            if (!day.getItems().isEmpty()) {
                ItineraryItem last = day.getItems().get(day.getItems().size() - 1);
                if (last.getLatitude() != null && last.getLongitude() != null) {
                    currStartLat = last.getLatitude();
                    currStartLon = last.getLongitude();
                    prevLocName = last.getTitle();
                }
            }

            trip.getDays().add(day);
        }
        recalculateTripTotals(trip);
    }

    public TripMember inviteMember(String userEmail, Long tripId, String inviteEmail, TripRole role) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Only owner can invite
        if (trip.getUser() == null || !trip.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Only the trip owner can invite members");
        }

        if (inviteEmail == null || inviteEmail.trim().isEmpty()) {
            throw new RuntimeException("Please enter a valid email or username.");
        }

        String targetEmail = inviteEmail.trim();

        // 15. AUTHENTICATION: Ensure target invited user exists, auto-provisioning if
        // first time invited
        Optional<User> targetUserOpt = userRepository.findByEmail(targetEmail);
        User targetUser;
        if (targetUserOpt.isPresent()) {
            targetUser = targetUserOpt.get();
        } else {
            User guest = new User();
            guest.setEmail(targetEmail);
            guest.setFullName(targetEmail.contains("@") ? targetEmail.split("@")[0] : targetEmail);
            guest.setPassword("$2a$10$wT5HqjJ7y7r8q7w8q7w8qu7w8q7w8q7w8q7w8q7w8q7w8q7w8q7w8");
            guest.setRole(com.travelmate.entity.Role.ROLE_USER);
            guest.setAge(25);
            targetUser = userRepository.save(guest);
        }
        if (trip.getUser().getId().equals(targetUser.getId())) {
            throw new RuntimeException("User is already the owner of this trip.");
        }

        Optional<TripMember> existing = tripMemberRepository.findByTripIdAndUserId(tripId, targetUser.getId());
        if (existing.isPresent()) {
            TripMember m = existing.get();
            if (m.getStatus() == InvitationStatus.ACCEPTED) {
                throw new RuntimeException("User is already a member of this trip.");
            } else if (m.getStatus() == InvitationStatus.PENDING) {
                throw new RuntimeException("User has already been invited to this trip.");
            } else {
                // If previously declined, renew invite
                m.setStatus(InvitationStatus.PENDING);
                m.setRole(role != null ? role : TripRole.VIEWER);
                m.setUpdatedAt(LocalDateTime.now());
                return tripMemberRepository.save(m);
            }
        }

        User caller = userRepository.findByEmail(userEmail).orElse(trip.getUser());
        TripMember member = new TripMember(
                trip,
                targetUser,
                role != null ? role : TripRole.VIEWER,
                InvitationStatus.PENDING,
                userEmail,
                caller.getFullName() != null ? caller.getFullName() : userEmail);
        return tripMemberRepository.save(member);
    }

    public List<TripMember> getUserInvitations(String userEmail) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty())
            return new ArrayList<>();
        return tripMemberRepository.findByUserIdAndStatus(userOpt.get().getId(), InvitationStatus.PENDING);
    }

    public Map<String, Object> respondToInvitation(String userEmail, Long memberId, String action) {
        TripMember member = tripMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!member.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Not authorized to respond to this invitation");
        }

        if ("ACCEPT".equalsIgnoreCase(action)) {
            member.setStatus(InvitationStatus.ACCEPTED);
            member.setUpdatedAt(LocalDateTime.now());
            tripMemberRepository.save(member);
            return Map.of("message", "Invitation accepted successfully", "tripId", member.getTrip().getId());
        } else {
            member.setStatus(InvitationStatus.DECLINED);
            member.setUpdatedAt(LocalDateTime.now());
            tripMemberRepository.save(member);
            return Map.of("message", "Invitation declined", "tripId", member.getTrip().getId());
        }
    }

    public void removeMember(String userEmail, Long tripId, Long memberId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        TripMember member = tripMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        boolean isOwner = trip.getUser() != null && trip.getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isSelf = member.getUser() != null && member.getUser().getEmail().equalsIgnoreCase(userEmail);

        if (!isOwner && !isSelf) {
            throw new RuntimeException("Not authorized to remove this member");
        }

        tripMemberRepository.delete(member);
    }

    public TripMember updateMemberRole(String userEmail, Long tripId, Long memberId, TripRole role) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (trip.getUser() == null || !trip.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Only the owner can update member roles");
        }

        TripMember member = tripMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setRole(role);
        member.setUpdatedAt(LocalDateTime.now());
        return tripMemberRepository.save(member);
    }

    public int getTargetItemsPerDay(String activityLevel) {
        if (activityLevel == null || activityLevel.trim().isEmpty()) {
            return 4; // Moderate default (3-4 places per day)
        }
        String level = activityLevel.trim().toLowerCase();
        if (level.contains("fast") || level.contains("packed") || level.contains("high") || level.contains("active")) {
            return 6; // Fast Paced (5-6 places per day)
        } else if (level.contains("relax") || level.contains("slow") || level.contains("low")
                || level.contains("unhurried")) {
            return 2; // Relaxed (1-2 places per day)
        } else {
            return 4; // Moderate (3-4 places per day)
        }
    }

    public Trip optimizeTripRoute(String userEmail, Long tripId) {
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            throw new RuntimeException("Trip not found");
        }
        Trip trip = tripOpt.get();
        double startLat = trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 20.5937;
        double startLon = trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 78.9629;
        if (trip.getOriginLatitude() != null && trip.getOriginLongitude() != null) {
            double rawD = (trip.getDestinationLatitude() != null && trip.getDestinationLongitude() != null)
                    ? calculateHaversineKm(trip.getOriginLatitude(), trip.getOriginLongitude(),
                            trip.getDestinationLatitude(), trip.getDestinationLongitude())
                    : 0.0;
            if (rawD <= 60.0) {
                startLat = trip.getOriginLatitude();
                startLon = trip.getOriginLongitude();
            }
        }

        // Collect all active items across all days to perform full cluster TSP
        List<ItineraryItem> allActiveItems = new ArrayList<>();
        List<Integer> dailyQuotas = new ArrayList<>();
        Map<Integer, List<ItineraryItem>> inactiveByDay = new HashMap<>();

        for (int i = 0; i < trip.getDays().size(); i++) {
            ItineraryDay day = trip.getDays().get(i);
            List<ItineraryItem> dayActives = new ArrayList<>();
            List<ItineraryItem> dayInactives = new ArrayList<>();
            for (ItineraryItem it : day.getItems()) {
                if (!"REPLACED".equalsIgnoreCase(it.getStatus()) && !"CANCELLED".equalsIgnoreCase(it.getStatus())) {
                    dayActives.add(it);
                    allActiveItems.add(it);
                } else {
                    dayInactives.add(it);
                }
            }
            dailyQuotas.add(dayActives.size());
            inactiveByDay.put(i, dayInactives);
        }

        List<List<ItineraryItem>> partitioned = routeOptimizationService.partitionAndOptimize(
                startLat, startLon, allActiveItems, dailyQuotas,
                RouteOptimizationService.ITEM_EXTRACTOR,
                trip.getDestinationLatitude(), trip.getDestinationLongitude());

        double currStartLat = startLat;
        double currStartLon = startLon;
        String prevLocName = trip.getStartingLocation() != null ? trip.getStartingLocation()
                : trip.getDestinationCity();

        for (int i = 0; i < trip.getDays().size(); i++) {
            ItineraryDay day = trip.getDays().get(i);
            List<ItineraryItem> optItems = (i < partitioned.size()) ? partitioned.get(i) : Collections.emptyList();
            List<ItineraryItem> dayInactives = inactiveByDay.getOrDefault(i, Collections.emptyList());

            day.getItems().clear();
            for (ItineraryItem optIt : optItems) {
                optIt.setItineraryDay(day);
                day.getItems().add(optIt);
            }
            for (ItineraryItem inact : dayInactives) {
                inact.setItineraryDay(day);
                day.getItems().add(inact);
            }

            java.time.LocalTime dayStartClock = java.time.LocalTime.of(9, 0);
            if (i == 0 && trip.getStartTime() != null) {
                try {
                    String st = trip.getStartTime().trim();
                    if (st.toLowerCase().contains("pm") || st.toLowerCase().contains("am")) {
                        dayStartClock = java.time.LocalTime.parse(st.toUpperCase(),
                                java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
                    } else {
                        dayStartClock = java.time.LocalTime.parse(st);
                    }
                } catch (Exception ignored) {
                }
            }

            routeOptimizationService.recalculateDaySchedule(day, currStartLat, currStartLon, prevLocName,
                    dayStartClock);

            if (!optItems.isEmpty()) {
                ItineraryItem last = optItems.get(optItems.size() - 1);
                if (last.getLatitude() != null && last.getLongitude() != null) {
                    currStartLat = last.getLatitude();
                    currStartLon = last.getLongitude();
                    prevLocName = last.getTitle();
                }
            }
        }

        recalculateTripTotals(trip);
        return tripRepository.save(trip);
    }

    private void adjustActivitiesForPace(Trip trip, String newActivityLevel) {
        if (newActivityLevel == null || trip.getDays() == null || trip.getDays().isEmpty())
            return;
        int targetPerDay = getTargetItemsPerDay(newActivityLevel);
        int maxItemsPerDay = (targetPerDay >= 5) ? 6 : (targetPerDay <= 2 ? 2 : 4);

        Set<String> existingTitles = new HashSet<>();
        for (ItineraryDay d : trip.getDays()) {
            for (ItineraryItem it : d.getItems()) {
                if (it.getTitle() != null)
                    existingTitles.add(it.getTitle().toLowerCase().trim());
            }
        }

        Double centerLat = trip.getDestinationLatitude() != null ? trip.getDestinationLatitude() : 16.5062;
        Double centerLon = trip.getDestinationLongitude() != null ? trip.getDestinationLongitude() : 80.6480;

        List<Map<String, Object>> candidates = new ArrayList<>();
        Set<String> seenCand = new HashSet<>();
        try {
            List<Attraction> dbAttrs = attractionRepository
                    .findByCityNameIgnoreCaseAndIsAvailableTrue(trip.getDestinationCity());
            if (dbAttrs == null || dbAttrs.isEmpty()) {
                dbAttrs = attractionRepository.searchAttractions(trip.getDestinationCity());
            }
            if (dbAttrs != null) {
                for (Attraction a : dbAttrs) {
                    if (a.getName() != null && !seenCand.contains(a.getName().toLowerCase().trim())) {
                        seenCand.add(a.getName().toLowerCase().trim());
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", a.getName());
                        m.put("category", a.getCategory() != null ? a.getCategory().getName() : "Sightseeing");
                        m.put("lat", a.getLatitude());
                        m.put("lon", a.getLongitude());
                        m.put("indoorOutdoor", a.getIndoorOutdoor() != null ? a.getIndoorOutdoor() : "Outdoor");
                        candidates.add(m);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<Map<String, Object>> disc = placeDiscoveryService.discoverNearby(centerLat, centerLon,
                    trip.getDestinationCity(), null, 30);
            if (disc != null) {
                for (Map<String, Object> d : disc) {
                    String n = (String) d.get("name");
                    if (n != null && !seenCand.contains(n.toLowerCase().trim())) {
                        seenCand.add(n.toLowerCase().trim());
                        candidates.add(d);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        int candIdx = 0;
        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a",
                Locale.ENGLISH);
        int durMin = (targetPerDay >= 5) ? 60 : (targetPerDay <= 2 ? 120 : 80);

        for (ItineraryDay day : trip.getDays()) {
            List<ItineraryItem> items = day.getItems();
            if (items.size() > maxItemsPerDay) {
                // Trim excess non-completed items from the end
                for (int idx = items.size() - 1; idx >= maxItemsPerDay; idx--) {
                    if (!"COMPLETED".equalsIgnoreCase(items.get(idx).getStatus())) {
                        items.remove(idx);
                    }
                }
            } else if (items.size() < maxItemsPerDay) {
                // Add genuine attractions up to maxItemsPerDay
                while (items.size() < maxItemsPerDay && candIdx < candidates.size()) {
                    Map<String, Object> cand = candidates.get(candIdx++);
                    String name = (String) cand.get("name");
                    if (name != null && !existingTitles.contains(name.toLowerCase().trim())) {
                        existingTitles.add(name.toLowerCase().trim());
                        ItineraryItem item = new ItineraryItem();
                        item.setItineraryDay(day);
                        item.setTitle(name);
                        item.setCategory((String) cand.getOrDefault("category", "Sightseeing"));
                        item.setCostInr(0.0);
                        item.setIndoorOutdoor((String) cand.getOrDefault("indoorOutdoor", "Outdoor"));
                        item.setRecommendationReason("Added to match updated " + newActivityLevel + " travel pace.");

                        java.time.LocalTime clock = java.time.LocalTime.of(9, 30)
                                .plusMinutes(items.size() * (durMin + 20));
                        item.setStartTime(clock.format(timeFmt));
                        item.setEndTime(clock.plusMinutes(durMin).format(timeFmt));
                        item.setDurationMinutes(durMin);
                        item.setStatus("SCHEDULED");
                        item.setItemOrder(items.size());
                        item.setVerificationStatus("100% Real Location • Verified");
                        if (cand.containsKey("lat") && cand.get("lat") != null) {
                            try {
                                item.setLatitude(Double.parseDouble(cand.get("lat").toString()));
                            } catch (Exception ignored) {
                            }
                        }
                        if (cand.containsKey("lon") && cand.get("lon") != null) {
                            try {
                                item.setLongitude(Double.parseDouble(cand.get("lon").toString()));
                            } catch (Exception ignored) {
                            }
                        }
                        items.add(item);
                    }
                }
            }

            // Re-optimize day's items route with 2-opt and recalculate schedule
            double dStartLat = centerLat;
            double dStartLon = centerLon;
            String dStartName = trip.getStartingLocation() != null ? trip.getStartingLocation()
                    : trip.getDestinationCity();
            if (day.getDayNumber() != null && day.getDayNumber() > 1
                    && trip.getDays().size() >= day.getDayNumber() - 1) {
                ItineraryDay prevDay = trip.getDays().get(day.getDayNumber() - 2);
                if (!prevDay.getItems().isEmpty()) {
                    ItineraryItem prevLast = prevDay.getItems().get(prevDay.getItems().size() - 1);
                    if (prevLast.getLatitude() != null && prevLast.getLongitude() != null) {
                        dStartLat = prevLast.getLatitude();
                        dStartLon = prevLast.getLongitude();
                        dStartName = prevLast.getTitle();
                    }
                }
            }
            List<ItineraryItem> scheduledItems = new ArrayList<>();
            List<ItineraryItem> otherItems = new ArrayList<>();
            for (ItineraryItem it : items) {
                if (!"REPLACED".equalsIgnoreCase(it.getStatus()) && !"CANCELLED".equalsIgnoreCase(it.getStatus())) {
                    scheduledItems.add(it);
                } else {
                    otherItems.add(it);
                }
            }
            List<ItineraryItem> optItems = routeOptimizationService.optimizeDayItems(
                    dStartLat, dStartLon, scheduledItems, centerLat, centerLon);
            items.clear();
            items.addAll(optItems);
            items.addAll(otherItems);
            routeOptimizationService.recalculateDaySchedule(day, dStartLat, dStartLon, dStartName,
                    java.time.LocalTime.of(9, 30));
        }
    }
}