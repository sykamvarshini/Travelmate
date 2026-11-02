import requests
import json

base_url = "http://localhost:8080/api"

# Login with demo user
login_payload = {
    "email": "traveler@travelmate.com",
    "password": "Password123!"
}

token = None
try:
    res = requests.post(f"{base_url}/auth/login", json=login_payload)
    if res.status_code == 200:
        token = res.json().get("token")
        print("Logged in successfully, token received.")
    else:
        print(f"Login failed: {res.status_code} {res.text}")
        # Try register
        reg_res = requests.post(f"{base_url}/auth/register", json={
            "fullName": "Traveler",
            "email": "traveler@travelmate.com",
            "password": "Password123!"
        })
        print("Register response:", reg_res.status_code)
        if reg_res.status_code == 200:
            token = reg_res.json().get("token")
except Exception as e:
    print("Auth exception:", e)

headers = {"Content-Type": "application/json"}
if token:
    headers["Authorization"] = f"Bearer {token}"

# Create a trip to Visakhapatnam
trip_payload = {
    "destinationCity": "Visakhapatnam",
    "startingLocation": "Hyderabad",
    "startDate": "2026-11-01",
    "endDate": "2026-11-03",
    "numDays": 3,
    "budgetInr": 25000,
    "travelStyle": "Solo",
    "activityLevel": "Moderate",
    "foodPreference": "Any",
    "transitPreference": "Train",
    "interests": ["Beaches", "Nature", "Museums"]
}

res = requests.post(f"{base_url}/trips", json=trip_payload, headers=headers)
print("Create trip status:", res.status_code)
if res.status_code == 200:
    trip = res.json()
    print(f"Trip created! ID: {trip.get('id')}, Title: {trip.get('title')}")
    days = trip.get("days", [])
    print(f"Total days: {len(days)}")
    for day in days:
        print(f"\n--- Day {day.get('dayNumber')} ({day.get('date')}) ---")
        for item in day.get("items", []):
            print(f"  * [{item.get('category')}] {item.get('title')} ({item.get('startTime')} - {item.get('endTime')})")
            print(f"    Match: {item.get('aiMatchScore')}%, Rating: {item.get('rating')}, Reason: {item.get('recommendationReason')}")
else:
    print("Error creating trip:", res.text)
