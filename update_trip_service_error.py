import re

with open('backend/src/main/java/com/travelmate/service/TripService.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the save logic and catch block to throw RuntimeException
old_save_logic = """                // Save again with days
                savedTrip = tripRepository.save(trip);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch ML recommendations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return savedTrip;"""

new_save_logic = """                // Save again with days
                savedTrip = tripRepository.save(trip);
            } else {
                throw new RuntimeException("Couldn't verify this location. Try another spelling or select a suggested location.");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch ML recommendations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Couldn't verify this location. Try another spelling or select a suggested location.");
        }
        
        return savedTrip;"""

content = content.replace(old_save_logic, new_save_logic)

with open('backend/src/main/java/com/travelmate/service/TripService.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated TripService.java")
