import requests
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080"

# 1. Login to get token
login_payload = {
    "email": "traveler@travelmate.com",
    "password": "password123"
}

# Try logging in or register if needed
s = requests.Session()
login_res = s.post(f"{BASE_URL}/api/auth/login", json=login_payload)
token = None
if login_res.status_code == 200:
    data = login_res.json()
    token = data.get("token")
    print(f"Logged in successfully. User: {data.get('email')}")
else:
    # Try registering
    reg_payload = {
        "fullName": "Traveler User",
        "email": "traveler@travelmate.com",
        "password": "password123"
    }
    reg_res = s.post(f"{BASE_URL}/api/auth/register", json=reg_payload)
    if reg_res.status_code in [200, 201]:
        data = reg_res.json()
        token = data.get("token")
        print(f"Registered and logged in. User: {data.get('email')}")
    else:
        print(f"Auth failed: {reg_res.text}")
        sys.exit(1)

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

print("\n--- TEST SCENARIO A: 2-Hour Local Trip (Hyderabad -> Kukatpally, Food, <= 5km) ---")
trip_req_a = {
    "startingLocation": "Hyderabad",
    "destinationCity": "Hyderabad",
    "locality": "Kukatpally",
    "interests": ["Food", "Shopping"],
    "durationHours": 2,
    "startTime": "14:00",
    "availableUntil": "16:00",
    "maxDistanceKm": 5.0,
    "maxEntryFee": 200.0,
    "travelDistancePref": "Nearby",
    "travelStyle": "Moderate",
    "budgetInr": 5000.0,
    "numDays": 1,
    "startDate": "2026-09-28",
    "endDate": "2026-09-28",
    "numTravelers": 1,
    "transitPreference": "Cab / Auto"
}

res_a = s.post(f"{BASE_URL}/api/trips", json=trip_req_a, headers=headers)
print(f"Scenario A Response: Status {res_a.status_code}")
if res_a.status_code in [200, 201]:
    trip_a = res_a.json()
    print(f"Trip ID: {trip_a.get('id')}")
    print(f"Destination: {trip_a.get('destinationCity')}, Locality: {trip_a.get('locality')}")
    days = trip_a.get("days", [])
    print(f"Num Days: {len(days)}")
    for d in days:
        items = d.get("items", [])
        print(f"  Day {d.get('dayNumber')}: {len(items)} activities scheduled:")
        for it in items:
            print(f"    - {it.get('title')} ({it.get('startTime')} - duration {it.get('durationMinutes')}m) | Cost: ₹{it.get('costInr')} | Status: {it.get('status')} | Rating: {it.get('rating')}")
else:
    print(f"Error: {res_a.text}")

print("\n--- TEST SCENARIO B: 3-Hour Trip (Vijayawada -> Kanaka Durga Temple, Spiritual/Heritage) ---")
trip_req_b = {
    "startingLocation": "Vijayawada",
    "destinationCity": "Vijayawada",
    "locality": "Kanaka Durga Temple",
    "interests": ["Spiritual", "Heritage"],
    "durationHours": 3,
    "startTime": "09:00",
    "availableUntil": "12:00",
    "maxDistanceKm": 5.0,
    "maxEntryFee": 100.0,
    "travelDistancePref": "Nearby",
    "travelStyle": "Relaxed",
    "budgetInr": 3000.0,
    "numDays": 1,
    "startDate": "2026-09-28",
    "endDate": "2026-09-28",
    "numTravelers": 1,
    "transitPreference": "Auto / Cab"
}

res_b = s.post(f"{BASE_URL}/api/trips", json=trip_req_b, headers=headers)
print(f"Scenario B Response: Status {res_b.status_code}")
if res_b.status_code in [200, 201]:
    trip_b = res_b.json()
    trip_id_b = trip_b.get("id")
    print(f"Trip ID: {trip_id_b}")
    items_b = trip_b.get("days", [])[0].get("items", [])
    for it in items_b:
        print(f"  Activity #{it.get('id')}: {it.get('title')} ({it.get('startTime')}) - Status: {it.get('status')} | Rating: {it.get('rating')}")

    if len(items_b) >= 2:
        act1 = items_b[0]
        act2 = items_b[1]
        
        print("\n--- TEST SCENARIO C: Mark First Activity Completed & Verify Next Activity ---")
        comp_res = s.post(f"{BASE_URL}/api/trips/{trip_id_b}/activities/{act1.get('id')}/complete", headers=headers)
        print(f"Complete Activity Response: Status {comp_res.status_code}")
        print(f"Result: {comp_res.json()}")

        print("\n--- TEST SCENARIO D: Weather Context Check with Simulated Rain ---")
        check_res = s.get(f"{BASE_URL}/api/trips/{trip_id_b}/activities/{act1.get('id')}/next-check?weatherOverride=rain", headers=headers)
        print(f"Next Destination Check Response: Status {check_res.status_code}")
        check_data = check_res.json()
        print(f"Next Place: {check_data.get('nextActivityTitle')}")
        print(f"Is Suitable: {check_data.get('isSuitable')}")
        print(f"Rain Probability: {check_data.get('rainProbability')}%")
        print(f"Alert Message: {check_data.get('alertMessage')}")
        print(f"Traffic: {check_data.get('trafficLevel')}, Crowd: {check_data.get('crowdLevel')}")

        print("\n--- TEST SCENARIO E: Fetch Alternatives & Reschedule Single Place ---")
        alts_res = s.get(f"{BASE_URL}/api/trips/{trip_id_b}/activities/{act2.get('id')}/alternatives?reason=Weather%20concern", headers=headers)
        print(f"Alternatives Response: Status {alts_res.status_code}")
        alts = alts_res.json()
        print(f"Found {len(alts)} Real Verified Alternatives:")
        for a in alts[:3]:
            print(f"  - {a.get('title')} ({a.get('category')}) | ₹{a.get('costInr')} | {a.get('indoorOutdoor')} | Rating: {a.get('rating')}")

        if len(alts) > 0:
            chosen = alts[0]
            print(f"\nReplacing Activity #{act2.get('id')} ({act2.get('title')}) with: {chosen.get('title')}")
            repl_res = s.post(f"{BASE_URL}/api/trips/{trip_id_b}/activities/{act2.get('id')}/replace", json=chosen, headers=headers)
            print(f"Replace Response: Status {repl_res.status_code}")
            
            # Fetch updated trip to verify persistence
            trip_refreshed = s.get(f"{BASE_URL}/api/trips/{trip_id_b}", headers=headers).json()
            refreshed_items = trip_refreshed.get("days", [])[0].get("items", [])
            print("Refreshed Trip Itinerary items:")
            for rit in refreshed_items:
                print(f"  - #{rit.get('id')}: {rit.get('title')} [{rit.get('status')}] (Time: {rit.get('startTime')})")
