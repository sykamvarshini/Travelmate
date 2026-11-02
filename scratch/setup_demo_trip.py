import requests

BASE = 'http://localhost:8080/api'
login_a = requests.post(f'{BASE}/auth/login', json={'email': 'owner_a@travelmate.com', 'password': 'Password@123'}).json()
token_a = login_a['token']
headers_a = {'Authorization': f'Bearer {token_a}'}

trip = requests.post(f'{BASE}/trips', json={
    'destinationCity': 'Hyderabad',
    'startingLocation': 'Vijayawada',
    'startDate': '2026-10-10',
    'endDate': '2026-10-11',
    'numDays': 2,
    'budgetInr': 15000.0,
    'numTravelers': 2,
    'travelStyle': 'Friends',
    'activityLevel': 'Moderate',
    'foodPreference': 'Any',
    'transitPreference': 'Train',
    'interests': ['Heritage', 'Food']
}, headers=headers_a).json()

trip_id = trip['id']
print('Created trip:', trip_id)

invite = requests.post(f'{BASE}/trips/{trip_id}/invite', json={
    'email': 'friend_b@travelmate.com',
    'role': 'EDITOR'
}, headers=headers_a).json()
invite_id = invite['id']
print('Invited friend_b:', invite_id)

login_b = requests.post(f'{BASE}/auth/login', json={'email': 'friend_b@travelmate.com', 'password': 'Password@123'}).json()
token_b = login_b['token']
headers_b = {'Authorization': f'Bearer {token_b}'}
accept = requests.post(f'{BASE}/trips/invitations/{invite_id}/respond?action=ACCEPT', headers=headers_b).json()
print('Accepted:', accept)

# Complete one activity for progress bar demonstration
act_id = trip['days'][0]['items'][0]['id']
complete = requests.post(f'{BASE}/trips/{trip_id}/activities/{act_id}/complete', headers=headers_b).json()
print('Completed activity #', act_id)
