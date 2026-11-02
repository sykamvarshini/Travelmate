import os
import sys
import pandas as pd
import numpy as np
import joblib
import json
from datetime import datetime
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

# Add parent directory to path so relative imports work
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
sys.path.append(parent_dir)

from preprocessing.feature_pipeline import TravelFeaturePipeline
from models.recommender import ContentBasedRecommender, CollaborativeFilteringRecommender, HybridTravelRecommender

def run_training_pipeline():
    print("=== STARTING ML MODEL TRAINING PIPELINE ===")
    data_dir = os.path.join(parent_dir, "data")
    saved_models_dir = os.path.join(parent_dir, "models", "saved_models")
    eval_dir = os.path.join(parent_dir, "evaluation")
    os.makedirs(saved_models_dir, exist_ok=True)
    os.makedirs(eval_dir, exist_ok=True)
    
    # 1. Load Data
    attractions_path = os.path.join(data_dir, "indian_attractions.csv")
    interactions_path = os.path.join(data_dir, "user_interactions.csv")
    profiles_path = os.path.join(data_dir, "traveler_profiles.csv")
    
    print(f"Loading attractions from: {attractions_path}")
    attractions_df = pd.read_csv(attractions_path)
    print(f"Loaded {len(attractions_df)} attractions.")
    
    print(f"Loading user interactions from: {interactions_path}")
    interactions_df = pd.read_csv(interactions_path)
    print(f"Loaded {len(interactions_df)} interactions.")
    
    # 2. Feature Engineering & Vectorization
    print("Fitting TravelFeaturePipeline on attractions...")
    pipeline = TravelFeaturePipeline()
    item_features = pipeline.fit_transform_attractions(attractions_df)
    print(f"Item feature matrix shape: {item_features.shape}")
    
    # 3. Train Content-Based Model
    content_model = ContentBasedRecommender(pipeline, attractions_df, item_features)
    
    # 4. Train Collaborative Filtering (SVD) Model with Evaluation Split
    print("Splitting interactions into Train (80%) and Test (20%)...")
    train_df, test_df = train_test_split(interactions_df, test_size=0.20, random_state=42)
    
    cf_model = CollaborativeFilteringRecommender(n_components=6)
    cf_model.fit(train_df)
    
    # Evaluate Collaborative Filtering on Test Set
    y_true = []
    y_pred = []
    hits_at_5 = 0
    test_evaluated = 0
    
    for _, row in test_df.iterrows():
        u = int(row['user_id'])
        a = int(row['attraction_id'])
        actual_rating = float(row['rating'])
        predicted_scaled = cf_model.predict_user_rating(u, a)
        
        if predicted_scaled is not None:
            pred_rating = predicted_scaled * 5.0
            y_true.append(actual_rating)
            y_pred.append(pred_rating)
            if abs(actual_rating - pred_rating) <= 1.0:
                hits_at_5 += 1
            test_evaluated += 1
            
    rmse = float(np.sqrt(mean_squared_error(y_true, y_pred))) if y_true else 0.0
    mae = float(np.mean(np.abs(np.array(y_true) - np.array(y_pred)))) if y_true else 0.0
    accuracy_within_1_star = float(hits_at_5 / test_evaluated) if test_evaluated else 0.0
    
    print(f"Test Set Evaluation: RMSE = {rmse:.4f}, MAE = {mae:.4f}, Accuracy within 1.0 star = {accuracy_within_1_star*100:.2f}%")
    
    # Content-based sanity test
    test_recs = content_model.predict_suitability(
        user_interests=["Beaches", "Adventure"],
        activity_level="High",
        travel_style="Friends",
        city="Goa"
    )
    print("\nSanity Check Top 3 Goa Recommendations for (Beaches + Adventure, High Intensity):")
    for r in test_recs[:3]:
        print(f" - [{r['category']}] {r['name']} (Score: {r['predicted_score']:.3f}): {r['recommendation_reason']}")

    # 5. Serialize Model Artifacts
    print("\nSaving trained models to disk...")
    joblib.dump(pipeline, os.path.join(saved_models_dir, "feature_pipeline.joblib"))
    joblib.dump(attractions_df, os.path.join(saved_models_dir, "attractions_df.joblib"))
    joblib.dump(item_features, os.path.join(saved_models_dir, "item_features.joblib"))
    joblib.dump(cf_model, os.path.join(saved_models_dir, "cf_model.joblib"))
    print("All model artifacts saved successfully.")
    
    # 6. Save Evaluation Metrics Report
    eval_metrics = {
        "model_version": "1.0.0",
        "training_timestamp": datetime.now().isoformat(),
        "total_attractions": len(attractions_df),
        "total_user_interactions": len(interactions_df),
        "feature_dimensions": int(item_features.shape[1]),
        "algorithms": {
            "content_based": {
                "algorithm": "Cosine Similarity over TF-IDF + Scaled Multi-Feature Matrix",
                "text_weight": 0.75,
                "numerical_features_weight": 0.25,
                "cold_start_support": True
            },
            "collaborative_filtering": {
                "algorithm": "TruncatedSVD Matrix Factorization",
                "latent_factors": 6,
                "rmse": round(rmse, 4),
                "mae": round(mae, 4),
                "within_1_star_accuracy": round(accuracy_within_1_star, 4),
                "test_samples": test_evaluated
            },
            "hybrid": {
                "combination": "Linear weighted score (65% Content + 35% SVD)",
                "fallback": "Pure Content-Based for unseen users"
            }
        }
    }
    
    metrics_path = os.path.join(eval_dir, "metrics.json")
    with open(metrics_path, "w", encoding="utf-8") as f:
        json.dump(eval_metrics, f, indent=2)
    print(f"Evaluation report written to: {metrics_path}")
    print("=== ML TRAINING PIPELINE FINISHED SUCCESSFULLY ===")

if __name__ == "__main__":
    run_training_pipeline()
