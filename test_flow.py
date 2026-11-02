import requests

def test_flow():
    # Register
    print("Registering...")
    r = requests.post('http://localhost:8080/api/auth/register', json={
        'fullName': 'Test Python',
        'email': 'python1@test.com',
        'password': 'password123',
        'age': 25,
        'role': 'USER'
    })
    print(r.status_code, r.text)
    token = r.json().get('token')
    if not token:
        print("Failed to get token!")
        return

    print("Creating trip...")
    r2 = requests.post('http://localhost:8080/api/trips', 
        headers={'Authorization': f'Bearer {token}'},
        json={
            'startingLocation': 'Delhi',
            'destinationCity': 'Goa',
            'startDate': '2024-12-01',
            'endDate': '2024-12-05',
            'budgetInr': 30000,
            'travelStyle': 'Solo',
            'interests': ['Beaches'],
            'activityLevel': 'Moderate',
            'foodPreference': 'Any',
            'numTravelers': 1,
            'numDays': 4
        }
    )
    print(r2.status_code, r2.text)

if __name__ == '__main__':
    test_flow()
