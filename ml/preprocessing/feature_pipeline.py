import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import MinMaxScaler
import re

class TravelFeaturePipeline:
    def __init__(self):
        self.tfidf = TfidfVectorizer(stop_words='english', token_pattern=r'(?u)\b[\w-]+\b')
        self.scaler = MinMaxScaler()
        self.intensity_map = {"Low": 0.2, "Moderate": 0.6, "High": 1.0}
        self.fitted = False

    def clean_text(self, text: str) -> str:
        if not isinstance(text, str):
            return ""
        text = text.lower().strip()
        text = re.sub(r'[^a-z0-9\s-]', ' ', text)
        return " ".join(text.split())

    def build_item_text(self, row) -> str:
        category = self.clean_text(str(row.get('category', '')))
        subcategory = self.clean_text(str(row.get('subcategory', '')))
        tags = self.clean_text(str(row.get('tags', '')))
        desc = self.clean_text(str(row.get('description', '')))
        city = self.clean_text(str(row.get('city', '')))
        indoor = "indoor" if str(row.get('indoor_outdoor', '')).lower() == "indoor" else "outdoor"
        return f"{category} {category} {subcategory} {tags} {indoor} {city} {desc}"

    def fit_transform_attractions(self, df: pd.DataFrame):
        corpus = [self.build_item_text(row) for _, row in df.iterrows()]
        tfidf_matrix = self.tfidf.fit_transform(corpus).toarray()
        
        # Numerical features: normalized rating, entry_fee, duration, activity_intensity
        ratings = df['rating'].fillna(4.0).values.reshape(-1, 1)
        durations = df['duration_hours'].fillna(2.0).values.reshape(-1, 1)
        intensities = np.array([self.intensity_map.get(str(x), 0.5) for x in df['activity_intensity']]).reshape(-1, 1)
        
        num_features = np.hstack([ratings, durations, intensities])
        scaled_num = self.scaler.fit_transform(num_features)
        
        # Combine TF-IDF text features with numerical weights
        combined_matrix = np.hstack([tfidf_matrix * 0.75, scaled_num * 0.25])
        self.fitted = True
        return combined_matrix

    def transform_attractions(self, df: pd.DataFrame):
        if not self.fitted:
            raise ValueError("Pipeline must be fitted first.")
        corpus = [self.build_item_text(row) for _, row in df.iterrows()]
        tfidf_matrix = self.tfidf.transform(corpus).toarray()
        
        ratings = df['rating'].fillna(4.0).values.reshape(-1, 1)
        durations = df['duration_hours'].fillna(2.0).values.reshape(-1, 1)
        intensities = np.array([self.intensity_map.get(str(x), 0.5) for x in df['activity_intensity']]).reshape(-1, 1)
        
        num_features = np.hstack([ratings, durations, intensities])
        scaled_num = self.scaler.transform(num_features)
        return np.hstack([tfidf_matrix * 0.75, scaled_num * 0.25])

    def build_user_vector(self, interests: list, activity_level: str, travel_style: str = "Solo"):
        if not self.fitted:
            raise ValueError("Pipeline must be fitted first.")
        
        interest_text = " ".join([self.clean_text(i) for i in interests])
        style_text = self.clean_text(travel_style)
        user_text = f"{interest_text} {interest_text} {style_text}"
        
        user_tfidf = self.tfidf.transform([user_text]).toarray()
        
        intensity_val = self.intensity_map.get(activity_level, 0.5)
        # Default target rating high (5.0), moderate duration (2.5h), target intensity
        num_vec = np.array([[5.0, 2.5, intensity_val]])
        scaled_num = self.scaler.transform(num_vec)
        
        return np.hstack([user_tfidf * 0.75, scaled_num * 0.25])
