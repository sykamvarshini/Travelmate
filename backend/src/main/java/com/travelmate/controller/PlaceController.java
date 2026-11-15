package com.travelmate.controller;

import com.travelmate.service.PlaceDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceDiscoveryService placeDiscoveryService;

    public PlaceController(PlaceDiscoveryService placeDiscoveryService) {
        this.placeDiscoveryService = placeDiscoveryService;
    }

    /**
     * Autocomplete endpoint for real Indian locations as the user types.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<Map<String, Object>>> autocomplete(@RequestParam("query") String query) {
        List<Map<String, Object>> suggestions = placeDiscoveryService.autocomplete(query);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Full search and verification endpoint for any Indian place or landmark.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPlace(@RequestParam("query") String query) {
        Map<String, Object> details = placeDiscoveryService.searchPlace(query);
        if (Boolean.FALSE.equals(details.get("isIndia"))) {
            return ResponseEntity.badRequest().body(details);
        }
        return ResponseEntity.ok(details);
    }

    /**
     * Discovers real nearby verified attractions around coordinates or destination.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<Map<String, Object>>> discoverNearby(
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "limit", defaultValue = "8") int limit) {

        if (lat == null || lon == null) {
            if (destination != null && !destination.trim().isEmpty()) {
                Map<String, Object> found = placeDiscoveryService.searchPlace(destination);
                if (found.containsKey("lat") && found.containsKey("lon")) {
                    lat = Double.parseDouble(found.get("lat").toString());
                    lon = Double.parseDouble(found.get("lon").toString());
                }
            }
        }

        if (lat == null || lon == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Map<String, Object>> nearby = placeDiscoveryService.discoverNearby(lat, lon, destination, category, limit);
        return ResponseEntity.ok(nearby);
    }

    /**
     * Dynamic category-based discovery in and around any Indian destination.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> discoverByCategory(
            @RequestParam("destination") String destination,
            @RequestParam("category") String category) {
        List<Map<String, Object>> items = placeDiscoveryService.discoverByCategory(destination, category);
        return ResponseEntity.ok(items);
    }

    /**
     * Search legitimate attractions within a destination for manual place addition and rescheduling.
     */
    @GetMapping("/destination-attractions")
    public ResponseEntity<List<Map<String, Object>>> getDestinationAttractions(
            @RequestParam("destination") String destination,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "prevLat", required = false) Double prevLat,
            @RequestParam(value = "prevLon", required = false) Double prevLon) {
        List<Map<String, Object>> list = placeDiscoveryService.searchDestinationAttractions(destination, query, prevLat, prevLon);
        return ResponseEntity.ok(list);
    }
}
