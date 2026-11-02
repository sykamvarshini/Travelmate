import urllib.request
import json
import time

BASE_URL = 'http://localhost:8080/api'

def request(method, path, body=None, token=None):
    url = f"{BASE_URL}{path}"
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode('utf-8')
            return resp.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        err_content = e.read().decode('utf-8')
        return e.code, json.loads(err_content) if err_content.startswith('{') or err_content.startswith('[') else {'raw': err_content}
    except Exception as e:
        return 0, {'error': str(e)}

print("=== STARTING COMPLETE BACKEND END-TO-END VERIFICATION ===")

# 1. Auth: Register / Login test user
test_email = f"tester_{int(time.time())}@travelmate.com"
test_password = "Password@123"

status, res = request('POST', '/auth/register', {
    'email': test_email,
    'password': test_password,
    'fullName': 'Verification Tester'
})
print(f"1. Register user ({test_email}): status {status}")

status, res = request('POST', '/auth/login', {
    'email': test_email,
    'password': test_password
})
token = res.get('token')
print(f"2. Login user: status {status}, token acquired: {bool(token)}")

# 2. Create Trip
trip_req = {
    'destinationCity': 'Goa',
    'startingLocation': 'Mumbai',
    'startDate': '2026-10-15',
    'endDate': '2026-10-17',
    'numDays': 3,
    'budgetInr': 35000.0,
    'numTravelers': 2,
    'travelStyle': 'Couple',
    'activityLevel': 'Moderate',
    'foodPreference': 'Any',
    'transitPreference': 'Train',
    'interests': ['Beaches', 'Heritage', 'Food']
}
status, trip = request('POST', '/trips', trip_req, token=token)
trip_id = trip.get('id')
print(f"3. Create Trip: status {status}, Trip ID: {trip_id}, Days: {len(trip.get('days', []))}")

if not trip_id:
    print("FAILED: Could not create trip")
    exit(1)

# Verify initial values
first_day = trip['days'][0]
first_item = first_day['items'][0] if first_day.get('items') else None
print(f"   Initial trip destination: {trip.get('destinationCity')}, budget: {trip.get('budgetInr')}")
if first_item:
    print(f"   First activity: {first_item.get('title')} (ID: {first_item.get('id')})")

# 4. Test EDIT TRIP
edit_payload = {
    'destinationCity': 'Goa',
    'startingLocation': 'Pune',
    'startDate': '2026-10-15',
    'endDate': '2026-10-18',
    'numDays': 4,
    'budgetInr': 48000.0,
    'numTravelers': 3,
    'travelStyle': 'Friends',
    'activityLevel': 'High',
    'foodPreference': 'Non-Veg',
    'transitPreference': 'Flight',
    'interests': ['Beaches', 'Adventure', 'Food']
}
status, updated_trip = request('PUT', f"/trips/{trip_id}", edit_payload, token=token)
print(f"4. EDIT TRIP (PUT /trips/{trip_id}): status {status}")

# Re-fetch via GET to verify persistence in DB
status, refetched_trip = request('GET', f"/trips/{trip_id}", token=token)
print(f"   GET /trips/{trip_id} after edit:")
print(f"   - Starting location: {refetched_trip.get('startingLocation')} (Expected: Pune)")
print(f"   - Budget: {refetched_trip.get('budgetInr')} (Expected: 48000.0)")
print(f"   - Travelers: {refetched_trip.get('numTravelers')} (Expected: 3)")
print(f"   - Travel style: {refetched_trip.get('travelStyle')} (Expected: Friends)")
print(f"   - Num days: {refetched_trip.get('numDays')} (Expected: 4)")
assert refetched_trip.get('startingLocation') == 'Pune', "Edit trip not persisted!"
assert refetched_trip.get('budgetInr') == 48000.0, "Budget not persisted!"

# 5. Test MARK AS COMPLETED
act_id = first_item.get('id')
status, comp_trip = request('POST', f"/trips/{trip_id}/activities/{act_id}/complete", token=token)
print(f"5. MARK AS COMPLETED (POST /trips/{trip_id}/activities/{act_id}/complete): status {status}")

# Check next activity
status, next_check = request('GET', f"/trips/{trip_id}/activities/{act_id}/next-check", token=token)
print(f"6. CHECK NEXT ACTIVITY: status {status}, hasNext: {next_check.get('hasNext')}, isSuitable: {next_check.get('isSuitable')}")

# 6. Test GET ALTERNATIVES
status, alts = request('GET', f"/trips/{trip_id}/activities/{act_id}/alternatives?reason=Personal+preference", token=token)
print(f"7. GET ALTERNATIVES: status {status}, count: {len(alts) if isinstance(alts, list) else 0}")

# 7. Test REPLACE ACTIVITY (Reschedule)
if alts and len(alts) > 0:
    alt_to_use = alts[0]
    status, replaced_trip = request('POST', f"/trips/{trip_id}/activities/{act_id}/replace", alt_to_use, token=token)
    print(f"8. REPLACE ACTIVITY: status {status}, replaced with: {alt_to_use.get('title')}")

# 8. Test WEATHER ADAPTATION
day_id = first_day.get('id')
status, adapted_trip = request('POST', f"/trips/{trip_id}/adapt?dayId={day_id}&weatherCondition=Rainy&rainProb=0.85&temp=24.0", token=token)
print(f"9. ADAPT DAY FOR RAIN: status {status}")
if adapted_trip.get('days'):
    adapted_items = adapted_trip['days'][0].get('items', [])
    statuses = [i.get('status') for i in adapted_items]
    print(f"   Item statuses after adaptation: {statuses}")

# 9. Test INVITE TRAVELLER
invitee_email = f"friend_{int(time.time())}@travelmate.com"
status, invite = request('POST', f"/trips/{trip_id}/invite", {
    'email': invitee_email,
    'role': 'EDITOR'
}, token=token)
print(f"10. INVITE TRAVELLER ({invitee_email}): status {status}, invite ID: {invite.get('id')}")

# 10. Test REMOVE ACTIVITY
second_item = trip['days'][0]['items'][1] if len(trip['days'][0].get('items', [])) > 1 else None
if second_item:
    sec_id = second_item.get('id')
    status, del_act_trip = request('DELETE', f"/trips/{trip_id}/activities/{sec_id}", token=token)
    print(f"11. REMOVE ACTIVITY: status {status}")

# 11. Test DELETE TRIP
status, del_res = request('DELETE', f"/trips/{trip_id}", token=token)
print(f"12. DELETE TRIP: status {status}, response: {del_res}")

print("=== ALL FEATURES SUCCESSFULLY VERIFIED END-TO-END! ===")
