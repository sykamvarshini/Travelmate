package com.travelmate.service;

import com.travelmate.entity.ItineraryDay;
import com.travelmate.entity.ItineraryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RouteOptimizationServiceTest {

    private RouteOptimizationService routeOptimizer;

    @BeforeEach
    void setUp() {
        routeOptimizer = new RouteOptimizationService();
    }

    /**
     * Requirement 1 & User Example:
     * A -> B (far) -> C (near A) -> D (near B) must resolve to A -> C -> B -> D.
     */
    @Test
    void testUserPromptExample_ACBDOrdering() {
        Map<String, Object> locA = Map.of("name", "Location A", "lat", 28.6500, "lon", 77.2300);
        Map<String, Object> locB = Map.of("name", "Location B", "lat", 28.5200, "lon", 77.1800);
        Map<String, Object> locC = Map.of("name", "Location C", "lat", 28.6550, "lon", 77.2350); // near A (< 1 km)
        Map<String, Object> locD = Map.of("name", "Location D", "lat", 28.5150, "lon", 77.1850); // near B (< 1 km)

        List<Map<String, Object>> inefficientList = List.of(locA, locB, locC, locD);

        double startLat = 28.6500;
        double startLon = 77.2300;

        List<Map<String, Object>> optimized = routeOptimizer.optimizeOpenRoute(
                startLat, startLon, inefficientList, RouteOptimizationService.MAP_EXTRACTOR, 28.6500, 77.2300
        );

        assertEquals(4, optimized.size());
        assertEquals("Location A", optimized.get(0).get("name"));
        assertEquals("Location C", optimized.get(1).get("name"), "Location C (near A) must follow Location A");
        assertEquals("Location B", optimized.get(2).get("name"), "Location B should be visited after the A-C cluster");
        assertEquals("Location D", optimized.get(3).get("name"), "Location D (near B) should be visited with B");

        double oldDist = calculateListDistance(inefficientList, startLat, startLon);
        double newDist = calculateListDistance(optimized, startLat, startLon);
        assertTrue(newDist < oldDist, "Optimized distance (" + newDist + " km) must be strictly less than unoptimized (" + oldDist + " km)");
        System.out.println("[PASS] 1. User Prompt Example A -> C -> B -> D: distance reduced from " + Math.round(oldDist*10)/10.0 + " km to " + Math.round(newDist*10)/10.0 + " km.");
    }

    /**
     * Requirement 1 & 4: Multi-Day Delhi Cluster (Old Delhi vs South Delhi).
     */
    @Test
    void testMultiDayPartitioning_NeighbourhoodGrouping() {
        Map<String, Object> redFort = Map.of("name", "Red Fort", "lat", 28.6562, "lon", 77.2410);
        Map<String, Object> jamaMasjid = Map.of("name", "Jama Masjid", "lat", 28.6507, "lon", 77.2334);
        Map<String, Object> chandniChowk = Map.of("name", "Chandni Chowk", "lat", 28.6505, "lon", 77.2303);
        Map<String, Object> rajGhat = Map.of("name", "Raj Ghat", "lat", 28.6406, "lon", 77.2495);

        Map<String, Object> qutubMinar = Map.of("name", "Qutub Minar", "lat", 28.5244, "lon", 77.1855);
        Map<String, Object> mehrauliPark = Map.of("name", "Mehrauli Archaeological Park", "lat", 28.5195, "lon", 77.1870);
        Map<String, Object> lotusTemple = Map.of("name", "Lotus Temple", "lat", 28.5535, "lon", 77.2588);
        Map<String, Object> humayunTomb = Map.of("name", "Humayun Tomb", "lat", 28.5933, "lon", 77.2507);

        List<Map<String, Object>> pool = List.of(
                qutubMinar, redFort, mehrauliPark, jamaMasjid,
                lotusTemple, chandniChowk, humayunTomb, rajGhat
        );

        double startLat = 28.6328; // Connaught Place (Central)
        double startLon = 77.2197;

        List<Integer> quotas = List.of(4, 4);

        List<List<Map<String, Object>>> partitioned = routeOptimizer.partitionAndOptimizeRecs(
                startLat, startLon, pool, quotas, startLat, startLon
        );

        assertEquals(2, partitioned.size(), "Must produce 2 days");
        assertEquals(4, partitioned.get(0).size(), "Day 1 must preserve quota of 4 items");
        assertEquals(4, partitioned.get(1).size(), "Day 2 must preserve quota of 4 items");

        Set<String> day1Names = new HashSet<>();
        for (Map<String, Object> item : partitioned.get(0)) {
            day1Names.add(item.get("name").toString());
        }

        assertTrue(day1Names.contains("Red Fort"));
        assertTrue(day1Names.contains("Jama Masjid"));
        assertTrue(day1Names.contains("Chandni Chowk"));

        Set<String> day2Names = new HashSet<>();
        for (Map<String, Object> item : partitioned.get(1)) {
            day2Names.add(item.get("name").toString());
        }

        assertTrue(day2Names.contains("Qutub Minar"));
        assertTrue(day2Names.contains("Mehrauli Archaeological Park"));
        System.out.println("[PASS] 2. Multi-day neighbourhood clustering: Day 1 Old Delhi (" + day1Names.size() + "), Day 2 South Delhi (" + day2Names.size() + ").");
    }

    /**
     * Requirement 2: Travel Pace Preservations (Relaxed: 1-2, Moderate: 3-4, Fast Paced: 5-6).
     */
    @Test
    void testTravelPacePreservation() {
        // Prepare 12 attractions in Jaipur
        List<Map<String, Object>> jaipur = new ArrayList<>();
        double[][] coords = {
                {26.9855, 75.8513}, // Amer Fort
                {26.9840, 75.8507}, // Jaigarh Fort
                {26.9372, 75.8156}, // Nahargarh Fort
                {26.9239, 75.8267}, // Hawa Mahal
                {26.9258, 75.8237}, // City Palace
                {26.9248, 75.8246}, // Jantar Mantar
                {26.9117, 75.8194}, // Albert Hall
                {26.9534, 75.8462}, // Jal Mahal
                {26.8920, 75.8050}, // Birla Mandir
                {26.9930, 75.8500}, // Anokhi Museum
                {26.9170, 75.8150}, // Bapu Bazaar
                {26.9200, 75.8250}  // Johari Bazaar
        };

        for (int i = 0; i < coords.length; i++) {
            jaipur.add(Map.of("name", "Jaipur Spot " + (i + 1), "lat", coords[i][0], "lon", coords[i][1]));
        }

        double startLat = 26.9200;
        double startLon = 75.8200;

        // Test 1: Relaxed (2 places per day for 2 days = 4 places total)
        List<List<Map<String, Object>>> relaxed = routeOptimizer.partitionAndOptimizeRecs(
                startLat, startLon, jaipur.subList(0, 4), List.of(2, 2), startLat, startLon
        );
        assertEquals(2, relaxed.size());
        assertEquals(2, relaxed.get(0).size(), "Relaxed Day 1 must have exactly 2 places");
        assertEquals(2, relaxed.get(1).size(), "Relaxed Day 2 must have exactly 2 places");

        // Test 2: Moderate (4 places per day for 2 days = 8 places total)
        List<List<Map<String, Object>>> moderate = routeOptimizer.partitionAndOptimizeRecs(
                startLat, startLon, jaipur.subList(0, 8), List.of(4, 4), startLat, startLon
        );
        assertEquals(2, moderate.size());
        assertEquals(4, moderate.get(0).size(), "Moderate Day 1 must have exactly 4 places");
        assertEquals(4, moderate.get(1).size(), "Moderate Day 2 must have exactly 4 places");

        // Test 3: Fast Paced (6 places per day for 2 days = 12 places total)
        List<List<Map<String, Object>>> fastPaced = routeOptimizer.partitionAndOptimizeRecs(
                startLat, startLon, jaipur, List.of(6, 6), startLat, startLon
        );
        assertEquals(2, fastPaced.size());
        assertEquals(6, fastPaced.get(0).size(), "Fast Paced Day 1 must have exactly 6 places");
        assertEquals(6, fastPaced.get(1).size(), "Fast Paced Day 2 must have exactly 6 places");

        // Test 4: 3-Day Moderate Pace (alternating 4, 3, 4 = 11 places total)
        List<List<Map<String, Object>>> moderate3Day = routeOptimizer.partitionAndOptimizeRecs(
                startLat, startLon, jaipur.subList(0, 11), List.of(4, 3, 4), startLat, startLon
        );
        assertEquals(3, moderate3Day.size());
        assertEquals(4, moderate3Day.get(0).size(), "Moderate 3-Day: Day 1 must have 4 places (never 1!)");
        assertEquals(3, moderate3Day.get(1).size(), "Moderate 3-Day: Day 2 must have 3 places (never 1!)");
        assertEquals(4, moderate3Day.get(2).size(), "Moderate 3-Day: Day 3 must have 4 places (never 1!)");

        System.out.println("[PASS] 3. Travel pace preservation: Relaxed (1-2/day), Moderate (3-4/day), Fast Paced (5-6/day) strictly verified across multi-day itineraries.");
    }

    /**
     * Requirement 1 & 7: Goa North vs South Coastal Progressive Route
     */
    @Test
    void testGoaCoastalProgressiveRoute() {
        List<Map<String, Object>> goa = List.of(
                Map.of("name", "Palolem Beach", "lat", 15.0100, "lon", 74.0232),          // Far South
                Map.of("name", "Fort Aguada", "lat", 15.4921, "lon", 73.7736),            // North
                Map.of("name", "Colva Beach", "lat", 15.2793, "lon", 73.9167),            // South
                Map.of("name", "Baga Beach", "lat", 15.5553, "lon", 73.7517),             // North
                Map.of("name", "Basilica of Bom Jesus", "lat", 15.5009, "lon", 73.9116),  // Central
                Map.of("name", "Calangute Beach", "lat", 15.5439, "lon", 73.7553),        // North
                Map.of("name", "Chapora Fort", "lat", 15.6056, "lon", 73.7364),           // Far North
                Map.of("name", "Se Cathedral", "lat", 15.5036, "lon", 73.9125)            // Central
        );

        double startLat = 15.4909; // Panaji
        double startLon = 73.8278;

        List<List<Map<String, Object>>> days = routeOptimizer.partitionAndOptimizeRecs(
                startLat, startLon, goa, List.of(4, 4), startLat, startLon
        );

        assertEquals(2, days.size());
        List<String> d1 = days.get(0).stream().map(m -> m.get("name").toString()).toList();
        List<String> d2 = days.get(1).stream().map(m -> m.get("name").toString()).toList();

        // North Goa places should be grouped on Day 1
        assertTrue(d1.contains("Fort Aguada"));
        assertTrue(d1.contains("Calangute Beach"));
        assertTrue(d1.contains("Baga Beach"));
        assertTrue(d1.contains("Chapora Fort"));

        // South / Central places on Day 2
        assertTrue(d2.contains("Se Cathedral"));
        assertTrue(d2.contains("Basilica of Bom Jesus"));
        assertTrue(d2.contains("Colva Beach"));
        assertTrue(d2.contains("Palolem Beach"));

        System.out.println("[PASS] 4. Goa Coastal progressive route: Day 1 North Goa, Day 2 Central & South Goa.");
    }

    /**
     * Requirement 5: Day Schedule, Distances and Transit Timings.
     */
    @Test
    void testRecalculateDaySchedule_DistanceAndTransitTiming() {
        ItineraryDay day = new ItineraryDay();
        day.setDayNumber(1);

        ItineraryItem item1 = new ItineraryItem();
        item1.setTitle("Place 1");
        item1.setLatitude(28.6500);
        item1.setLongitude(77.2300);
        item1.setDurationMinutes(60);

        ItineraryItem item2 = new ItineraryItem();
        item2.setTitle("Place 2");
        item2.setLatitude(28.6550); // ~0.7 km away
        item2.setLongitude(77.2350);
        item2.setDurationMinutes(90);

        day.getItems().add(item1);
        day.getItems().add(item2);

        double startLat = 28.6480;
        double startLon = 77.2280;

        routeOptimizer.recalculateDaySchedule(day, startLat, startLon, "Hotel Plaza", LocalTime.of(9, 0));

        assertEquals(0, item1.getItemOrder());
        assertNotNull(item1.getDistanceFromPrevious());
        assertTrue(item1.getDistanceFromPrevious() > 0.0);
        assertNotNull(item1.getDurationFromPrevious());
        assertTrue(item1.getDurationFromPrevious() >= 15);
        assertEquals("09:15 AM", item1.getStartTime());
        assertEquals("10:15 AM", item1.getEndTime());
        assertTrue(item1.getTransportFromPrevious().contains("Hotel Plaza"));

        assertEquals(1, item2.getItemOrder());
        assertTrue(item2.getDistanceFromPrevious() < 2.0, "Consecutive close places should have small distance");
        assertTrue(item2.getDurationFromPrevious() >= 10);
        assertEquals("10:25 AM", item2.getStartTime());
        assertEquals("11:55 AM", item2.getEndTime());
        assertTrue(item2.getTransportFromPrevious().contains("Place 1"));
        System.out.println("[PASS] 5. Day schedule, distances, and transit timings verified.");
    }

    private double calculateListDistance(List<Map<String, Object>> list, double startLat, double startLon) {
        double dist = 0.0;
        double curLat = startLat;
        double curLon = startLon;
        for (Map<String, Object> item : list) {
            double[] coords = RouteOptimizationService.MAP_EXTRACTOR.getCoordinates(item, startLat, startLon);
            dist += routeOptimizer.haversineDistance(curLat, curLon, coords[0], coords[1]);
            curLat = coords[0];
            curLon = coords[1];
        }
        return dist;
    }

    /**
     * Requirement 1 & User Prompt Screenshot Test:
     * 11 Goa places where Place 11 (Reis Magos Fort, near Place 1 Aguada) was stranded
     * after Place 10 (Colva Beach in South Goa), creating an inefficient 28 km backtracking jump.
     * Verifies that Cluster-Based TSP + Or-Opt eliminates the 28 km jump and produces
     * a progressive North -> Central -> South route with zero backtracking.
     */
    @Test
    void testGoa11Places_Eliminates28KmBacktrackingJump() {
        List<Map<String, Object>> inefficientGoa = List.of(
                Map.of("name", "Place 1 (Fort Aguada)", "lat", 15.4921, "lon", 73.7736),
                Map.of("name", "Place 2 (Candolim Beach)", "lat", 15.5170, "lon", 73.7650),
                Map.of("name", "Place 3 (Calangute Beach)", "lat", 15.5439, "lon", 73.7553),
                Map.of("name", "Place 4 (Baga Beach)", "lat", 15.5553, "lon", 73.7517),
                Map.of("name", "Place 5 (Anjuna Flea Market)", "lat", 15.5830, "lon", 73.7440),
                Map.of("name", "Place 6 (Chapora Fort)", "lat", 15.6056, "lon", 73.7364),
                Map.of("name", "Place 7 (Panaji Church)", "lat", 15.4989, "lon", 73.8278),
                Map.of("name", "Place 8 (Basilica Bom Jesus)", "lat", 15.5009, "lon", 73.9116),
                Map.of("name", "Place 9 (Mangueshi Temple)", "lat", 15.4360, "lon", 73.9680),
                Map.of("name", "Place 10 (Colva Beach)", "lat", 15.2793, "lon", 73.9167),
                Map.of("name", "Place 11 (Reis Magos Fort)", "lat", 15.4980, "lon", 73.8080)
        );

        double startLat = 15.4921;
        double startLon = 73.7736;

        double unoptimizedTotalDist = calculateListDistance(inefficientGoa, startLat, startLon);
        // Distance between Place 10 and Place 11 in unoptimized order
        double badJumpDist = routeOptimizer.haversineDistance(15.2793, 73.9167, 15.4980, 73.8080);
        assertTrue(badJumpDist > 25.0, "Unoptimized sequence has a bad jump > 25 km");

        List<Map<String, Object>> optimized = routeOptimizer.optimizeOpenRoute(
                startLat, startLon, inefficientGoa, RouteOptimizationService.MAP_EXTRACTOR, startLat, startLon
        );

        assertEquals(11, optimized.size());
        double optimizedTotalDist = calculateListDistance(optimized, startLat, startLon);

        // Verify total distance savings (drops from 92+ km down to ~66 km)
        assertTrue(optimizedTotalDist < 70.0, "Optimized distance (" + optimizedTotalDist + " km) must be < 70 km");
        assertTrue(optimizedTotalDist < unoptimizedTotalDist - 20.0, "Must save > 20 km over unoptimized tour");

        // Verify Place 10 (Colva Beach) is visited at the end (South Goa terminus)
        assertEquals("Place 10 (Colva Beach)", optimized.get(10).get("name"), "Colva Beach should be the final South Goa stop");

        // Verify Place 11 (Reis Magos Fort) is visited with the North/Central cluster BEFORE South Goa
        int reisMagosIndex = -1;
        int colvaIndex = -1;
        for (int i = 0; i < optimized.size(); i++) {
            if ("Place 11 (Reis Magos Fort)".equals(optimized.get(i).get("name"))) reisMagosIndex = i;
            if ("Place 10 (Colva Beach)".equals(optimized.get(i).get("name"))) colvaIndex = i;
        }
        assertTrue(reisMagosIndex < colvaIndex, "Reis Magos (Place 11) must be visited BEFORE Colva Beach (Place 10)");
        assertTrue(reisMagosIndex <= 7, "Reis Magos must be clustered in the North/Central tour");

        // Verify no jump in the optimized tour is anywhere near 28 km
        for (int i = 1; i < optimized.size(); i++) {
            double[] prevC = RouteOptimizationService.MAP_EXTRACTOR.getCoordinates(optimized.get(i - 1), startLat, startLon);
            double[] currC = RouteOptimizationService.MAP_EXTRACTOR.getCoordinates(optimized.get(i), startLat, startLon);
            double stepDist = routeOptimizer.haversineDistance(prevC[0], prevC[1], currC[0], currC[1]);
            assertTrue(stepDist < 20.0, "Consecutive step distance (" + stepDist + " km) must be < 20 km; 28 km jump eliminated!");
        }

        System.out.println("[PASS] 6. Goa 11-place route: eliminated 28 km jump! Distance dropped from "
                + Math.round(unoptimizedTotalDist * 10) / 10.0 + " km to "
                + Math.round(optimizedTotalDist * 10) / 10.0 + " km (Reis Magos ordered at position #" + (reisMagosIndex + 1) + ").");
    }

    public static void main(String[] args) {
        System.out.println("=======================================================================");
        System.out.println("RUNNING FULL ROUTE OPTIMIZATION & TRAVEL PACE TEST SUITE");
        System.out.println("=======================================================================");
        RouteOptimizationServiceTest test = new RouteOptimizationServiceTest();
        test.setUp();
        test.testUserPromptExample_ACBDOrdering();
        test.testMultiDayPartitioning_NeighbourhoodGrouping();
        test.testTravelPacePreservation();
        test.testGoaCoastalProgressiveRoute();
        test.testRecalculateDaySchedule_DistanceAndTransitTiming();
        test.testGoa11Places_Eliminates28KmBacktrackingJump();
        System.out.println("\n>>> ALL 6 TEST SUITES PASSED WITH 100% SUCCESS! <<<");
        System.out.println("=======================================================================");
    }
}
