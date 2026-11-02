import os
import sys

# Ensure root directory is on PYTHONPATH
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi.testclient import TestClient
from ml.api.main import app

client = TestClient(app)

def test_api():
    print("Testing /health...")
    h = client.get("/health")
    print("Health response:", h.status_code, h.json())
    assert h.status_code == 200

    print("\nTesting /recommend...")
    rec_res = client.post("/recommend", json={
        "interests": ["Beaches", "Adventure", "Food"],
        "activity_level": "High",
        "travel_style": "Friends",
        "budget": "Mid-Range",
        "city": "Goa",
        "top_k": 5
    })
    print("Recommend response status:", rec_res.status_code)
    data = rec_res.json()
    recs = data["recommendations"]
    for r in recs:
        print(f" -> {r['name']} ({r['category']}, {r['indoor_outdoor']}) Score: {r['predicted_score']} | Reason: {r['recommendation_reason']}")

    print("\nTesting /context-rerank with Heavy Rain (85% rain probability)...")
    rerank_res = client.post("/context-rerank", json={
        "candidates": recs,
        "weather_condition": "Heavy Rain",
        "rain_probability": 0.85,
        "current_hour": 14,
        "travel_party": "Friends"
    })
    print("Context re-rank status:", rerank_res.status_code)
    reranked = rerank_res.json()["reranked_items"]
    print("Re-ranked order under heavy rain:")
    for r in reranked:
        print(f" -> {r['name']} ({r['indoor_outdoor']}) Final: {r['final_score']} | Breakdown: {r['context_score_breakdown']} | Alert: {r['weather_alert']}")

if __name__ == "__main__":
    test_api()
