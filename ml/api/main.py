import os
import sys
import joblib
import pandas as pd
import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import json

# Setup paths
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
sys.path.append(parent_dir)

import requests
from models.recommender import ContentBasedRecommender, CollaborativeFilteringRecommender, HybridTravelRecommender

app = FastAPI(
    title="TravelMate AI - Recommendation & Context Scoring Microservice",
    description="Context-Aware ML Recommendation and Re-Ranking Engine for Indian Tourism",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load Models on Startup
saved_models_dir = os.path.join(parent_dir, "models", "saved_models")
eval_dir = os.path.join(parent_dir, "evaluation")

try:
    print(f"Loading ML models from {saved_models_dir}...")
    feature_pipeline = joblib.load(os.path.join(saved_models_dir, "feature_pipeline.joblib"))
    attractions_df = joblib.load(os.path.join(saved_models_dir, "attractions_df.joblib"))
    item_features = joblib.load(os.path.join(saved_models_dir, "item_features.joblib"))
    cf_model = joblib.load(os.path.join(saved_models_dir, "cf_model.joblib"))
    
    content_recommender = ContentBasedRecommender(feature_pipeline, attractions_df, item_features)
    hybrid_recommender = HybridTravelRecommender(content_recommender, cf_model)
    print("Models loaded successfully!")
except Exception as e:
    print(f"CRITICAL ERROR loading models: {e}")
    feature_pipeline = None
    attractions_df = None
    item_features = None
    cf_model = None
    hybrid_recommender = None

# Schemas
class RecommendRequest(BaseModel):
    user_id: Optional[int] = None
    interests: List[str] = Field(default_factory=lambda: ["Beaches", "Adventure"])
    activity_level: str = "Moderate"
    travel_style: str = "Solo"
    budget: str = "Mid-Range"
    city: Optional[str] = "Goa"
    locality: Optional[str] = None
    duration_hours: Optional[int] = None
    max_distance_km: Optional[float] = None
    max_entry_fee: Optional[float] = None
    travel_distance_pref: Optional[str] = None
    top_k: int = 10

class ContextWeights(BaseModel):
    w_pref: float = 0.40
    w_weather: float = 0.25
    w_time: float = 0.15
    w_budget: float = 0.10
    w_party: float = 0.10

class ContextRerankRequest(BaseModel):
    candidates: List[Dict[str, Any]]
    weather_condition: str = "Clear Sky"
    rain_probability: float = 0.0 # 0.0 to 1.0
    temperature_c: float = 28.0
    current_hour: int = 10 # 0-23
    travel_party: str = "Solo" # Solo, Couple, Family, Friends
    has_seniors: bool = False
    has_children: bool = False
    budget_remaining: float = 20000.0
    weights: Optional[ContextWeights] = None

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "TravelMate AI - ML Microservice",
        "models_loaded": hybrid_recommender is not None,
        "database_entries": len(attractions_df) if attractions_df is not None else 0
    }

@app.get("/model-info")
def model_info():
    metrics_path = os.path.join(eval_dir, "metrics.json")
    if os.path.exists(metrics_path):
        with open(metrics_path, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"status": "Metrics file not found"}

@app.post("/recommend")
def get_recommendations(req: RecommendRequest):
    if hybrid_recommender is None:
        raise HTTPException(status_code=500, detail="Recommendation models are not initialized.")
    
    
    # 1. Enforce India-Only Scope
    foreign_cities = {
        "paris", "london", "dubai", "new york", "singapore", "bangkok", "tokyo", "rome", 
        "sydney", "toronto", "barcelona", "amsterdam", "los angeles", "berlin", "istanbul", 
        "kuala lumpur", "doha", "abu dhabi", "bali", "hong kong", "san francisco", "chicago", 
        "maldives", "switzerland", "zurich", "las vegas", "miami", "venice", "florence", 
        "madrid", "seoul", "beijing", "shanghai", "cairo", "cape town", "colombo", "kathmandu"
    }
    if req.city:
        clean_c = req.city.strip().lower()
        for fc in foreign_cities:
            if clean_c == fc or clean_c.startswith(fc + " ") or clean_c.startswith(fc + ","):
                raise HTTPException(status_code=400, detail="TravelMate AI currently supports travel within India.")

    recommendations = hybrid_recommender.recommend(
        user_interests=req.interests,
        activity_level=req.activity_level,
        travel_style=req.travel_style,
        city=req.city,
        user_id=req.user_id,
        top_k=req.top_k
    )
    
    # If recommendations are fewer than top_k, discover real verified places dynamically around the destination
    if req.city and len(recommendations) < req.top_k:
        print(f"Dynamic tourist attraction lookup for: {req.city} (current count: {len(recommendations)}, needed: {req.top_k})")
        headers = {'User-Agent': 'TravelMateAI/1.0'}
        city_query = req.city.strip()
        
        lat = None
        lon = None
        if recommendations and "latitude" in recommendations[0] and recommendations[0]["latitude"]:
            lat = float(recommendations[0]["latitude"])
            lon = float(recommendations[0]["longitude"])
        
        # Geocode Destination if lat/lon not yet available
        if lat is None or lon is None:
            try:
                nom_url = f"https://nominatim.openstreetmap.org/search?q={requests.utils.quote(city_query)}+India&format=json&limit=3"
                nom_res = requests.get(nom_url, headers=headers, timeout=6).json()
                if nom_res:
                    primary = nom_res[0]
                    p_lat = float(primary['lat'])
                    p_lon = float(primary['lon'])
                    # Validate India bounds: Lat 6.0 to 38.0, Lon 68.0 to 98.0
                    if p_lat < 6.0 or p_lat > 38.0 or p_lon < 68.0 or p_lon > 98.0:
                        raise HTTPException(status_code=400, detail="TravelMate AI currently supports travel within India.")
                    lat = p_lat
                    lon = p_lon
                    p_name = primary.get('name') or city_query
                    p_type = primary.get('type', '')

                    # If the search query itself was a specific attraction/monument/temple, add it if not already present
                    landmark_types = ['attraction', 'museum', 'place_of_worship', 'fort', 'castle', 'viewpoint', 'theme_park', 'monument', 'ruins', 'beach']
                    if p_type in landmark_types and not any(r['name'].lower() == p_name.lower() for r in recommendations):
                        cat = "Spiritual / Heritage" if p_type == 'place_of_worship' else ("Museum & Art" if p_type == 'museum' else ("Beaches & Coastline" if p_type == 'beach' else "Historical Monument"))
                        is_indoor = "Indoor" if p_type in ['museum', 'place_of_worship'] else "Outdoor"
                        fee = 50.0 if p_type == 'museum' else 0.0
                        recommendations.insert(0, {
                            "id": 9000 + len(recommendations),
                            "attraction_id": 9000 + len(recommendations),
                            "name": p_name,
                            "title": p_name,
                            "city": city_query,
                            "state": "India",
                            "category": cat,
                            "rating": None,
                            "reviews_count": None,
                            "entry_fee_inr": fee,
                            "cost": fee,
                            "duration_hours": 2.0,
                            "duration_minutes": 120,
                            "indoor_outdoor": is_indoor,
                            "latitude": lat,
                            "longitude": lon,
                            "predicted_score": 0.98,
                            "recommendation_reason": f"Renowned landmark and primary point of interest in {city_query}.",
                            "description": f"Verified tourist destination in {city_query}."
                        })
            except HTTPException:
                raise
            except Exception as e:
                print(f"Geocoding error: {e}")

        def is_nearby(clat, clon, max_km=40):
            if lat is None or lon is None:
                return True
            return ((clat - lat)**2 + (clon - lon)**2)**0.5 * 111 <= max_km

        effective_max_dist = req.max_distance_km if req.max_distance_km is not None else 40.0
        if req.travel_distance_pref == "Minimal":
            effective_max_dist = min(effective_max_dist, 2.5)
        elif req.travel_distance_pref == "Nearby":
            effective_max_dist = min(effective_max_dist, 5.0)
        elif req.travel_distance_pref == "Moderate":
            effective_max_dist = min(effective_max_dist, 10.0)

        if lat is not None and lon is not None:
            # 2. Wikipedia GeoSearch for verified notable landmarks within 10km (Wikipedia limit)
            try:
                wiki_url = f"https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord={lat}|{lon}&gsradius=10000&gslimit=25&format=json"
                wiki_res = requests.get(wiki_url, headers=headers, timeout=6).json()
                wiki_items = wiki_res.get('query', {}).get('geosearch', [])
                
                noise = [
                    'school', 'vidyalaya', 'college', 'university', 'academy', 'campus', 'polytechnic', 'institute', 'shiksha',
                    'hospital', 'clinic', 'dispensary', 'nursing home', 'medical', 'pharmacy',
                    'court', 'tees hazari', 'tis hazari', 'patiala house', 'karkardooma', 'tribunal', 'police', 'thana', 'jail',
                    'lok niwas', 'raj niwas', 'bhavan', 'secretariat', 'collectorate', 'office', 'headquarters',
                    'colony', 'enclave', 'apartments', 'residency', 'society', 'nagar', 'vihar', 'sector', 'block', 'pocket',
                    'karol bagh', 'lajpat nagar', 'sarojini nagar', 'pitampura', 'rohini', 'dwarka', 'janakpuri',
                    'mandal', 'district', 'division', 'constituency', 'railway station', 'station', 'metro station', 'airport',
                    'flyover', 'bridge', 'corporation', 'capital region', 'shed', 'fire', 'junction', 'bus station', 'bus stop', 'bypas'
                ]
                
                for item in wiki_items:
                    t = item['title']
                    t_low = t.lower()
                    if any(w in t_low for w in noise):
                        continue
                    if any(r['name'].lower() == t.lower() or r.get('title', '').lower() == t.lower() for r in recommendations):
                        continue
                    
                    item_lat = float(item['lat'])
                    item_lon = float(item['lon'])
                    if not is_nearby(item_lat, item_lon, effective_max_dist):
                        continue

                    # Must match authentic tourist categories
                    cat = None
                    indoor = "Outdoor"
                    fee = 0.0
                    if any(k in t_low for k in ['temple', 'mandir', 'church', 'mosque', 'shrine', 'masjid', 'gudi', 'dargah', 'basilica', 'monastery', 'ashram']):
                        cat = "Spiritual / Heritage"
                        indoor = "Indoor"
                        fee = 0.0
                    elif any(k in t_low for k in ['museum', 'gallery', 'memorial', 'planetarium']):
                        cat = "Museum & Art"
                        indoor = "Indoor"
                        fee = 50.0
                    elif any(k in t_low for k in ['fort', 'palace', 'caves', 'ruins', 'monument', 'stupa', 'mahal', 'qila', 'baoli', 'stepwell', 'minar']):
                        cat = "Historical Monument"
                        indoor = "Outdoor"
                        fee = 25.0
                    elif any(k in t_low for k in ['park', 'garden', 'lake', 'falls', 'waterfall', 'beach', 'valley', 'hills', 'zoo', 'sanctuary', 'viewpoint', 'safari']):
                        cat = "Nature & Viewpoint"
                        indoor = "Outdoor"
                        fee = 30.0

                    if cat is None:
                        continue # Reject non-tourist entities

                    if req.max_entry_fee is not None and fee > req.max_entry_fee:
                        continue
                    
                    recommendations.append({
                        "id": 9000 + len(recommendations),
                        "attraction_id": 9000 + len(recommendations),
                        "name": t,
                        "title": t,
                        "city": city_query,
                        "state": "India",
                        "category": cat,
                        "rating": None,
                        "reviews_count": None,
                        "entry_fee_inr": fee,
                        "cost": fee,
                        "duration_hours": 2.0,
                        "duration_minutes": 120,
                        "indoor_outdoor": indoor,
                        "latitude": item_lat,
                        "longitude": item_lon,
                        "predicted_score": 0.92,
                        "recommendation_reason": f"Iconic cultural landmark and verified point of interest in {city_query}.",
                        "description": f"Verified cultural site located in {city_query}."
                    })
                    if len(recommendations) >= req.top_k:
                        break
            except Exception as e:
                print(f"Wiki lookup error: {e}")

            # 3. Supplemental Overpass OSM Query if still under top_k
            if len(recommendations) < req.top_k:
                try:
                    overpass_q = f'[out:json][timeout:4];(node["tourism"~"attraction|museum|viewpoint|zoo|theme_park"](around:25000,{lat},{lon});node["historic"~"fort|monument|castle|ruins|archaeological_site"](around:25000,{lat},{lon});node["amenity"="place_of_worship"](around:25000,{lat},{lon}););out 15;'
                    ov_url = f"https://overpass-api.de/api/interpreter?data={requests.utils.quote(overpass_q)}"
                    ov_res = requests.get(ov_url, headers=headers, timeout=5).json()
                    elements = ov_res.get('elements', [])
                    for el in elements:
                        tags = el.get('tags', {})
                        s_name = tags.get('name')
                        if s_name and not any(r['name'].lower() == s_name.lower() or r.get('title', '').lower() == s_name.lower() for r in recommendations):
                            s_lat = float(el['lat'])
                            s_lon = float(el['lon'])
                            if not is_nearby(s_lat, s_lon, effective_max_dist):
                                continue
                            cat = "Spiritual / Heritage" if tags.get('amenity') == 'place_of_worship' else ("Historical Monument" if 'historic' in tags else "Sightseeing")
                            is_ind = "Indoor" if tags.get('amenity') == 'place_of_worship' or tags.get('tourism') == 'museum' else "Outdoor"
                            fee = 30.0 if tags.get('tourism') == 'museum' else 0.0
                            if req.max_entry_fee is not None and fee > req.max_entry_fee:
                                continue
                            recommendations.append({
                                "id": 9000 + len(recommendations),
                                "attraction_id": 9000 + len(recommendations),
                                "name": s_name,
                                "title": s_name,
                                "city": city_query,
                                "state": "India",
                                "category": cat,
                                "rating": None,
                                "reviews_count": None,
                                "entry_fee_inr": fee,
                                "cost": fee,
                                "duration_hours": 2.0,
                                "duration_minutes": 120,
                                "indoor_outdoor": is_ind,
                                "latitude": s_lat,
                                "longitude": s_lon,
                                "predicted_score": 0.88,
                                "recommendation_reason": f"Popular verified destination in {city_query}.",
                                "description": f"Verified location from OpenStreetMap."
                            })
                            if len(recommendations) >= req.top_k:
                                break
                except Exception as e:
                    print(f"Overpass query error: {e}")

            # 4. Supplemental Nominatim POI search if still under top_k
            if len(recommendations) < req.top_k:
                for sub_term in [f"attractions in {city_query}", f"temple in {city_query}", f"fort in {city_query}", f"lake in {city_query}"]:
                    if len(recommendations) >= req.top_k:
                        break
                    try:
                        nom_poi_url = f"https://nominatim.openstreetmap.org/search?q={requests.utils.quote(sub_term)}&countrycodes=in&format=json&limit=4"
                        poi_res = requests.get(nom_poi_url, headers=headers, timeout=4).json()
                        for p in poi_res:
                            p_name = p.get('name')
                            if not p_name or any(r['name'].lower() == p_name.lower() or r.get('title', '').lower() == p_name.lower() for r in recommendations):
                                continue
                            p_lat = float(p['lat'])
                            p_lon = float(p['lon'])
                            if not is_nearby(p_lat, p_lon, effective_max_dist):
                                continue
                            cat = "Spiritual / Heritage" if "temple" in sub_term else ("Historical Monument" if "fort" in sub_term else "Sightseeing")
                            is_ind = "Indoor" if "temple" in sub_term else "Outdoor"
                            recommendations.append({
                                "id": 9000 + len(recommendations),
                                "attraction_id": 9000 + len(recommendations),
                                "name": p_name,
                                "title": p_name,
                                "city": city_query,
                                "state": "India",
                                "category": cat,
                                "rating": None,
                                "reviews_count": None,
                                "entry_fee_inr": 0.0,
                                "cost": 0.0,
                                "duration_hours": 2.0,
                                "duration_minutes": 120,
                                "indoor_outdoor": is_ind,
                                "latitude": p_lat,
                                "longitude": p_lon,
                                "predicted_score": 0.86,
                                "recommendation_reason": f"Verified point of interest near {city_query}.",
                                "description": f"Verified location from OpenStreetMap."
                            })
                            if len(recommendations) >= req.top_k:
                                break
                    except Exception:
                        pass

    # Apply distance and budget filtering on initial recommendations if specified
    if req.max_entry_fee is not None:
        recommendations = [r for r in recommendations if r.get("entry_fee_inr", 0.0) is None or r.get("entry_fee_inr", 0.0) <= req.max_entry_fee]

    if not recommendations:
        raise HTTPException(status_code=404, detail=f"Location '{req.city}' not found in verified dataset or mapping services.")

    return {
        "city": req.city,
        "user_profile": {
            "interests": req.interests,
            "activity_level": req.activity_level,
            "travel_style": req.travel_style,
            "budget": req.budget
        },
        "count": len(recommendations),
        "recommendations": recommendations
    }

@app.post("/context-rerank")
def context_rerank(req: ContextRerankRequest):
    weights = req.weights or ContextWeights()
    w_pref = weights.w_pref
    w_weather = weights.w_weather
    w_time = weights.w_time
    w_budget = weights.w_budget
    w_party = weights.w_party
    
    reranked = []
    
    for item in req.candidates:
        base_pref = float(item.get("predicted_score", item.get("content_score", 0.5)))
        indoor_outdoor = str(item.get("indoor_outdoor", "Outdoor")).capitalize()
        entry_fee = float(item.get("entry_fee_inr", 0))
        activity_intensity = str(item.get("activity_intensity", "Moderate"))
        
        # 1. Weather Suitability Score (0.0 to 1.0)
        # If high rain prob (> 0.4), outdoor gets heavily penalized, indoor gets boosted
        weather_score = 1.0
        weather_alert = None
        if req.rain_probability > 0.40:
            if indoor_outdoor == "Outdoor":
                # Drops drastically with high rain
                weather_score = max(0.05, 1.0 - (req.rain_probability * 1.2))
                weather_alert = f"Outdoor activity strongly affected by expected rain ({int(req.rain_probability*100)}% chance)."
            else:
                # Indoor activities are protected / boosted in rain
                weather_score = 1.0 + (req.rain_probability * 0.3)
                weather_alert = "Indoor destination, ideal for rainy weather."
        else:
            if indoor_outdoor == "Outdoor":
                weather_score = 1.0
            else:
                weather_score = 0.90 # slightly favor nice weather outdoors
                
        # 2. Time Fit Score (0.0 to 1.0)
        # Check opening hours
        time_score = 1.0
        opening_str = str(item.get("opening_time", "09:00"))
        closing_str = str(item.get("closing_time", "18:00"))
        try:
            op_h = int(opening_str.split(":")[0])
            cl_h = int(closing_str.split(":")[0])
            if req.current_hour < op_h or req.current_hour >= cl_h:
                time_score = 0.20 # Closed or about to close
            elif req.current_hour >= cl_h - 1:
                time_score = 0.50 # Less than 1 hour left
            else:
                time_score = 1.0
        except Exception:
            time_score = 1.0

        # 3. Budget Fit Score
        budget_score = 1.0
        if req.budget_remaining < 500 and entry_fee > 200:
            budget_score = 0.20
        elif req.budget_remaining < entry_fee:
            budget_score = 0.05
        else:
            budget_score = 1.0 - min(0.5, entry_fee / max(1.0, req.budget_remaining))

        # 4. Demographic & Travel Party Fit
        party_score = 1.0
        if req.has_seniors or req.has_children:
            if activity_intensity == "High":
                party_score = 0.30 # Not suitable for seniors/small children
            elif activity_intensity == "Low":
                party_score = 1.10
        elif req.travel_party == "Friends" and activity_intensity == "High":
            party_score = 1.15

        # Final Weighted Score Calculation
        final_score = (
            w_pref * base_pref +
            w_weather * weather_score +
            w_time * time_score +
            w_budget * budget_score +
            w_party * party_score
        )
        
        item_copy = dict(item)
        item_copy["context_score_breakdown"] = {
            "preference_score": round(base_pref, 3),
            "weather_score": round(weather_score, 3),
            "time_score": round(time_score, 3),
            "budget_score": round(budget_score, 3),
            "party_score": round(party_score, 3),
            "final_context_score": round(final_score, 4)
        }
        item_copy["final_score"] = round(final_score, 4)
        item_copy["weather_alert"] = weather_alert
        reranked.append(item_copy)

    # Sort by final context score descending
    reranked.sort(key=lambda x: x["final_score"], reverse=True)
    
    return {
        "weather_context": {
            "condition": req.weather_condition,
            "rain_probability": req.rain_probability,
            "temperature_c": req.temperature_c
        },
        "total_items": len(reranked),
        "reranked_items": reranked
    }
