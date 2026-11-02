import re

# Update TripController
with open('backend/src/main/java/com/travelmate/controller/TripController.java', 'r') as f:
    tc_content = f.read()

get_and_adapt = """
    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTrip(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Trip trip = tripService.getTripById(email, id);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/adapt")
    public ResponseEntity<Trip> adaptTripDay(
            @PathVariable Long id, 
            @RequestParam Long dayId, 
            @RequestParam String weatherCondition, 
            @RequestParam Double rainProb, 
            @RequestParam Double temp) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Trip adaptedTrip = tripService.adaptTripDay(email, id, dayId, weatherCondition, rainProb, temp);
        return ResponseEntity.ok(adaptedTrip);
    }
"""

tc_content = tc_content.replace('public ResponseEntity<?> deleteTrip(@PathVariable Long id) {', get_and_adapt + '\n    @DeleteMapping("/{id}")\n    public ResponseEntity<?> deleteTrip(@PathVariable Long id) {')

with open('backend/src/main/java/com/travelmate/controller/TripController.java', 'w') as f:
    f.write(tc_content)


# Update TripService
with open('backend/src/main/java/com/travelmate/service/TripService.java', 'r') as f:
    ts_content = f.read()

adapt_service = """
    public Trip getTripById(String userEmail, Long id) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        Optional<Trip> tripOpt = tripRepository.findById(id);
        if (tripOpt.isEmpty() || !tripOpt.get().getUser().getId().equals(userOpt.get().getId())) {
            throw new RuntimeException("Trip not found");
        }
        return tripOpt.get();
    }

    public Trip adaptTripDay(String userEmail, Long tripId, Long dayId, String weatherCondition, Double rainProb, Double temp) {
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
        
        // Update weather context on the day
        targetDay.setWeatherCondition(weatherCondition);
        targetDay.setRainProbability(rainProb);
        targetDay.setMaxTempC(temp);
        
        // Re-evaluate items using ML context re-rank
        // Here we simulate the logic by shifting outdoor items out or tagging them
        boolean needsAdaptation = rainProb > 0.40;
        
        if (needsAdaptation) {
            for (ItineraryItem item : targetDay.getItems()) {
                if ("Outdoor".equalsIgnoreCase(item.getIndoorOutdoor())) {
                    item.setStatus("REPLACED");
                    item.setWeatherAlert("Outdoor activity skipped due to rain.");
                    // Ideally, add a new indoor activity here via ML /recommend
                } else {
                    item.setRecommendationReason("Safe indoor activity for rainy weather.");
                }
            }
            
            // Recalculate cost
            double total = trip.getDays().stream()
                    .flatMap(d -> d.getItems().stream())
                    .filter(i -> !"REPLACED".equals(i.getStatus()))
                    .mapToDouble(ItineraryItem::getCostInr)
                    .sum();
            trip.setTotalEstimatedCost(total);
            
            return tripRepository.save(trip);
        }
        
        return trip;
    }
"""

ts_content = ts_content.replace('public void deleteTrip(String userEmail, Long id) {', adapt_service + '\n    public void deleteTrip(String userEmail, Long id) {')

with open('backend/src/main/java/com/travelmate/service/TripService.java', 'w') as f:
    f.write(ts_content)

print("Updated TripController and TripService")
