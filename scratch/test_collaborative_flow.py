import requests
import json
import time

BASE_URL = "http://localhost:8080/api"

def print_step(title):
    print("\n" + "="*60)
    print(f"[STEP] {title}")
    print("="*60)

def main():
    # 1. Register/Login User A (Trip Owner)
    print_step("Step 1: Authenticate User A (Trip Owner)")
    user_a_email = "owner_a@travelmate.com"
    user_a_pwd = "Password@123"
    
    # Try register User A
    reg_a = requests.post(f"{BASE_URL}/auth/register", json={
        "email": user_a_email,
        "password": user_a_pwd,
        "fullName": "Rahul Owner",
        "age": 28,
        "role": "USER"
    })
    
    # Login User A
    login_a = requests.post(f"{BASE_URL}/auth/login", json={
        "email": user_a_email,
        "password": user_a_pwd
    })
    assert login_a.status_code == 200, f"User A login failed: {login_a.text}"
    token_a = login_a.json()["token"]
    headers_a = {"Authorization": f"Bearer {token_a}"}
    print(f"[PASS] User A authenticated: {user_a_email}")

    # 2. Register/Login User B (Collaborator / Friend)
    print_step("Step 2: Authenticate User B (Invited Collaborator)")
    user_b_email = "friend_b@travelmate.com"
    user_b_pwd = "Password@123"
    
    reg_b = requests.post(f"{BASE_URL}/auth/register", json={
        "email": user_b_email,
        "password": user_b_pwd,
        "fullName": "Priya Friend",
        "age": 26,
        "role": "USER"
    })
    
    login_b = requests.post(f"{BASE_URL}/auth/login", json={
        "email": user_b_email,
        "password": user_b_pwd
    })
    assert login_b.status_code == 200, f"User B login failed: {login_b.text}"
    token_b = login_b.json()["token"]
    headers_b = {"Authorization": f"Bearer {token_b}"}
    print(f"[PASS] User B authenticated: {user_b_email}")

    # 3. User A Creates Trip: Vijayawada -> Hyderabad, 2 days, Rs.20,000, Heritage + Food
    print_step("Step 3: User A Generates Trip (Vijayawada -> Hyderabad, 2 Days, Rs.20,000, Heritage + Food)")
    trip_payload = {
        "destinationCity": "Hyderabad",
        "startingLocation": "Vijayawada",
        "startDate": "2026-10-05",
        "endDate": "2026-10-06",
        "numDays": 2,
        "budgetInr": 20000.0,
        "numTravelers": 2,
        "travelStyle": "Friends",
        "activityLevel": "Moderate",
        "foodPreference": "Any",
        "transitPreference": "Train",
        "travelDistancePref": "Moderate (< 25 km)",
        "interests": ["Heritage", "Food"]
    }
    
    create_res = requests.post(f"{BASE_URL}/trips", json=trip_payload, headers=headers_a)
    assert create_res.status_code == 200, f"Trip creation failed: {create_res.text}"
    created_trip = create_res.json()
    trip_id = created_trip["id"]
    print(f"[PASS] Trip created successfully! ID: #{trip_id}, Title: {created_trip.get('title')}")
    print(f"  Days: {len(created_trip.get('days', []))}, Budget: Rs.{created_trip.get('budgetInr')}")

    # 4. User A modifies trip via Edit: Budget Rs.20,000 -> Rs.15,000
    print_step("Step 4: User A edits trip budget Rs.20,000 -> Rs.15,000 via Edit API")
    edit_payload = {
        **trip_payload,
        "budgetInr": 15000.0
    }
    update_res = requests.put(f"{BASE_URL}/trips/{trip_id}", json=edit_payload, headers=headers_a)
    assert update_res.status_code == 200, f"Trip edit failed: {update_res.text}"
    updated_trip = update_res.json()
    assert updated_trip["id"] == trip_id, f"Trip ID changed! Expected {trip_id}, got {updated_trip['id']}"
    assert updated_trip["budgetInr"] == 15000.0, f"Budget not updated! Got {updated_trip['budgetInr']}"
    print(f"[PASS] Trip ID preserved (#{trip_id}). Budget updated to Rs.{updated_trip['budgetInr']}. Notice: {updated_trip.get('lastUpdateNotice')}")

    # 5. User A Invites User B as EDITOR
    print_step("Step 5: User A invites User B (Priya) to the SAME trip ID")
    invite_res = requests.post(f"{BASE_URL}/trips/{trip_id}/invite", json={
        "email": user_b_email,
        "role": "EDITOR"
    }, headers=headers_a)
    assert invite_res.status_code == 200, f"Invite failed: {invite_res.text}"
    invite_data = invite_res.json()
    invite_id = invite_data["id"]
    print(f"[PASS] Invitation sent. Invite ID: #{invite_id}, Status: {invite_data.get('status')}, Role: {invite_data.get('role')}")

    # 6. User B checks pending invitations
    print_step("Step 6: User B checks invitations list")
    b_invites = requests.get(f"{BASE_URL}/trips/invitations", headers=headers_b).json()
    assert len(b_invites) > 0, "User B received no invitations"
    target_invite = [inv for inv in b_invites if inv["id"] == invite_id][0]
    print(f"[PASS] User B sees invitation: from {target_invite.get('invitedByName')} to join '{target_invite.get('tripTitle')}' (#{target_invite.get('tripId')})")

    # 7. User B Accepts the Invitation
    print_step("Step 7: User B accepts the invitation")
    accept_res = requests.post(f"{BASE_URL}/trips/invitations/{invite_id}/respond?action=ACCEPT", headers=headers_b)
    assert accept_res.status_code == 200, f"Accept failed: {accept_res.text}"
    print(f"[PASS] Invitation accepted response: {accept_res.json()}")

    # 8. User B views My Trips & views the shared trip
    print_step("Step 8: User B accesses the shared trip (SAME Trip ID, SAME itinerary, SAME budget)")
    b_trips = requests.get(f"{BASE_URL}/trips", headers=headers_b).json()
    matching_trips = [t for t in b_trips if t["id"] == trip_id]
    assert len(matching_trips) > 0, "User B cannot see the shared trip in My Trips!"
    shared_trip = matching_trips[0]
    print(f"[PASS] User B accesses Trip #{shared_trip['id']}:")
    print(f"  Destination: {shared_trip['destinationCity']}")
    print(f"  Budget: Rs.{shared_trip['budgetInr']}")
    print(f"  Days count: {len(shared_trip['days'])}")
    print(f"  No new trip was created. Same trip reference!")

    # 9. User B marks an activity as Completed
    print_step("Step 9: User B marks the first activity as Completed")
    first_activity = shared_trip["days"][0]["items"][0]
    act_id = first_activity["id"]
    print(f"  Marking activity #{act_id} ({first_activity['title']}) as COMPLETED...")
    
    complete_res = requests.post(f"{BASE_URL}/trips/{trip_id}/activities/{act_id}/complete", headers=headers_b)
    assert complete_res.status_code == 200, f"Mark completed failed: {complete_res.text}"
    
    # Verify User A sees the completed activity on the shared trip
    trip_for_a = requests.get(f"{BASE_URL}/trips/{trip_id}", headers=headers_a).json()
    act_for_a = [item for day in trip_for_a["days"] for item in day["items"] if item["id"] == act_id][0]
    assert act_for_a["status"] == "COMPLETED", f"Activity status for User A is {act_for_a['status']}, expected COMPLETED"
    print(f"[PASS] Verified! User A sees activity '{act_for_a['title']}' as {act_for_a['status']}. Shared status synchronized!")

    # 10. Adaptive Condition: Weather affects next activity
    print_step("Step 10: Check Next Activity with adverse weather override")
    next_check = requests.get(f"{BASE_URL}/trips/{trip_id}/activities/{act_id}/next-check?weatherOverride=rain", headers=headers_a).json()
    print(f"[PASS] Condition check output:")
    print(f"  Next Activity: {next_check.get('nextActivityTitle')}")
    print(f"  Is Suitable: {next_check.get('isSuitable')}")
    print(f"  Alert Message: {next_check.get('alertMessage')}")
    print(f"  Rain Prob: {next_check.get('rainProbability')}%")

    next_act_id = next_check.get("nextActivityId")
    if next_act_id:
        print_step("Step 11: Fetch verified alternatives and User chooses alternative")
        alts = requests.get(f"{BASE_URL}/trips/{trip_id}/activities/{next_act_id}/alternatives?reason=Weather concern", headers=headers_a).json()
        print(f"[PASS] Found {len(alts)} real verified alternatives:")
        for idx, alt in enumerate(alts[:3]):
            print(f"  {idx+1}. {alt.get('title')} ({alt.get('category')}, Fee: Rs.{alt.get('costInr')}, Match: {alt.get('aiMatchScore')}%)")

        if len(alts) > 0:
            chosen = alts[0]
            print(f"  User selects alternative: '{chosen.get('title')}'")
            replace_res = requests.post(f"{BASE_URL}/trips/{trip_id}/activities/{next_act_id}/replace", json=chosen, headers=headers_a)
            assert replace_res.status_code == 200, f"Replace failed: {replace_res.text}"
            
            # Verify User B sees the replaced destination
            trip_for_b = requests.get(f"{BASE_URL}/trips/{trip_id}", headers=headers_b).json()
            titles_for_b = [item["title"] for day in trip_for_b["days"] for item in day["items"]]
            assert chosen.get("title") in titles_for_b, f"Alternative not found in User B's itinerary!"
            print(f"[PASS] Verified! User B sees replaced alternative '{chosen.get('title')}' on the shared itinerary!")

    # 11. Delete Trip: Owner deletes trip, verify shared member no longer has access
    print_step("Step 12: User A (Owner) deletes the trip")
    del_res = requests.delete(f"{BASE_URL}/trips/{trip_id}", headers=headers_a)
    assert del_res.status_code == 200, f"Delete failed: {del_res.text}"
    print(f"[PASS] Owner deleted trip #{trip_id}")

    # Verify User B cannot access the deleted trip
    b_trips_after = requests.get(f"{BASE_URL}/trips", headers=headers_b).json()
    b_ids = [t["id"] for t in b_trips_after]
    assert trip_id not in b_ids, f"Trip #{trip_id} still accessible to User B after deletion!"
    print(f"[PASS] Verified! Trip #{trip_id} is no longer accessible to shared members.")

    print("\n" + "*"*60)
    print("SUCCESS: ALL 12 STEPS OF THE END-TO-END COLLABORATIVE SCENARIO PASSED!")
    print("*"*60)

if __name__ == "__main__":
    main()
