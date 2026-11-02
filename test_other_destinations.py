import requests

base_url = "http://localhost:8080/api"
headers = {"Content-Type": "application/json"}

for city in ["Vijayawada", "Goa", "Jaipur"]:
    payload = {
        "destinationCity": city,
        "startingLocation": "Hyderabad",
        "startDate": "2026-11-05",
        "endDate": "2026-11-06",
        "numDays": 2,
        "budgetInr": 20000,
        "travelStyle": "Solo",
        "activityLevel": "Moderate",
        "foodPreference": "Any",
        "transitPreference": "Train",
        "interests": ["Heritage", "Culture"]
    }
    res = requests.post(f"{base_url}/trips", json=payload, headers=headers)
    if res.status_code == 200:
        trip = res.json()
        print(f"\n==================== {city.upper()} ====================")
        for day in trip.get("days", []):
            print(f"Day {day.get('dayNumber')}:")
            for item in day.get("items", []):
                print(f"  - [{item.get('category')}] {item.get('title')} (Rating: {item.get('rating')}, Match: {item.get('aiMatchScore')}%)")
    else:
        print(f"Failed for {city}: {res.status_code} {res.text}")
