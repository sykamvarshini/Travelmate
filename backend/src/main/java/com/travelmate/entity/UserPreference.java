package com.travelmate.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "user_preferences")
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String travelStyle = "Solo"; // Solo, Couple, Family, Friends
    private String foodPreference = "Any"; // Vegetarian, Non-Vegetarian, Vegan, Any
    private String activityLevel = "Moderate"; // Relaxed, Moderate, High Intensity
    private String budgetLevel = "Mid-Range"; // Budget, Mid-Range, Luxury
    private String transitPreference = "Cab/Car"; // Public, Cab/Car, Self-Drive, Walking

    @Column(length = 500)
    private String interests = "Beaches,Adventure,Food"; // Comma-separated

    private Boolean hasSeniors = false;
    private Boolean hasChildren = false;
    private String mobilityRequirements;

    public UserPreference() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTravelStyle() { return travelStyle; }
    public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }

    public String getFoodPreference() { return foodPreference; }
    public void setFoodPreference(String foodPreference) { this.foodPreference = foodPreference; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getBudgetLevel() { return budgetLevel; }
    public void setBudgetLevel(String budgetLevel) { this.budgetLevel = budgetLevel; }

    public String getTransitPreference() { return transitPreference; }
    public void setTransitPreference(String transitPreference) { this.transitPreference = transitPreference; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public Boolean getHasSeniors() { return hasSeniors; }
    public void setHasSeniors(Boolean hasSeniors) { this.hasSeniors = hasSeniors; }

    public Boolean getHasChildren() { return hasChildren; }
    public void setHasChildren(Boolean hasChildren) { this.hasChildren = hasChildren; }

    public String getMobilityRequirements() { return mobilityRequirements; }
    public void setMobilityRequirements(String mobilityRequirements) { this.mobilityRequirements = mobilityRequirements; }
}
