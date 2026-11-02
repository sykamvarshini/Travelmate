import requests

def test_flow():
    print("Logging in...")
    r = requests.post('http://localhost:8080/api/auth/login', json={
        'email': 'python1@test.com',
        'password': 'password123'
    })
    token = r.json().get('token')
    if not token:
        print("Failed to get token!")
        return

    print("Creating trip...")
    trip_data = {
        "destinationCity": "Goa",
        "startDate": "2026-12-01",
        "endDate": "2026-12-03",
        "travelStyle": "Relaxation",
        "interests": ["Beaches", "Food"],
        "budgetInr": 20000.0,
        "numTravelers": 2
    }
    r_create = requests.post('http://localhost:8080/api/trips', json=trip_data, headers={'Authorization': f'Bearer {token}'})
    print("Create status:", r_create.status_code)
    
    trip = r_create.json()
    print("Trip:", trip)
    
    if "days" in trip:
        print(f"Generated {len(trip['days'])} days!")
        for day in trip['days']:
            print(f"Day {day['dayNumber']}: {len(day.get('items', []))} items")
            for item in day.get('items', []):
                print(f"  - {item['startTime']} | {item['title']} | INR {item['costInr']}")
    else:
        print("No days array returned!")

if __name__ == '__main__':
    test_flow()
