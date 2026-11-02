ts_path = r"E:\travel\backend\src\main\java\com\travelmate\service\TripService.java"
with open(ts_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace the deduplication logic in createTrip
old_dedup = """                        // Strict Deduplication across all days of the trip
                        String titleKey = titleStr.toLowerCase().trim();
                        if (scheduledTitles.contains(titleKey)) {
                            continue;
                        }
                        scheduledTitles.add(titleKey);"""

new_dedup = """                        // Strict Deduplication across all days of the trip
                        String normKey = normalizeAttractionKey(titleStr);
                        if (isAlreadyScheduled(normKey, scheduledTitles)) {
                            continue;
                        }
                        scheduledTitles.add(normKey);"""

if old_dedup in content:
    content = content.replace(old_dedup, new_dedup)
    print("Replaced deduplication logic in TripService.java.")
else:
    print("Warning: old_dedup not found directly.")

# Add helper methods before closing brace
helpers = """
    private String normalizeAttractionKey(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("\\\\(.*?\\\\)", "").replaceAll("[^a-z0-9]", "").trim();
    }

    private boolean isAlreadyScheduled(String normKey, Set<String> scheduledKeys) {
        if (normKey.isEmpty()) return false;
        if (scheduledKeys.contains(normKey)) return true;
        for (String existing : scheduledKeys) {
            if (existing.equals(normKey) || 
                (normKey.length() >= 6 && existing.contains(normKey)) || 
                (existing.length() >= 6 && normKey.contains(existing))) {
                return true;
            }
        }
        return false;
    }
"""

last_brace = content.rfind("}")
content = content[:last_brace] + helpers + "\n}\n"

with open(ts_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Added normalizeAttractionKey and isAlreadyScheduled to TripService.java")
