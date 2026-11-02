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

    print("Fetching trips...")
    r_trips = requests.get('http://localhost:8080/api/trips', headers={'Authorization': f'Bearer {token}'})
    trips = r_trips.json()
    print("Current trips:", len(trips))

    for trip in trips:
        print(f"Deleting trip {trip['id']}...")
        r_del = requests.delete(f'http://localhost:8080/api/trips/{trip["id"]}', headers={'Authorization': f'Bearer {token}'})
        print(r_del.status_code, r_del.text)

    r_trips2 = requests.get('http://localhost:8080/api/trips', headers={'Authorization': f'Bearer {token}'})
    print("Trips after deletion:", len(r_trips2.json()))

if __name__ == '__main__':
    test_flow()
