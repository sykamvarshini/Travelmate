import re

# Read TripService.java
with open('backend/src/main/java/com/travelmate/service/TripService.java', 'r') as f:
    content = f.read()

# I want to add some imports at the top
imports = """
import com.travelmate.entity.ItineraryDay;
import com.travelmate.entity.ItineraryItem;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
"""
content = content.replace('import java.util.Optional;', 'import java.util.Optional;\n' + imports)


replacement_code = """
        try {
            Map<String, Object> response = mlIntegrationService.getRecommendations(mlRequest);
            System.out.println("ML Recommendations received: " + response.get("count"));
            
            List<Map<String, Object>> recs = (List<Map<String, Object>>) response.get("recommendations");
            if (recs != null && !recs.isEmpty()) {
                long numDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
                trip.setNumDays((int) numDays);
                
                int itemsPerDay = Math.max(1, recs.size() / (int) numDays);
                if (itemsPerDay > 4) itemsPerDay = 4; // Max 4 activities a day
                
                int recIndex = 0;
                for (int i = 0; i < numDays; i++) {
                    ItineraryDay day = new ItineraryDay();
                    day.setTrip(trip);
                    day.setDayNumber(i + 1);
                    day.setDate(trip.getStartDate().plusDays(i));
                    
                    // Simple mock weather for now
                    day.setWeatherCondition("Sunny");
                    day.setMaxTempC(30.0);
                    day.setMinTempC(22.0);
                    
                    // Assign items to this day
                    int itemsToday = 0;
                    String[] times = {"09:00", "12:00", "15:00", "18:00"};
                    
                    while (itemsToday < itemsPerDay && recIndex < recs.size()) {
                        Map<String, Object> rec = recs.get(recIndex);
                        ItineraryItem item = new ItineraryItem();
                        item.setItineraryDay(day);
                        
                        item.setTitle(rec.getOrDefault("name", "Activity").toString());
                        item.setCategory(rec.getOrDefault("category", "Sightseeing").toString());
                        item.setStartTime(times[itemsToday % times.length]);
                        item.setDurationMinutes((int) Double.parseDouble(rec.getOrDefault("duration_hours", "2.0").toString()) * 60);
                        item.setCostInr(Double.parseDouble(rec.getOrDefault("entry_fee_inr", "0").toString()));
                        item.setIndoorOutdoor(rec.getOrDefault("indoor_outdoor", "Outdoor").toString());
                        
                        if (rec.containsKey("latitude") && rec.get("latitude") != null) {
                            item.setLatitude(Double.parseDouble(rec.get("latitude").toString()));
                        }
                        if (rec.containsKey("longitude") && rec.get("longitude") != null) {
                            item.setLongitude(Double.parseDouble(rec.get("longitude").toString()));
                        }
                        
                        // item.setRecommendationReason("Matched your interest in " + item.getCategory() + " (Score: " + rec.getOrDefault("predicted_score", "0.9") + ")");
                        
                        item.setItemOrder(itemsToday);
                        day.getItems().add(item);
                        
                        recIndex++;
                        itemsToday++;
                    }
                    
                    trip.getDays().add(day);
                }
                
                // Calculate total budget based on items
                double total = trip.getDays().stream()
                        .flatMap(d -> d.getItems().stream())
                        .mapToDouble(ItineraryItem::getCostInr)
                        .sum();
                trip.setTotalEstimatedCost(total);
                
                // Save again with days
                savedTrip = tripRepository.save(trip);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch ML recommendations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return savedTrip;
"""

import re

# We want to replace the try block in createTrip
content = re.sub(r'try \{.*?return savedTrip;', replacement_code, content, flags=re.DOTALL)

with open('backend/src/main/java/com/travelmate/service/TripService.java', 'w') as f:
    f.write(content)
print("Updated TripService.java")
