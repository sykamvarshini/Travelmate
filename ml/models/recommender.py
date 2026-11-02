import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.decomposition import TruncatedSVD

class ContentBasedRecommender:
    def __init__(self, feature_pipeline, attractions_df: pd.DataFrame, item_features: np.ndarray):
        self.pipeline = feature_pipeline
        self.df = attractions_df.copy()
        self.item_features = item_features

    def predict_suitability(self, user_interests: list, activity_level: str, travel_style: str = "Solo", city: str = None):
        user_vector = self.pipeline.build_user_vector(user_interests, activity_level, travel_style)
        similarities = cosine_similarity(user_vector, self.item_features)[0]
        
        results = []
        for idx, sim in enumerate(similarities):
            row = self.df.iloc[idx].to_dict()
            if city:
                city_clean = city.strip().lower()
                row_city_clean = str(row['city']).strip().lower()
                row_name_clean = str(row['name']).strip().lower()
                
                aliases = {
                    "vizag": "visakhapatnam",
                    "araku": "araku valley",
                    "tirumala": "tirupati",
                    "bangalore": "bengaluru",
                    "banaras": "varanasi",
                    "kashi": "varanasi",
                    "mysore": "mysuru",
                    "cochin": "kochi",
                    "calcutta": "kolkata",
                    "bombay": "mumbai",
                    "madras": "chennai"
                }
                mapped_city = aliases.get(city_clean, city_clean)
                
                matches_city = (
                    row_city_clean == mapped_city or 
                    mapped_city in row_city_clean or 
                    row_city_clean in mapped_city
                )
                matches_name = (
                    mapped_city in row_name_clean or 
                    row_name_clean in mapped_city
                )
                
                if not (matches_city or matches_name):
                    continue
            
            # Additional heuristic adjustments for demographics and style
            score = float(sim)
            # Normalize to 0.0 - 1.0 range
            score = max(0.0, min(1.0, (score + 1.0) / 2.0 if score < 0 else score))
            
            # Generate personalized explanation
            matched_interests = [i for i in user_interests if i.lower() in str(row.get('category', '')).lower() or i.lower() in str(row.get('tags', '')).lower()]
            reasons = []
            if matched_interests:
                reasons.append(f"Directly matches your interest in {', '.join(matched_interests)}")
            if row.get('activity_intensity') == activity_level:
                reasons.append(f"Fits your {activity_level.lower()} pace")
            if not reasons:
                reasons.append(f"Top-rated {row.get('category')} landmark in {row.get('city')}")

            dur_hrs = float(row['duration_hours'])
            fee = float(row['entry_fee_inr'])
            name_val = str(row['name'])

            results.append({
                "attraction_id": int(row['id']),
                "name": name_val,
                "title": name_val,
                "city": str(row['city']),
                "state": str(row['state']),
                "category": str(row['category']),
                "rating": float(row['rating']),
                "entry_fee_inr": fee,
                "cost": fee,
                "duration_hours": dur_hrs,
                "duration_minutes": int(dur_hrs * 60),
                "opening_time": str(row['opening_time']),
                "closing_time": str(row['closing_time']),
                "indoor_outdoor": str(row['indoor_outdoor']),
                "activity_intensity": str(row['activity_intensity']),
                "age_suitability": str(row['age_suitability']),
                "latitude": float(row['latitude']),
                "longitude": float(row['longitude']),
                "description": str(row['description']),
                "tags": str(row['tags']),
                "content_score": round(score, 4),
                "predicted_score": round(score, 4),
                "recommendation_reason": " • ".join(reasons)
            })
            
        results.sort(key=lambda x: x["predicted_score"], reverse=True)
        return results

class CollaborativeFilteringRecommender:
    def __init__(self, n_components: int = 8):
        self.n_components = n_components
        self.svd = TruncatedSVD(n_components=n_components, random_state=42)
        self.user_item_matrix = None
        self.user_ids = []
        self.attraction_ids = []
        self.fitted = False

    def fit(self, interactions_df: pd.DataFrame):
        # Create user-item pivot table
        pivot = interactions_df.pivot_table(index='user_id', columns='attraction_id', values='rating', fill_value=0.0)
        self.user_ids = list(pivot.index)
        self.attraction_ids = list(pivot.columns)
        matrix = pivot.values
        
        # Fit SVD if enough samples
        if matrix.shape[0] >= self.n_components and matrix.shape[1] >= self.n_components:
            self.user_factors = self.svd.fit_transform(matrix)
            self.item_factors = self.svd.components_
            self.fitted = True
            print(f"Collaborative filtering model fitted with {len(self.user_ids)} users and {len(self.attraction_ids)} items.")
        else:
            print("Insufficient data for SVD collaborative filtering. Pure content-based will be used.")
            self.fitted = False

    def predict_user_rating(self, user_id: int, attraction_id: int) -> float:
        if not self.fitted or user_id not in self.user_ids or attraction_id not in self.attraction_ids:
            return None # Cold-start fallback
        
        u_idx = self.user_ids.index(user_id)
        i_idx = self.attraction_ids.index(attraction_id)
        pred = np.dot(self.user_factors[u_idx], self.item_factors[:, i_idx])
        # Scale to 0.0 - 1.0 (assuming 5-star rating system)
        return float(np.clip(pred / 5.0, 0.0, 1.0))

class HybridTravelRecommender:
    def __init__(self, content_model: ContentBasedRecommender, cf_model: CollaborativeFilteringRecommender):
        self.content_model = content_model
        self.cf_model = cf_model

    def recommend(self, user_interests: list, activity_level: str, travel_style: str = "Solo", 
                  city: str = None, user_id: int = None, top_k: int = 10):
        content_recs = self.content_model.predict_suitability(
            user_interests=user_interests,
            activity_level=activity_level,
            travel_style=travel_style,
            city=city
        )
        
        for item in content_recs:
            cf_score = None
            if user_id is not None:
                cf_score = self.cf_model.predict_user_rating(user_id, item['attraction_id'])
            
            if cf_score is not None:
                # 65% content match, 35% collaborative peer preference
                combined = 0.65 * item['content_score'] + 0.35 * cf_score
                item['cf_score'] = round(cf_score, 4)
                item['predicted_score'] = round(combined, 4)
                item['recommendation_reason'] += " (Recommended by travelers with similar tastes)"
            else:
                item['cf_score'] = None
                item['predicted_score'] = item['content_score']
                
        content_recs.sort(key=lambda x: x["predicted_score"], reverse=True)
        return content_recs[:top_k]
