package com.travelmate.dto;

import java.time.LocalDate;
import java.util.List;

public class TripCreateRequest {
    private String destinationCity;
    private String startingLocation;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numDays = 3;
    private Integer numTravelers = 1;
    private Double budgetInr = 30000.0;
    private String travelStyle = "Solo"; // Solo, Couple, Family, Friends
    private String ageGroup;
    private List<String> interests;
    private String foodPreference = "Any";
    private String activityLevel = "Moderate"; // Relaxed, Moderate, High Intensity
    private String transitPreference = "Cab/Car";
    private String specialRequirements;
    private String arrivalTime = "10:00";
    private String hotelCheckInTime = "13:00";
    private Integer durationHours;
    private String startTime;
    private String availableUntil;
    private Double maxDistanceKm;
    private Double maxEntryFee;
    private String travelDistancePref;
    private String locality;

    public TripCreateRequest() {}

    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getNumDays() { return numDays; }
    public void setNumDays(Integer numDays) { this.numDays = numDays; }

    public Integer getNumTravelers() { return numTravelers; }
    public void setNumTravelers(Integer numTravelers) { this.numTravelers = numTravelers; }

    public Double getBudgetInr() { return budgetInr; }
    public void setBudgetInr(Double budgetInr) { this.budgetInr = budgetInr; }

    public String getTravelStyle() { return travelStyle; }
    public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public String getFoodPreference() { return foodPreference; }
    public void setFoodPreference(String foodPreference) { this.foodPreference = foodPreference; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getTransitPreference() { return transitPreference; }
    public void setTransitPreference(String transitPreference) { this.transitPreference = transitPreference; }

    public String getStartingLocation() { return startingLocation; }
    public void setStartingLocation(String startingLocation) { this.startingLocation = startingLocation; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getHotelCheckInTime() { return hotelCheckInTime; }
    public void setHotelCheckInTime(String hotelCheckInTime) { this.hotelCheckInTime = hotelCheckInTime; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(String availableUntil) { this.availableUntil = availableUntil; }

    public Double getMaxDistanceKm() { return maxDistanceKm; }
    public void setMaxDistanceKm(Double maxDistanceKm) { this.maxDistanceKm = maxDistanceKm; }

    public Double getMaxEntryFee() { return maxEntryFee; }
    public void setMaxEntryFee(Double maxEntryFee) { this.maxEntryFee = maxEntryFee; }

    public String getTravelDistancePref() { return travelDistancePref; }
    public void setTravelDistancePref(String travelDistancePref) { this.travelDistancePref = travelDistancePref; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
}
