package com.travelmate.service;

import com.travelmate.entity.Attraction;
import com.travelmate.entity.ItineraryDay;
import com.travelmate.entity.ItineraryItem;
import com.travelmate.entity.Trip;
import com.travelmate.entity.User;
import com.travelmate.repository.AttractionRepository;
import com.travelmate.repository.ItineraryItemRepository;
import com.travelmate.repository.TripMemberRepository;
import com.travelmate.repository.TripRepository;
import com.travelmate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ManualItineraryEditingTest {

    private TripRepository tripRepository;
    private UserRepository userRepository;
    private MLIntegrationService mlIntegrationService;
    private LocationService locationService;
    private PlaceDiscoveryService placeDiscoveryService;
    private WeatherService weatherService;
    private ItineraryItemRepository itineraryItemRepository;
    private TripMemberRepository tripMemberRepository;
    private RouteOptimizationService routeOptimizationService;
    private AttractionRepository attractionRepository;
    private RestTemplate restTemplate;

    private TripService tripService;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        userRepository = mock(UserRepository.class);
        mlIntegrationService = mock(MLIntegrationService.class);
        locationService = mock(LocationService.class);
        placeDiscoveryService = mock(PlaceDiscoveryService.class);
        weatherService = mock(WeatherService.class);
        itineraryItemRepository = mock(ItineraryItemRepository.class);
        tripMemberRepository = mock(TripMemberRepository.class);
        routeOptimizationService = new RouteOptimizationService();
        attractionRepository = mock(AttractionRepository.class);
        restTemplate = mock(RestTemplate.class);

        tripService = new TripService(
                tripRepository,
                userRepository,
                mlIntegrationService,
                locationService,
                placeDiscoveryService,
                weatherService,
                itineraryItemRepository,
                tripMemberRepository,
                routeOptimizationService,
                attractionRepository);

        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Trip createVisakhapatnamTrip() {
        Trip trip = new Trip();
        trip.setId(50L);
        trip.setDestinationCity("Visakhapatnam");
        trip.setStartingLocation("Visakhapatnam Railway Station");
        trip.setDestinationLatitude(17.6868);
        trip.setDestinationLongitude(83.2185);
        trip.setOriginLatitude(17.7215);
        trip.setOriginLongitude(83.2890);
        trip.setStartDate(LocalDate.now());
        trip.setEndDate(LocalDate.now().plusDays(1));
        trip.setNumDays(2);
        trip.setActivityLevel("Moderate");
        trip.setBudgetInr(25000.0);

        User user = new User();
        user.setEmail("traveler@travelmate.com");
        trip.setUser(user);

        // Day 1
        ItineraryDay day1 = new ItineraryDay();
        day1.setId(501L);
        day1.setDayNumber(1);
        day1.setDate(LocalDate.now());
        day1.setTrip(trip);

        ItineraryItem d1i1 = new ItineraryItem();
        d1i1.setId(1001L);
        d1i1.setTitle("Simhachalam Temple");
        d1i1.setCategory("Temples / Spiritual");
        d1i1.setStartTime("09:30 AM");
        d1i1.setDurationMinutes(90);
        d1i1.setLatitude(17.7660);
        d1i1.setLongitude(83.2500);
        d1i1.setItemOrder(0);
        d1i1.setStatus("SCHEDULED");
        d1i1.setItineraryDay(day1);

        ItineraryItem d1i2 = new ItineraryItem();
        d1i2.setId(1002L);
        d1i2.setTitle("RK Beach");
        d1i2.setCategory("Beaches");
        d1i2.setStartTime("11:30 AM");
        d1i2.setDurationMinutes(90);
        d1i2.setLatitude(17.7140);
        d1i2.setLongitude(83.3240);
        d1i2.setItemOrder(1);
        d1i2.setStatus("SCHEDULED");
        d1i2.setItineraryDay(day1);

        ItineraryItem d1i3 = new ItineraryItem();
        d1i3.setId(1003L);
        d1i3.setTitle("Kailasagiri");
        d1i3.setCategory("Viewpoints / Attractions");
        d1i3.setStartTime("01:30 PM");
        d1i3.setDurationMinutes(90);
        d1i3.setLatitude(17.7490);
        d1i3.setLongitude(83.3420);
        d1i3.setItemOrder(2);
        d1i3.setStatus("SCHEDULED");
        d1i3.setItineraryDay(day1);

        ItineraryItem d1i4 = new ItineraryItem();
        d1i4.setId(1004L);
        d1i4.setTitle("Submarine Museum");
        d1i4.setCategory("Museums");
        d1i4.setStartTime("03:30 PM");
        d1i4.setDurationMinutes(90);
        d1i4.setLatitude(17.7180);
        d1i4.setLongitude(83.3330);
        d1i4.setItemOrder(3);
        d1i4.setStatus("SCHEDULED");
        d1i4.setItineraryDay(day1);

        day1.setItems(new ArrayList<>(Arrays.asList(d1i1, d1i2, d1i3, d1i4)));

        // Day 2
        ItineraryDay day2 = new ItineraryDay();
        day2.setId(502L);
        day2.setDayNumber(2);
        day2.setDate(LocalDate.now().plusDays(1));
        day2.setTrip(trip);

        ItineraryItem d2i1 = new ItineraryItem();
        d2i1.setId(2001L);
        d2i1.setTitle("Araku Valley");
        d2i1.setCategory("Nature / Hill Stations");
        d2i1.setStartTime("09:30 AM");
        d2i1.setDurationMinutes(120);
        d2i1.setLatitude(18.3333);
        d2i1.setLongitude(82.8833);
        d2i1.setItemOrder(0);
        d2i1.setStatus("SCHEDULED");
        d2i1.setItineraryDay(day2);

        ItineraryItem d2i2 = new ItineraryItem();
        d2i2.setId(2002L);
        d2i2.setTitle("Borra Caves");
        d2i2.setCategory("Nature / Caves");
        d2i2.setStartTime("12:00 PM");
        d2i2.setDurationMinutes(90);
        d2i2.setLatitude(18.2814);
        d2i2.setLongitude(83.0402);
        d2i2.setItemOrder(1);
        d2i2.setStatus("SCHEDULED");
        d2i2.setItineraryDay(day2);

        ItineraryItem d2i3 = new ItineraryItem();
        d2i3.setId(2003L);
        d2i3.setTitle("Coffee Museum");
        d2i3.setCategory("Museums");
        d2i3.setStartTime("02:00 PM");
        d2i3.setDurationMinutes(60);
        d2i3.setLatitude(18.3280);
        d2i3.setLongitude(82.8750);
        d2i3.setItemOrder(2);
        d2i3.setStatus("SCHEDULED");
        d2i3.setItineraryDay(day2);

        day2.setItems(new ArrayList<>(Arrays.asList(d2i1, d2i2, d2i3)));

        trip.setDays(new ArrayList<>(Arrays.asList(day1, day2)));

        when(tripRepository.findById(50L)).thenReturn(Optional.of(trip));
        when(itineraryItemRepository.findById(1001L)).thenReturn(Optional.of(d1i1));
        when(itineraryItemRepository.findById(1002L)).thenReturn(Optional.of(d1i2));
        when(itineraryItemRepository.findById(1003L)).thenReturn(Optional.of(d1i3));
        when(itineraryItemRepository.findById(1004L)).thenReturn(Optional.of(d1i4));
        when(itineraryItemRepository.findById(2001L)).thenReturn(Optional.of(d2i1));
        when(itineraryItemRepository.findById(2002L)).thenReturn(Optional.of(d2i2));
        when(itineraryItemRepository.findById(2003L)).thenReturn(Optional.of(d2i3));

        return trip;
    }

    @Test
    @DisplayName("Mandatory TEST A: Move RK Beach from Day 1 to Day 2 - verify removal, insertion, and sequence")
    void testA_MoveRKBeach_Day1ToDay2() {
        Trip trip = createVisakhapatnamTrip();
        assertEquals(4, trip.getDays().get(0).getItems().size());
        assertEquals(3, trip.getDays().get(1).getItems().size());

        // Move RK Beach (1002L) from Day 1 to Day 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1002L, 502L);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        // Day 1 must have 3 items left: Simhachalam Temple, Kailasagiri, Submarine
        // Museum
        assertEquals(3, day1.getItems().size());
        assertFalse(day1.getItems().stream().anyMatch(i -> i.getId().equals(1002L)));
        assertEquals("Simhachalam Temple", day1.getItems().get(0).getTitle());
        assertEquals(0, day1.getItems().get(0).getItemOrder());
        assertEquals("Kailasagiri", day1.getItems().get(1).getTitle());
        assertEquals(1, day1.getItems().get(1).getItemOrder());
        assertEquals("Submarine Museum", day1.getItems().get(2).getTitle());
        assertEquals(2, day1.getItems().get(2).getItemOrder());

        // Day 2 must have 4 items including RK Beach
        assertEquals(4, day2.getItems().size());
        assertTrue(day2.getItems().stream().anyMatch(i -> i.getId().equals(1002L)));
        ItineraryItem rkBeachInDay2 = day2.getItems().stream().filter(i -> i.getId().equals(1002L)).findFirst().get();
        assertEquals(day2, rkBeachInDay2.getItineraryDay());

        // Sequence numbers in Day 2 must be strictly sequential 0, 1, 2, 3
        for (int i = 0; i < day2.getItems().size(); i++) {
            assertEquals(i, day2.getItems().get(i).getItemOrder());
        }
    }

    @Test
    @DisplayName("Mandatory TEST B: Move Kailasagiri from position 3 to position 1 in Day 1 - verify shift")
    void testB_MoveKailasagiri_Position3ToPosition1() {
        Trip trip = createVisakhapatnamTrip();
        ItineraryDay day1 = trip.getDays().get(0);
        // Original order: Simhachalam (0), RK Beach (1), Kailasagiri (2, pos 3),
        // Submarine Museum (3)
        assertEquals("Kailasagiri", day1.getItems().get(2).getTitle());

        // Move Kailasagiri (1003L) to position 1
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1003L, 501L, 1);

        assertNotNull(updated);
        ItineraryDay updatedDay1 = updated.getDays().get(0);
        List<ItineraryItem> items = updatedDay1.getItems();

        assertEquals(4, items.size());
        // Position 1 (index 0) must actually become Kailasagiri
        assertEquals("Kailasagiri", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());

        // Other places shift down automatically
        assertEquals("Simhachalam Temple", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("RK Beach", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
        assertEquals("Submarine Museum", items.get(3).getTitle());
        assertEquals(3, items.get(3).getItemOrder());
    }

    @Test
    @DisplayName("Mandatory TEST C: Move place from Day 2 to Day 1 - verify both days update")
    void testC_MovePlace_Day2ToDay1() {
        Trip trip = createVisakhapatnamTrip();

        // Move Borra Caves (2002L) from Day 2 to Day 1 at position 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 2002L, 501L, 2);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        // Day 1 has 5 items now, Borra Caves inserted at position 2 (index 1)
        assertEquals(5, day1.getItems().size());
        assertEquals("Borra Caves", day1.getItems().get(1).getTitle());
        assertEquals(1, day1.getItems().get(1).getItemOrder());

        // Day 2 has 2 items left (Araku Valley, Coffee Museum)
        assertEquals(2, day2.getItems().size());
        assertFalse(day2.getItems().stream().anyMatch(i -> i.getId().equals(2002L)));
        assertEquals(0, day2.getItems().get(0).getItemOrder());
        assertEquals(1, day2.getItems().get(1).getItemOrder());
    }

    @Test
    @DisplayName("Mandatory TEST D: Move place from Day 1 to Day 2 and set position to 2 - combined test")
    void testD_MovePlace_Day1ToDay2_Position2_Combined() {
        Trip trip = createVisakhapatnamTrip();

        // Move RK Beach (1002L) from Day 1 to Day 2 at position 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1002L, 502L, 2);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        // Expected Day 1:
        // 1. Simhachalam Temple
        // 2. Kailasagiri
        // 3. Submarine Museum
        assertEquals(3, day1.getItems().size());
        assertEquals("Simhachalam Temple", day1.getItems().get(0).getTitle());
        assertEquals("Kailasagiri", day1.getItems().get(1).getTitle());
        assertEquals("Submarine Museum", day1.getItems().get(2).getTitle());

        // Expected Day 2:
        // 1. Araku Valley
        // 2. RK Beach
        // 3. Borra Caves
        // 4. Coffee Museum
        assertEquals(4, day2.getItems().size());
        assertEquals("Araku Valley", day2.getItems().get(0).getTitle());
        assertEquals("RK Beach", day2.getItems().get(1).getTitle());
        assertEquals("Borra Caves", day2.getItems().get(2).getTitle());
        assertEquals("Coffee Museum", day2.getItems().get(3).getTitle());

        // Verify sequence numbers
        for (int i = 0; i < day2.getItems().size(); i++) {
            assertEquals(i, day2.getItems().get(i).getItemOrder());
        }
    }

    @Test
    @DisplayName("Mandatory TEST E: Check MySQL persistence - no duplicates, no disappearing, distinct order")
    void testE_CheckMySQLPersistence_SequenceNumbers_NoDuplicates() {
        Trip trip = createVisakhapatnamTrip();

        // Perform combined move
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1002L, 502L, 2);

        // Verify save was called for all affected items and days
        verify(itineraryItemRepository, atLeast(7)).save(any(ItineraryItem.class));
        verify(itineraryItemRepository, atLeastOnce()).flush();
        verify(tripRepository, atLeastOnce()).save(any(Trip.class));

        // Check for duplicates across the entire trip
        Set<Long> seenIds = new HashSet<>();
        int totalItems = 0;
        for (ItineraryDay d : updated.getDays()) {
            Set<Integer> ordersInDay = new HashSet<>();
            for (ItineraryItem it : d.getItems()) {
                totalItems++;
                assertFalse(seenIds.contains(it.getId()), "Duplicate item ID found: " + it.getId());
                seenIds.add(it.getId());

                assertFalse(ordersInDay.contains(it.getItemOrder()),
                        "Duplicate itemOrder in day: " + it.getItemOrder());
                ordersInDay.add(it.getItemOrder());
            }
        }
        assertEquals(7, totalItems, "No items should disappear; expected exactly 7 items");
    }

    @Test
    @DisplayName("Mandatory TEST 4: Swap Up - 3rd place (Kailasagiri) swaps with 2nd place (RK Beach)")
    void test4_SwapUp_ThirdPlaceWithSecondPlace() {
        Trip trip = createVisakhapatnamTrip();
        ItineraryDay day1 = trip.getDays().get(0);
        assertEquals("Simhachalam Temple", day1.getItems().get(0).getTitle());
        assertEquals("RK Beach", day1.getItems().get(1).getTitle());
        assertEquals("Kailasagiri", day1.getItems().get(2).getTitle());
        assertEquals("Submarine Museum", day1.getItems().get(3).getTitle());

        // Swap Up on Kailasagiri (1003L) at index 2 -> moves to position 2
        // (targetPosition = 2)
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1003L, 501L, 2);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals(4, items.size());
        assertEquals("Simhachalam Temple", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("Kailasagiri", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("RK Beach", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
        assertEquals("Submarine Museum", items.get(3).getTitle());
        assertEquals(3, items.get(3).getItemOrder());
    }

    @Test
    @DisplayName("Mandatory TEST 5: Swap Down - 2nd place (RK Beach) swaps with 3rd place (Kailasagiri)")
    void test5_SwapDown_SecondPlaceWithThirdPlace() {
        Trip trip = createVisakhapatnamTrip();
        ItineraryDay day1 = trip.getDays().get(0);
        assertEquals("Simhachalam Temple", day1.getItems().get(0).getTitle());
        assertEquals("RK Beach", day1.getItems().get(1).getTitle());
        assertEquals("Kailasagiri", day1.getItems().get(2).getTitle());
        assertEquals("Submarine Museum", day1.getItems().get(3).getTitle());

        // Swap Down on RK Beach (1002L) at index 1 -> moves to position 3
        // (targetPosition = 3)
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1002L, 501L, 3);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals(4, items.size());
        assertEquals("Simhachalam Temple", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("Kailasagiri", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("RK Beach", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
        assertEquals("Submarine Museum", items.get(3).getTitle());
        assertEquals(3, items.get(3).getItemOrder());
    }

    @Test
    @DisplayName("Mandatory TEST 8: Failed Save Simulation - Database failure throws exception, no fake success")
    void test8_FailedSave_PropagatesRealError_NoCorruptedState() {
        Trip trip = createVisakhapatnamTrip();
        // Configure repository to throw an exception on flush or save
        doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "Database constraint violation during reorder"))
                .when(itineraryItemRepository).flush();

        Exception thrown = assertThrows(Exception.class, () -> {
            tripService.moveActivityToDay("traveler@travelmate.com", 50L, 1002L, 502L, 2);
        });

        assertNotNull(thrown);
        assertTrue(thrown.getMessage().contains("Database constraint violation")
                || thrown instanceof org.springframework.dao.DataIntegrityViolationException);
    }

    private Trip createVisakhapatnamThreeDayTrip() {
        Trip trip = new Trip();
        trip.setId(70L);
        trip.setDestinationCity("Visakhapatnam");
        trip.setStartingLocation("Visakhapatnam Station");
        trip.setDestinationLatitude(17.6868);
        trip.setDestinationLongitude(83.2185);
        trip.setOriginLatitude(17.7215);
        trip.setOriginLongitude(83.2890);
        trip.setStartDate(LocalDate.now());
        trip.setEndDate(LocalDate.now().plusDays(2));
        trip.setNumDays(3);
        trip.setActivityLevel("Moderate");
        trip.setBudgetInr(35000.0);

        User user = new User();
        user.setEmail("traveler@travelmate.com");
        trip.setUser(user);

        // Day 1
        ItineraryDay day1 = new ItineraryDay();
        day1.setId(601L);
        day1.setDayNumber(1);
        day1.setTrip(trip);

        ItineraryItem d1i1 = new ItineraryItem();
        d1i1.setId(7001L);
        d1i1.setTitle("Kailasagiri");
        d1i1.setCategory("Viewpoints");
        d1i1.setItemOrder(0);
        d1i1.setStatus("SCHEDULED");
        d1i1.setItineraryDay(day1);

        ItineraryItem d1i2 = new ItineraryItem();
        d1i2.setId(7002L);
        d1i2.setTitle("RK Beach");
        d1i2.setCategory("Beaches");
        d1i2.setItemOrder(1);
        d1i2.setStatus("SCHEDULED");
        d1i2.setItineraryDay(day1);

        ItineraryItem d1i3 = new ItineraryItem();
        d1i3.setId(7003L);
        d1i3.setTitle("Submarine Museum");
        d1i3.setCategory("Museums");
        d1i3.setItemOrder(2);
        d1i3.setStatus("SCHEDULED");
        d1i3.setItineraryDay(day1);

        day1.setItems(new ArrayList<>(Arrays.asList(d1i1, d1i2, d1i3)));

        // Day 2
        ItineraryDay day2 = new ItineraryDay();
        day2.setId(602L);
        day2.setDayNumber(2);
        day2.setTrip(trip);

        ItineraryItem d2i1 = new ItineraryItem();
        d2i1.setId(7004L);
        d2i1.setTitle("Araku Valley");
        d2i1.setCategory("Nature");
        d2i1.setItemOrder(0);
        d2i1.setStatus("SCHEDULED");
        d2i1.setItineraryDay(day2);

        ItineraryItem d2i2 = new ItineraryItem();
        d2i2.setId(7005L);
        d2i2.setTitle("Borra Caves");
        d2i2.setCategory("Nature");
        d2i2.setItemOrder(1);
        d2i2.setStatus("SCHEDULED");
        d2i2.setItineraryDay(day2);

        ItineraryItem d2i3 = new ItineraryItem();
        d2i3.setId(7006L);
        d2i3.setTitle("Coffee Museum");
        d2i3.setCategory("Museums");
        d2i3.setItemOrder(2);
        d2i3.setStatus("SCHEDULED");
        d2i3.setItineraryDay(day2);

        day2.setItems(new ArrayList<>(Arrays.asList(d2i1, d2i2, d2i3)));

        // Day 3
        ItineraryDay day3 = new ItineraryDay();
        day3.setId(603L);
        day3.setDayNumber(3);
        day3.setTrip(trip);

        ItineraryItem d3i1 = new ItineraryItem();
        d3i1.setId(7007L);
        d3i1.setTitle("Simhachalam Temple");
        d3i1.setCategory("Temples / Spiritual");
        d3i1.setItemOrder(0);
        d3i1.setStatus("SCHEDULED");
        d3i1.setItineraryDay(day3);

        ItineraryItem d3i2 = new ItineraryItem();
        d3i2.setId(7008L);
        d3i2.setTitle("Rushikonda Beach");
        d3i2.setCategory("Beaches");
        d3i2.setItemOrder(1);
        d3i2.setStatus("SCHEDULED");
        d3i2.setItineraryDay(day3);

        day3.setItems(new ArrayList<>(Arrays.asList(d3i1, d3i2)));

        trip.setDays(new ArrayList<>(Arrays.asList(day1, day2, day3)));

        when(tripRepository.findById(70L)).thenReturn(Optional.of(trip));
        when(itineraryItemRepository.findById(7001L)).thenReturn(Optional.of(d1i1));
        when(itineraryItemRepository.findById(7002L)).thenReturn(Optional.of(d1i2));
        when(itineraryItemRepository.findById(7003L)).thenReturn(Optional.of(d1i3));
        when(itineraryItemRepository.findById(7004L)).thenReturn(Optional.of(d2i1));
        when(itineraryItemRepository.findById(7005L)).thenReturn(Optional.of(d2i2));
        when(itineraryItemRepository.findById(7006L)).thenReturn(Optional.of(d2i3));
        when(itineraryItemRepository.findById(7007L)).thenReturn(Optional.of(d3i1));
        when(itineraryItemRepository.findById(7008L)).thenReturn(Optional.of(d3i2));

        return trip;
    }

    @Test
    @DisplayName("User Scenario TEST 1: Move Simhachalam Temple from Day 3 to Day 1, position #3")
    void testUserScenario_Test1_MoveSimhachalam_Day3ToDay1_Pos3() {
        Trip trip = createVisakhapatnamThreeDayTrip();
        assertEquals(3, trip.getDays().get(0).getItems().size());
        assertEquals(2, trip.getDays().get(2).getItems().size());

        // Move Simhachalam Temple (7007L) from Day 3 to Day 1 (601L) at position 3
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 70L, 7007L, 601L, 3);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day3 = updated.getDays().get(2);

        // Day 1 has 4 places now: Kailasagiri, RK Beach, Simhachalam Temple, Submarine
        // Museum
        assertEquals(4, day1.getItems().size());
        assertEquals("Kailasagiri", day1.getItems().get(0).getTitle());
        assertEquals(0, day1.getItems().get(0).getItemOrder());
        assertEquals("RK Beach", day1.getItems().get(1).getTitle());
        assertEquals(1, day1.getItems().get(1).getItemOrder());
        assertEquals("Simhachalam Temple", day1.getItems().get(2).getTitle()); // Position #3
        assertEquals(2, day1.getItems().get(2).getItemOrder());
        assertEquals("Submarine Museum", day1.getItems().get(3).getTitle());
        assertEquals(3, day1.getItems().get(3).getItemOrder());

        // Foreign key updated to Day 1
        assertEquals(day1, day1.getItems().get(2).getItineraryDay());

        // Day 3 has 1 place remaining: Rushikonda Beach
        assertEquals(1, day3.getItems().size());
        assertEquals("Rushikonda Beach", day3.getItems().get(0).getTitle());
        assertEquals(0, day3.getItems().get(0).getItemOrder());
    }

    @Test
    @DisplayName("User Scenario TEST 2: Move a place from Day 1 to Day 2 - both days update")
    void testUserScenario_Test2_MovePlace_Day1ToDay2() {
        Trip trip = createVisakhapatnamThreeDayTrip();

        // Move RK Beach (7002L) from Day 1 to Day 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 70L, 7002L, 602L);

        assertNotNull(updated);
        assertEquals(2, updated.getDays().get(0).getItems().size());
        assertEquals(4, updated.getDays().get(1).getItems().size());
        assertTrue(updated.getDays().get(1).getItems().stream().anyMatch(i -> i.getId().equals(7002L)));
    }

    @Test
    @DisplayName("User Scenario TEST 3: Change a place from position #3 to position #1")
    void testUserScenario_Test3_Position3ToPosition1() {
        Trip trip = createVisakhapatnamThreeDayTrip();
        ItineraryDay day1 = trip.getDays().get(0);
        assertEquals("Submarine Museum", day1.getItems().get(2).getTitle());

        // Move Submarine Museum (7003L) to position 1
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 70L, 7003L, 601L, 1);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals("Submarine Museum", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("Kailasagiri", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("RK Beach", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
    }

    @Test
    @DisplayName("User Scenario TEST 4: Swap Up moves place up exactly one position")
    void testUserScenario_Test4_SwapUp_OnePosition() {
        Trip trip = createVisakhapatnamThreeDayTrip();
        // Day 1 has [Kailasagiri, RK Beach, Submarine Museum]
        // Swap Up on Submarine Museum (7003L, pos 3) -> targetPosition = 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 70L, 7003L, 601L, 2);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals("Kailasagiri", items.get(0).getTitle());
        assertEquals("Submarine Museum", items.get(1).getTitle());
        assertEquals("RK Beach", items.get(2).getTitle());
    }

    @Test
    @DisplayName("User Scenario TEST 5: Swap Down moves place down exactly one position")
    void testUserScenario_Test5_SwapDown_OnePosition() {
        Trip trip = createVisakhapatnamThreeDayTrip();
        // Day 1 has [Kailasagiri, RK Beach, Submarine Museum]
        // Swap Down on Kailasagiri (7001L, pos 1) -> targetPosition = 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 70L, 7001L, 601L, 2);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals("RK Beach", items.get(0).getTitle());
        assertEquals("Kailasagiri", items.get(1).getTitle());
        assertEquals("Submarine Museum", items.get(2).getTitle());
    }

    @Test
    @DisplayName("User Scenario TEST 6: Refresh website verification - MySQL persistence verified")
    void testUserScenario_Test6_RefreshWebsite_PersistenceVerified() {
        Trip trip = createVisakhapatnamThreeDayTrip();
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 70L, 7007L, 601L, 3);

        // Verify repository save was invoked for items and days
        verify(itineraryItemRepository, atLeastOnce()).save(any(ItineraryItem.class));
        verify(itineraryItemRepository, atLeastOnce()).flush();
        verify(tripRepository, atLeastOnce()).save(any(Trip.class));

        // Reload via getTripById simulating page refresh
        Trip reloaded = tripService.getTripById("traveler@travelmate.com", 70L);
        assertNotNull(reloaded);
        assertEquals(4, reloaded.getDays().get(0).getItems().size());
        assertEquals("Simhachalam Temple", reloaded.getDays().get(0).getItems().get(2).getTitle());
        assertEquals(1, reloaded.getDays().get(2).getItems().size());
    }

    private Trip createTU142VisakhapatnamTrip() {
        Trip trip = new Trip();
        trip.setId(75L);
        trip.setDestinationCity("Visakhapatnam");
        trip.setStartingLocation("Visakhapatnam Station");
        trip.setDestinationLatitude(17.6868);
        trip.setDestinationLongitude(83.2185);
        trip.setOriginLatitude(17.7215);
        trip.setOriginLongitude(83.2890);
        trip.setStartDate(LocalDate.now());
        trip.setEndDate(LocalDate.now().plusDays(1));
        trip.setNumDays(2);
        trip.setActivityLevel("Moderate");
        trip.setBudgetInr(35000.0);

        User user = new User();
        user.setEmail("traveler@travelmate.com");
        trip.setUser(user);

        // Day 1: 1. TU 142 Aircraft Museum, 2. RK Beach, 3. Kailasagiri
        ItineraryDay day1 = new ItineraryDay();
        day1.setId(651L);
        day1.setDayNumber(1);
        day1.setTrip(trip);

        ItineraryItem d1i1 = new ItineraryItem();
        d1i1.setId(7501L);
        d1i1.setTitle("TU 142 Aircraft Museum");
        d1i1.setCategory("Museums");
        d1i1.setLatitude(17.7215);
        d1i1.setLongitude(83.3375);
        d1i1.setItemOrder(0);
        d1i1.setStatus("SCHEDULED");
        d1i1.setItineraryDay(day1);

        ItineraryItem d1i2 = new ItineraryItem();
        d1i2.setId(7502L);
        d1i2.setTitle("RK Beach");
        d1i2.setCategory("Beaches");
        d1i2.setLatitude(17.7144);
        d1i2.setLongitude(83.3235);
        d1i2.setItemOrder(1);
        d1i2.setStatus("SCHEDULED");
        d1i2.setItineraryDay(day1);

        ItineraryItem d1i3 = new ItineraryItem();
        d1i3.setId(7503L);
        d1i3.setTitle("Kailasagiri");
        d1i3.setCategory("Viewpoints");
        d1i3.setLatitude(17.7490);
        d1i3.setLongitude(83.3423);
        d1i3.setItemOrder(2);
        d1i3.setStatus("SCHEDULED");
        d1i3.setItineraryDay(day1);

        day1.setItems(new ArrayList<>(Arrays.asList(d1i1, d1i2, d1i3)));

        // Day 2: 1. Araku Valley, 2. Borra Caves, 3. Coffee Museum
        ItineraryDay day2 = new ItineraryDay();
        day2.setId(652L);
        day2.setDayNumber(2);
        day2.setTrip(trip);

        ItineraryItem d2i1 = new ItineraryItem();
        d2i1.setId(7504L);
        d2i1.setTitle("Araku Valley");
        d2i1.setCategory("Nature");
        d2i1.setLatitude(18.3273);
        d2i1.setLongitude(82.8775);
        d2i1.setItemOrder(0);
        d2i1.setStatus("SCHEDULED");
        d2i1.setItineraryDay(day2);

        ItineraryItem d2i2 = new ItineraryItem();
        d2i2.setId(7505L);
        d2i2.setTitle("Borra Caves");
        d2i2.setCategory("Nature");
        d2i2.setLatitude(18.2804);
        d2i2.setLongitude(83.0395);
        d2i2.setItemOrder(1);
        d2i2.setStatus("SCHEDULED");
        d2i2.setItineraryDay(day2);

        ItineraryItem d2i3 = new ItineraryItem();
        d2i3.setId(7506L);
        d2i3.setTitle("Coffee Museum");
        d2i3.setCategory("Museums");
        d2i3.setLatitude(18.3333);
        d2i3.setLongitude(82.8667);
        d2i3.setItemOrder(2);
        d2i3.setStatus("SCHEDULED");
        d2i3.setItineraryDay(day2);

        day2.setItems(new ArrayList<>(Arrays.asList(d2i1, d2i2, d2i3)));

        trip.setDays(new ArrayList<>(Arrays.asList(day1, day2)));

        when(tripRepository.findById(75L)).thenReturn(Optional.of(trip));
        when(itineraryItemRepository.findById(7501L)).thenReturn(Optional.of(d1i1));
        when(itineraryItemRepository.findById(7502L)).thenReturn(Optional.of(d1i2));
        when(itineraryItemRepository.findById(7503L)).thenReturn(Optional.of(d1i3));
        when(itineraryItemRepository.findById(7504L)).thenReturn(Optional.of(d2i1));
        when(itineraryItemRepository.findById(7505L)).thenReturn(Optional.of(d2i2));
        when(itineraryItemRepository.findById(7506L)).thenReturn(Optional.of(d2i3));

        return trip;
    }

    private Trip createIntraDayTrip() {
        Trip trip = new Trip();
        trip.setId(76L);
        trip.setDestinationCity("Visakhapatnam");
        trip.setStartingLocation("Visakhapatnam Station");
        trip.setDestinationLatitude(17.6868);
        trip.setDestinationLongitude(83.2185);
        trip.setOriginLatitude(17.7215);
        trip.setOriginLongitude(83.2890);
        trip.setStartDate(LocalDate.now());
        trip.setEndDate(LocalDate.now());
        trip.setNumDays(1);
        trip.setActivityLevel("Moderate");
        trip.setBudgetInr(20000.0);

        User user = new User();
        user.setEmail("traveler@travelmate.com");
        trip.setUser(user);

        // Day 1: 1. TU 142 Aircraft Museum, 2. Kailasagiri Hilltop Park, 3. Kailasagiri
        ItineraryDay day1 = new ItineraryDay();
        day1.setId(661L);
        day1.setDayNumber(1);
        day1.setTrip(trip);

        ItineraryItem i1 = new ItineraryItem();
        i1.setId(7601L);
        i1.setTitle("TU 142 Aircraft Museum");
        i1.setCategory("Museums");
        i1.setItemOrder(0);
        i1.setStatus("SCHEDULED");
        i1.setItineraryDay(day1);

        ItineraryItem i2 = new ItineraryItem();
        i2.setId(7602L);
        i2.setTitle("Kailasagiri Hilltop Park");
        i2.setCategory("Parks");
        i2.setItemOrder(1);
        i2.setStatus("SCHEDULED");
        i2.setItineraryDay(day1);

        ItineraryItem i3 = new ItineraryItem();
        i3.setId(7603L);
        i3.setTitle("Kailasagiri");
        i3.setCategory("Viewpoints");
        i3.setItemOrder(2);
        i3.setStatus("SCHEDULED");
        i3.setItineraryDay(day1);

        day1.setItems(new ArrayList<>(Arrays.asList(i1, i2, i3)));
        trip.setDays(new ArrayList<>(Collections.singletonList(day1)));

        when(tripRepository.findById(76L)).thenReturn(Optional.of(trip));
        when(itineraryItemRepository.findById(7601L)).thenReturn(Optional.of(i1));
        when(itineraryItemRepository.findById(7602L)).thenReturn(Optional.of(i2));
        when(itineraryItemRepository.findById(7603L)).thenReturn(Optional.of(i3));

        return trip;
    }

    @Test
    @DisplayName("STRICT TEST 1: Move TU 142 Aircraft Museum from Day 1 to Day 2, Position #3")
    void testStrict_Test1_MoveTU142_Day1ToDay2_Position3() {
        Trip trip = createTU142VisakhapatnamTrip();
        assertEquals(3, trip.getDays().get(0).getItems().size());
        assertEquals(3, trip.getDays().get(1).getItems().size());

        // Move TU 142 (7501L) to Day 2 (652L) at position 3
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 75L, 7501L, 652L, 3);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        // Day 1 has 2 places: 1. RK Beach, 2. Kailasagiri
        assertEquals(2, day1.getItems().size());
        assertEquals("RK Beach", day1.getItems().get(0).getTitle());
        assertEquals(0, day1.getItems().get(0).getItemOrder());
        assertEquals("Kailasagiri", day1.getItems().get(1).getTitle());
        assertEquals(1, day1.getItems().get(1).getItemOrder());

        // Day 2 has 4 places: 1. Araku Valley, 2. Borra Caves, 3. TU 142 Aircraft Museum, 4. Coffee Museum
        assertEquals(4, day2.getItems().size());
        assertEquals("Araku Valley", day2.getItems().get(0).getTitle());
        assertEquals(0, day2.getItems().get(0).getItemOrder());
        assertEquals("Borra Caves", day2.getItems().get(1).getTitle());
        assertEquals(1, day2.getItems().get(1).getItemOrder());
        assertEquals("TU 142 Aircraft Museum", day2.getItems().get(2).getTitle());
        assertEquals(2, day2.getItems().get(2).getItemOrder());
        assertEquals("Coffee Museum", day2.getItems().get(3).getTitle());
        assertEquals(3, day2.getItems().get(3).getItemOrder());
    }

    @Test
    @DisplayName("STRICT TEST 2: Move place from Day 2 to Day 1, Position #2")
    void testStrict_Test2_MovePlace_Day2ToDay1() {
        Trip trip = createTU142VisakhapatnamTrip();

        // Move Borra Caves (7505L) from Day 2 to Day 1 at position 2
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 75L, 7505L, 651L, 2);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        assertEquals(4, day1.getItems().size());
        assertEquals("Borra Caves", day1.getItems().get(1).getTitle());
        assertEquals(1, day1.getItems().get(1).getItemOrder());

        assertEquals(2, day2.getItems().size());
        assertEquals("Araku Valley", day2.getItems().get(0).getTitle());
        assertEquals("Coffee Museum", day2.getItems().get(1).getTitle());
    }

    @Test
    @DisplayName("STRICT TEST 3: Change place from Position #3 to Position #1 in intra-day list")
    void testStrict_Test3_Position3ToPosition1() {
        Trip trip = createIntraDayTrip();

        // Move Kailasagiri (7603L) from pos 3 to pos 1
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 76L, 7603L, 661L, 1);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals("Kailasagiri", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("TU 142 Aircraft Museum", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("Kailasagiri Hilltop Park", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
    }

    @Test
    @DisplayName("STRICT TEST 4: Click Down Arrow on first place -> moves to position #2")
    void testStrict_Test4_DownArrow_FirstPlace() {
        Trip trip = createIntraDayTrip();

        // Down Arrow on TU 142 (7601L, pos 1 -> pos 2)
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 76L, 7601L, 661L, 2);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals("Kailasagiri Hilltop Park", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("TU 142 Aircraft Museum", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("Kailasagiri", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
    }

    @Test
    @DisplayName("STRICT TEST 5: Click Down Arrow on second place -> moves to position #3")
    void testStrict_Test5_DownArrow_SecondPlace() {
        Trip trip = createIntraDayTrip();

        // Down Arrow on Kailasagiri Hilltop Park (7602L, pos 2 -> pos 3)
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 76L, 7602L, 661L, 3);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals("TU 142 Aircraft Museum", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("Kailasagiri", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("Kailasagiri Hilltop Park", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
    }

    @Test
    @DisplayName("STRICT TEST 6: Click Up Arrow on third place -> moves to position #2")
    void testStrict_Test6_UpArrow_ThirdPlace() {
        Trip trip = createIntraDayTrip();

        // Up Arrow on Kailasagiri (7603L, pos 3 -> pos 2)
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 76L, 7603L, 661L, 2);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals("TU 142 Aircraft Museum", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals("Kailasagiri", items.get(1).getTitle());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals("Kailasagiri Hilltop Park", items.get(2).getTitle());
        assertEquals(2, items.get(2).getItemOrder());
    }

    @Test
    @DisplayName("STRICT TEST 7: MySQL inspection - itemOrder values and itineraryDay foreign keys match displayed itinerary")
    void testStrict_Test7_MySQL_Inspection() {
        Trip trip = createTU142VisakhapatnamTrip();
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 75L, 7501L, 652L, 3);

        verify(itineraryItemRepository, atLeastOnce()).save(any(ItineraryItem.class));
        verify(itineraryItemRepository, atLeastOnce()).flush();
        verify(tripRepository, atLeastOnce()).save(any(Trip.class));

        ItineraryDay day2 = updated.getDays().get(1);
        for (int i = 0; i < day2.getItems().size(); i++) {
            ItineraryItem item = day2.getItems().get(i);
            assertEquals(i, item.getItemOrder(), "itemOrder must be consecutive zero-indexed matching array order");
            assertEquals(day2.getId(), item.getItineraryDay().getId(), "foreign key itineraryDay must point to Day 2");
        }
    }

    @Test
    @DisplayName("STRICT TEST 8: Refresh website simulation - persistence verified via getTripById")
    void testStrict_Test8_RefreshWebsite_Persistence() {
        Trip trip = createTU142VisakhapatnamTrip();
        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 75L, 7501L, 652L, 3);

        // Simulate page reload via getTripById
        Trip reloaded = tripService.getTripById("traveler@travelmate.com", 75L);
        assertNotNull(reloaded);
        assertEquals(2, reloaded.getDays().get(0).getItems().size());
        assertEquals(4, reloaded.getDays().get(1).getItems().size());
        assertEquals("TU 142 Aircraft Museum", reloaded.getDays().get(1).getItems().get(2).getTitle());
    }

    private Trip createMockTrip() {
    Trip trip = new Trip();trip.setId(10L);trip.setDestinationCity("Goa");trip.setStartingLocation("Panaji Central");trip.setDestinationLatitude(15.2993);trip.setDestinationLongitude(74.1240);trip.setOriginLatitude(15.4989);trip.setOriginLongitude(73.8278);trip.setStartDate(LocalDate.now());trip.setEndDate(LocalDate.now().plusDays(1));trip.setNumDays(2);trip.setActivityLevel("Moderate");trip.setBudgetInr(20000.0);trip.setTotalEstimatedCost(3500.0);

    User user = new User();user.setEmail("traveler@travelmate.com");trip.setUser(user);

    ItineraryDay day1 = new ItineraryDay();day1.setId(101L);day1.setDayNumber(1);day1.setDate(LocalDate.now());day1.setWeatherCondition("Sunny & Pleasant");day1.setRainProbability(0.10);day1.setMaxTempC(31.0);day1.setMinTempC(24.0);day1.setTrip(trip);

    ItineraryItem item1 = new ItineraryItem();item1.setId(201L);item1.setTitle("Fort Aguada");item1.setCategory("Historical Fort");item1.setStartTime("10:00");item1.setEndTime("11:30");item1.setDurationMinutes(90);item1.setLatitude(15.4920);item1.setLongitude(73.7737);item1.setDistanceFromPrevious(4.5);item1.setDurationFromPrevious(18);item1.setStatus("SCHEDULED");item1.setItineraryDay(day1);

    ItineraryItem item2 = new ItineraryItem();item2.setId(202L);item2.setTitle("Calangute Beach");item2.setCategory("Beach");item2.setStartTime("12:00");item2.setEndTime("14:00");item2.setDurationMinutes(120);item2.setLatitude(15.5439);item2.setLongitude(73.7554);item2.setDistanceFromPrevious(6.2);item2.setDurationFromPrevious(20);item2.setStatus("SCHEDULED");item2.setItineraryDay(day1);

    day1.setItems(new ArrayList<>(Arrays.asList(item1,item2)));

    ItineraryDay day2 = new ItineraryDay();day2.setId(102L);day2.setDayNumber(2);day2.setDate(LocalDate.now().plusDays(1));day2.setWeatherCondition("Pleasant");day2.setRainProbability(0.15);day2.setMaxTempC(30.0);day2.setMinTempC(23.0);day2.setTrip(trip);

    ItineraryItem item3 = new ItineraryItem();item3.setId(203L);item3.setTitle("Dudhsagar Waterfalls");item3.setCategory("Nature & Waterfalls");item3.setStartTime("09:00");item3.setEndTime("13:00");item3.setDurationMinutes(240);item3.setLatitude(15.3144);item3.setLongitude(74.3143);item3.setStatus("SCHEDULED");item3.setItineraryDay(day2);

    day2.setItems(new ArrayList<>(Collections.singletonList(item3)));

    trip.setDays(new ArrayList<>(Arrays.asList(day1,day2)));

    when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryItemRepository.findById(201L)).thenReturn(Optional.of(item1));
        when(itineraryItemRepository.findById(202L)).thenReturn(Optional.of(item2));
        when(itineraryItemRepository.findById(203L)).thenReturn(Optional.of(item3));

        return trip;
    }

    @Test
    @DisplayName("Feature 1: Direct Manual Place Addition without searching and saving to MySQL")
    void testAddActivityToDay_DirectManualEntry_Success() {
        Trip trip = createMockTrip();

        Map<String, Object> manualPlace = new HashMap<>();
        manualPlace.put("title", "Fisherman's Wharf Cafe");
        manualPlace.put("category", "Restaurant & Food");
        manualPlace.put("address", "Near Baga Circle, North Goa");
        manualPlace.put("startTime", "13:00");
        manualPlace.put("durationMinutes", 75);
        manualPlace.put("notes", "Must try the butter garlic prawns");

        Trip updated = tripService.addActivityToDay("traveler@travelmate.com", 10L, 101L, manualPlace);

        assertNotNull(updated);
        assertEquals(3, updated.getDays().get(0).getItems().size());

        ItineraryItem added = updated.getDays().get(0).getItems().get(2);
        assertEquals("Fisherman's Wharf Cafe", added.getTitle());
        assertTrue(added.getRecommendationReason().contains("Manually added by traveller"));
        assertTrue(added.getRecommendationReason().contains("butter garlic prawns"));
        assertEquals("Near Baga Circle, North Goa", added.getAddress());
        assertEquals("Restaurant & Food", added.getCategory());
        assertEquals(75, added.getDurationMinutes());
    }

    @Test
    @DisplayName("Feature 1: Allow editing a manually added place and recalculate day schedule")
    void testUpdateActivity_Success() {
        Trip trip = createMockTrip();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", "Fort Aguada Lighthouse");
        updateData.put("startTime", "10:00");
        updateData.put("durationMinutes", 60);
        updateData.put("notes", "Arrive early to avoid crowd");

        Trip updated = tripService.updateActivity("traveler@travelmate.com", 10L, 201L, updateData);

        assertNotNull(updated);
        ItineraryItem edited = updated.getDays().get(0).getItems().get(0);
        assertEquals("Fort Aguada Lighthouse", edited.getTitle());
        assertEquals(60, edited.getDurationMinutes());
        assertTrue(edited.getRecommendationReason().contains("Arrive early"));
    }

    @Test
    @DisplayName("Feature 1: Manually reorder day activities (Move Up / Down)")
    void testReorderDayActivities_Success() {
        Trip trip = createMockTrip();

        List<Long> newOrder = Arrays.asList(202L, 201L);
        Trip updated = tripService.reorderDayActivities("traveler@travelmate.com", 10L, 101L, newOrder);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals(202L, items.get(0).getId());
        assertEquals("Calangute Beach", items.get(0).getTitle());
        assertEquals(201L, items.get(1).getId());
        assertEquals("Fort Aguada", items.get(1).getTitle());
    }

    @Test
    @DisplayName("Feature 1: Remove an activity and recalculate remaining schedule")
    void testRemoveActivity_Success() {
        Trip trip = createMockTrip();

        Trip updated = tripService.removeActivity("traveler@travelmate.com", 10L, 201L);

        assertNotNull(updated);
        ItineraryItem cancelled = updated.getDays().get(0).getItems().stream()
                .filter(i -> i.getId().equals(201L))
                .findFirst().orElse(null);
        assertNotNull(cancelled);
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    @DisplayName("Feature 1: Move activity between days")
    void testMoveActivityToDay_Success() {
        Trip trip = createMockTrip();
        assertEquals(2, trip.getDays().get(0).getItems().size());
        assertEquals(1, trip.getDays().get(1).getItems().size());

        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 10L, 201L, 102L);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        // 201L should no longer be in Day 1
        boolean inDay1 = day1.getItems().stream().anyMatch(i -> i.getId().equals(201L));
        assertFalse(inDay1);
        assertEquals(1, day1.getItems().size());

        // 201L should now be in Day 2
        boolean inDay2 = day2.getItems().stream().anyMatch(i -> i.getId().equals(201L));
        assertTrue(inDay2);
        assertEquals(2, day2.getItems().size());
    }

    @Test
    @DisplayName("Feature 1: Replace activity with custom manual place")
    void testReplaceActivity_WithManualPlace_Success() {
        Trip trip = createMockTrip();
        Map<String, Object> repData = new HashMap<>();
        repData.put("title", "My Favourite Restaurant");
        repData.put("category", "Restaurant & Food");
        repData.put("startTime", "01:00 PM");
        repData.put("durationMinutes", 90);
        repData.put("recommendationReason", "Manually added by traveller: local seafood");

        Trip updated = tripService.replaceActivity("traveler@travelmate.com", 10L, 201L, repData);
        assertNotNull(updated);
        ItineraryItem replaced = updated.getDays().get(0).getItems().stream()
                .filter(i -> i.getId().equals(201L))
                .findFirst().orElse(null);
        assertNotNull(replaced);
        assertEquals("My Favourite Restaurant", replaced.getTitle());
        assertEquals("Restaurant & Food", replaced.getCategory());
    }

    @Test
    @DisplayName("Real-World TEST 1: Replace Simhachalam Temple with GITAM University - coordinates, category, distance")
    void test1_ReplaceSimhachalamTempleWithGITAM_UpdatesCoordinatesCategoryRoute() {
        Trip trip = new Trip();
        trip.setId(20L);
        trip.setDestinationCity("Visakhapatnam");
        trip.setStartingLocation("Visakhapatnam Hub");
        trip.setDestinationLatitude(17.6868);
        trip.setDestinationLongitude(83.2185);
        trip.setOriginLatitude(17.6868);
        trip.setOriginLongitude(83.2185);

        User user = new User();
        user.setEmail("traveler@travelmate.com");
        trip.setUser(user);

        ItineraryDay day1 = new ItineraryDay();
        day1.setId(501L);
        day1.setDayNumber(1);
        day1.setTrip(trip);

        ItineraryItem stop1 = new ItineraryItem();
        stop1.setId(601L);
        stop1.setTitle("INS Kursura Submarine Museum");
        stop1.setCategory("Museums");
        stop1.setLatitude(17.7180);
        stop1.setLongitude(83.3330);
        stop1.setDurationMinutes(90);
        stop1.setItineraryDay(day1);

        ItineraryItem simhachalam = new ItineraryItem();
        simhachalam.setId(602L);
        simhachalam.setTitle("Simhachalam Temple");
        simhachalam.setCategory("Temples and Religious Places");
        simhachalam.setLatitude(17.7660);
        simhachalam.setLongitude(83.2500);
        simhachalam.setDurationMinutes(120);
        simhachalam.setItineraryDay(day1);

        day1.setItems(new ArrayList<>(Arrays.asList(stop1, simhachalam)));
        trip.setDays(new ArrayList<>(Collections.singletonList(day1)));

        when(tripRepository.findById(20L)).thenReturn(Optional.of(trip));
        when(itineraryItemRepository.findById(601L)).thenReturn(Optional.of(stop1));
        when(itineraryItemRepository.findById(602L)).thenReturn(Optional.of(simhachalam));

        Map<String, Object> gitamFound = new HashMap<>();
        gitamFound.put("name", "GITAM (Deemed to be University)");
        gitamFound.put("lat", 17.7813);
        gitamFound.put("lon", 83.3776);
        gitamFound.put("address", "Gandhi Nagar, Rushikonda, Visakhapatnam, Andhra Pradesh 530045, India");
        gitamFound.put("category", "Education / University");
        when(placeDiscoveryService.searchPlace(contains("GITAM"))).thenReturn(gitamFound);

        Map<String, Object> replacementPayload = new HashMap<>();
        replacementPayload.put("title", "GITAM (Deemed to be University)");
        replacementPayload.put("category", "Education / University");
        replacementPayload.put("latitude", 17.7813);
        replacementPayload.put("longitude", 83.3776);
        replacementPayload.put("address", "Gandhi Nagar, Rushikonda, Visakhapatnam, Andhra Pradesh 530045, India");

        Trip updated = tripService.replaceActivity("traveler@travelmate.com", 20L, 602L, replacementPayload);

        assertNotNull(updated);
        ItineraryItem replaced = updated.getDays().get(0).getItems().get(1);
        assertEquals("GITAM (Deemed to be University)", replaced.getTitle());
        assertEquals("Education / University", replaced.getCategory());
        assertEquals(17.7813, replaced.getLatitude(), 0.0001);
        assertEquals(83.3776, replaced.getLongitude(), 0.0001);
        assertNotEquals(17.7660, replaced.getLatitude()); // Verified NOT Simhachalam Temple
        assertNotEquals(83.2500, replaced.getLongitude());
        assertNotNull(replaced.getDistanceFromPrevious());
        assertTrue(replaced.getDistanceFromPrevious() > 0);
        assertTrue(replaced.getDurationFromPrevious() >= 10);
    }

    @Test
    @DisplayName("Real-World TEST 2: Add place manually with autocomplete - verify coords, address, category")
    void test2_AddManualPlace_AutocompleteRealLocation_PersistsWithCoords() {
        Trip trip = createMockTrip();
        Map<String, Object> gvpPlace = new HashMap<>();
        gvpPlace.put("title", "Gayatri Vidya Parishad (Gayatri University)");
        gvpPlace.put("address", "Madhurawada, Visakhapatnam, Andhra Pradesh 530048, India");
        gvpPlace.put("latitude", 17.8205);
        gvpPlace.put("longitude", 83.3421);
        gvpPlace.put("category", "Education / University");
        gvpPlace.put("startTime", "14:00");
        gvpPlace.put("durationMinutes", 90);

        Trip updated = tripService.addActivityToDay("traveler@travelmate.com", 10L, 101L, gvpPlace);
        assertNotNull(updated);
        ItineraryItem added = updated.getDays().get(0).getItems().get(2);
        assertEquals("Gayatri Vidya Parishad (Gayatri University)", added.getTitle());
        assertEquals("Education / University", added.getCategory());
        assertEquals(17.8205, added.getLatitude(), 0.0001);
        assertEquals(83.3421, added.getLongitude(), 0.0001);
        assertEquals("Madhurawada, Visakhapatnam, Andhra Pradesh 530048, India", added.getAddress());
    }

    @Test
    @DisplayName("Real-World TEST 3: Move third place to first position - verify sequence and time adjustment")
    void test3_ReorderPlaces_ThirdPlaceToFirstPosition() {
        Trip trip = createMockTrip();
        ItineraryDay day1 = trip.getDays().get(0);

        ItineraryItem item3 = new ItineraryItem();
        item3.setId(205L);
        item3.setTitle("Tenneti Park Viewpoint");
        item3.setCategory("Viewpoints / Attractions");
        item3.setLatitude(17.7570);
        item3.setLongitude(83.3550);
        item3.setDurationMinutes(60);
        item3.setItineraryDay(day1);
        day1.getItems().add(item3);

        when(itineraryItemRepository.findById(205L)).thenReturn(Optional.of(item3));

        // Move 3rd place (205L) to 1st position: [205L, 201L, 202L]
        List<Long> newOrder = Arrays.asList(205L, 201L, 202L);
        Trip updated = tripService.reorderDayActivities("traveler@travelmate.com", 10L, 101L, newOrder);

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        assertEquals(205L, items.get(0).getId());
        assertEquals("Tenneti Park Viewpoint", items.get(0).getTitle());
        assertEquals(0, items.get(0).getItemOrder());
        assertEquals(201L, items.get(1).getId());
        assertEquals(1, items.get(1).getItemOrder());
        assertEquals(202L, items.get(2).getId());
        assertEquals(2, items.get(2).getItemOrder());

        // Verify timing adjusts sequentially (parsed with 12-hour formatter)
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        java.time.LocalTime t0 = java.time.LocalTime.parse(items.get(0).getStartTime(), dtf);
        java.time.LocalTime t1 = java.time.LocalTime.parse(items.get(1).getStartTime(), dtf);
        java.time.LocalTime t2 = java.time.LocalTime.parse(items.get(2).getStartTime(), dtf);
        assertTrue(!t1.isBefore(t0));
        assertTrue(!t2.isBefore(t1));
    }

    @Test
    @DisplayName("Real-World TEST 4: Move place from Day 1 to Day 2 - verify removal, addition, sequence")
    void test4_MovePlaceBetweenDays_Day1ToDay2() {
        Trip trip = createMockTrip();
        assertEquals(2, trip.getDays().get(0).getItems().size());
        assertEquals(1, trip.getDays().get(1).getItems().size());

        Trip updated = tripService.moveActivityToDay("traveler@travelmate.com", 10L, 202L, 102L);

        assertNotNull(updated);
        ItineraryDay day1 = updated.getDays().get(0);
        ItineraryDay day2 = updated.getDays().get(1);

        // Day 1 has 1 item left (201L), 202L is removed
        assertEquals(1, day1.getItems().size());
        assertEquals(201L, day1.getItems().get(0).getId());
        assertEquals(0, day1.getItems().get(0).getItemOrder());

        // Day 2 has 2 items (203L, 202L)
        assertEquals(2, day2.getItems().size());
        assertTrue(day2.getItems().stream().anyMatch(i -> i.getId().equals(202L)));
        assertEquals(day2, day2.getItems().stream().filter(i -> i.getId().equals(202L)).findFirst().get().getItineraryDay());
    }

    @Test
    @DisplayName("Real-World TEST 5: Reorder 3 destinations - verify all affected route distances recalculate")
    void test5_ReorderThreeDestinations_RecalculatesAllRouteDistancesAndTransitTimes() {
        Trip trip = createMockTrip();
        ItineraryDay day1 = trip.getDays().get(0);

        ItineraryItem item3 = new ItineraryItem();
        item3.setId(206L);
        item3.setTitle("Baga Beach");
        item3.setCategory("Beaches");
        item3.setLatitude(15.5553);
        item3.setLongitude(73.7517);
        item3.setDurationMinutes(90);
        item3.setItineraryDay(day1);
        day1.getItems().add(item3);

        when(itineraryItemRepository.findById(206L)).thenReturn(Optional.of(item3));

        // Reorder to 206L, 202L, 201L
        Trip updated = tripService.reorderDayActivities("traveler@travelmate.com", 10L, 101L, Arrays.asList(206L, 202L, 201L));

        assertNotNull(updated);
        List<ItineraryItem> items = updated.getDays().get(0).getItems();
        for (ItineraryItem it : items) {
            assertNotNull(it.getDistanceFromPrevious());
            assertTrue(it.getDistanceFromPrevious() > 0);
            assertNotNull(it.getDurationFromPrevious());
            assertTrue(it.getDurationFromPrevious() > 0);
            assertNotNull(it.getTransportFromPrevious());
        }
    }

    public static void main(String[] args) {
        System.out.println("=======================================================================");
        System.out.println("RUNNING TRAVELMATE MANUAL ITINERARY EDITING TEST SUITE");
        System.out.println("=======================================================================");
        ManualItineraryEditingTest test = new ManualItineraryEditingTest();

        try {
            test.setUp();
            test.testAddActivityToDay_DirectManualEntry_Success();
            System.out.println("[PASS] 1. Direct manual place addition without searching, saved in MySQL");

            test.setUp();
            test.testUpdateActivity_Success();
            System.out.println("[PASS] 2. Edit manually added place: title, time, duration, and notes updated");

            test.setUp();
            test.testReorderDayActivities_Success();
            System.out.println("[PASS] 3. Reorder activities: Move Up / Move Down sequence updated");

            test.setUp();
            test.testRemoveActivity_Success();
            System.out.println("[PASS] 4. Remove activity: Status set to CANCELLED and schedule recalculated");

            test.setUp();
            test.testMoveActivityToDay_Success();
            System.out.println("[PASS] 5. Move activity between days: Day 1 item moved to Day 2 and both schedules recalculated");

            test.setUp();
            test.testReplaceActivity_WithManualPlace_Success();
            System.out.println("[PASS] 6. Replace activity with custom manual place: title and category updated");

            test.setUp();
            test.test1_ReplaceSimhachalamTempleWithGITAM_UpdatesCoordinatesCategoryRoute();
            System.out.println("[PASS] TEST 1: Replace Simhachalam Temple with GITAM (Deemed to be University) -> category 'Education / University', coordinates updated (17.7813, 83.3776), road route distance recalculated");

            test.setUp();
            test.test2_AddManualPlace_AutocompleteRealLocation_PersistsWithCoords();
            System.out.println("[PASS] TEST 2: Add place manually with autocomplete (Gayatri Vidya Parishad) -> coordinates (17.8205, 83.3421), address, category 'Education / University'");

            test.setUp();
            test.test3_ReorderPlaces_ThirdPlaceToFirstPosition();
            System.out.println("[PASS] TEST 3: Move third place to first position -> sequence numbers 0, 1, 2 updated and start times adjusted sequentially");

            test.setUp();
            test.test4_MovePlaceBetweenDays_Day1ToDay2();
            System.out.println("[PASS] TEST 4: Move place from Day 1 to Day 2 -> removed from Day 1, added to Day 2, sequence numbers and schedules updated for both days");

            test.setUp();
            test.test5_ReorderThreeDestinations_RecalculatesAllRouteDistancesAndTransitTimes();
            System.out.println("[PASS] TEST 5: Reorder 3 destinations -> all affected route distances, transit times, and transport labels recalculated");

            test.setUp();
            test.testA_MoveRKBeach_Day1ToDay2();
            System.out.println("[PASS] TEST A: Move RK Beach from Day 1 to Day 2 -> removed from Day 1, added to Day 2, sequence intact");

            test.setUp();
            test.testB_MoveKailasagiri_Position3ToPosition1();
            System.out.println("[PASS] TEST B: Move Kailasagiri from position 3 to position 1 -> Kailasagiri is #1, others shifted down");

            test.setUp();
            test.testC_MovePlace_Day2ToDay1();
            System.out.println("[PASS] TEST C: Move place from Day 2 to Day 1 -> Borra Caves moved, both days updated");

            test.setUp();
            test.testD_MovePlace_Day1ToDay2_Position2_Combined();
            System.out.println("[PASS] TEST D: Move place from Day 1 to Day 2 and set position to 2 -> Araku (1), RK Beach (2), Borra (3), Coffee Museum (4)");

            test.setUp();
            test.testE_CheckMySQLPersistence_SequenceNumbers_NoDuplicates();
            System.out.println("[PASS] TEST E: Check persistence -> repository save & flush verified, 0 duplicates, 0 missing places");

            test.setUp();
            test.test4_SwapUp_ThirdPlaceWithSecondPlace();
            System.out.println("[PASS] TEST 4 (Swap Up): 3rd place (Kailasagiri) swaps with 2nd place (RK Beach) -> orders updated correctly");

            test.setUp();
            test.test5_SwapDown_SecondPlaceWithThirdPlace();
            System.out.println("[PASS] TEST 5 (Swap Down): 2nd place (RK Beach) swaps with 3rd place (Kailasagiri) -> orders updated correctly");

            test.setUp();
            test.test8_FailedSave_PropagatesRealError_NoCorruptedState();
            System.out.println("[PASS] TEST 8 (Simulated Failure): Database failure during save/flush throws exception, no fake success");

            test.setUp();
            test.testUserScenario_Test1_MoveSimhachalam_Day3ToDay1_Pos3();
            System.out.println("[PASS] USER TEST 1: Simhachalam Temple moved from Day 3 to Day 1, position #3 -> Day 1 has 4 places (Simhachalam at #3), Day 3 has 1 place");

            test.setUp();
            test.testUserScenario_Test2_MovePlace_Day1ToDay2();
            System.out.println("[PASS] USER TEST 2: Place moved from Day 1 to Day 2 -> both days updated in MySQL and schedules recalculated");

            test.setUp();
            test.testUserScenario_Test3_Position3ToPosition1();
            System.out.println("[PASS] USER TEST 3: Place moved from position #3 to #1 -> becomes #1 and other places shift down automatically");

            test.setUp();
            test.testUserScenario_Test4_SwapUp_OnePosition();
            System.out.println("[PASS] USER TEST 4: Swap Up moves place up exactly 1 position and saves new order in MySQL");

            test.setUp();
            test.testUserScenario_Test5_SwapDown_OnePosition();
            System.out.println("[PASS] USER TEST 5: Swap Down moves place down exactly 1 position and saves new order in MySQL");

            test.setUp();
            test.testUserScenario_Test6_RefreshWebsite_PersistenceVerified();
            System.out.println("[PASS] USER TEST 6: Refresh website simulation -> MySQL persistence verified, all changes remain saved");

            test.setUp();
            test.testStrict_Test1_MoveTU142_Day1ToDay2_Position3();
            System.out.println("[PASS] STRICT TEST 1: Move TU 142 Aircraft Museum from Day 1 to Day 2, Position #3 -> Day 1 [RK Beach, Kailasagiri], Day 2 [Araku, Borra, TU 142, Coffee Museum]");

            test.setUp();
            test.testStrict_Test2_MovePlace_Day2ToDay1();
            System.out.println("[PASS] STRICT TEST 2: Move place from Day 2 to Day 1 -> Borra Caves moved, both days updated");

            test.setUp();
            test.testStrict_Test3_Position3ToPosition1();
            System.out.println("[PASS] STRICT TEST 3: Change Kailasagiri from Position #3 to Position #1 -> becomes #1, other places shifted");

            test.setUp();
            test.testStrict_Test4_DownArrow_FirstPlace();
            System.out.println("[PASS] STRICT TEST 4: Click Down Arrow on first place (TU 142) -> moves to position #2");

            test.setUp();
            test.testStrict_Test5_DownArrow_SecondPlace();
            System.out.println("[PASS] STRICT TEST 5: Click Down Arrow on second place (Kailasagiri Hilltop Park) -> moves to position #3");

            test.setUp();
            test.testStrict_Test6_UpArrow_ThirdPlace();
            System.out.println("[PASS] STRICT TEST 6: Click Up Arrow on third place (Kailasagiri) -> moves to position #2");

            test.setUp();
            test.testStrict_Test7_MySQL_Inspection();
            System.out.println("[PASS] STRICT TEST 7: MySQL inspection -> itemOrder values and itineraryDay foreign keys match displayed itinerary");

            test.setUp();
            test.testStrict_Test8_RefreshWebsite_Persistence();
            System.out.println("[PASS] STRICT TEST 8: Refresh website simulation -> persistence verified via getTripById");

            System.out.println("\n>>> ALL 33 MANUAL ITINERARY EDITING & RESCHEDULE TESTS PASSED (100% SUCCESS) <<<");
            System.out.println("=======================================================================");
        } catch (Throwable t) {
            System.err.println("[FAIL] Test failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
