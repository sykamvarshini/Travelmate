import re

with open('backend/src/main/java/com/travelmate/service/TripService.java', 'r', encoding='utf-8') as f:
    content = f.read()

methods_to_add = """
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
        
        targetDay.setWeatherCondition(weatherCondition);
        targetDay.setRainProbability(rainProb);
        targetDay.setMaxTempC(temp);
        
        boolean needsAdaptation = rainProb > 0.40;
        
        if (needsAdaptation) {
            for (ItineraryItem item : targetDay.getItems()) {
                if ("Outdoor".equalsIgnoreCase(item.getIndoorOutdoor())) {
                    item.setStatus("REPLACED");
                    item.setWeatherAlert("Outdoor activity skipped due to rain.");
                } else {
                    item.setRecommendationReason("Safe indoor activity for rainy weather.");
                }
            }
            
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

if "getTripById" not in content:
    # Insert before the last closing brace
    last_brace_index = content.rfind("}")
    if last_brace_index != -1:
        content = content[:last_brace_index] + methods_to_add + "\n}"
    
    with open('backend/src/main/java/com/travelmate/service/TripService.java', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Added getTripById and adaptTripDay")
else:
    print("Methods already exist")
