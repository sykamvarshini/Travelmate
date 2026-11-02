package com.travelmate.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "trips", "password"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_id", nullable = true)
    private Destination destination;

    @Column(length = 150)
    private String destinationCity;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer numDays = 1;
    private Integer numTravelers = 1;
    private Double budgetInr = 30000.0;
    private Double totalEstimatedCost = 0.0;

    @Column(length = 30)
    private String status = "PLANNED"; // PLANNED, ACTIVE, COMPLETED, CANCELLED

    private String travelStyle = "Solo";
    private String activityLevel = "Moderate";
    private String foodPreference = "Any";
    private String interests = "Beaches,Adventure";

    @Column(length = 150)
    private String startingLocation;

    @Column(length = 100)
    private String transitPreference;

    @Column(columnDefinition = "TEXT")
    private String transportNotice;

    private Double originLatitude;
    private Double originLongitude;

    private Double destinationLatitude;
    private Double destinationLongitude;

    private Integer durationHours;
    private String startTime;
    private String availableUntil;
    private Double maxDistanceKm;
    private Double maxEntryFee;
    private String travelDistancePref;
    private String locality;

    @Column(columnDefinition = "TEXT")
    private String lastUpdateNotice;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dayNumber ASC")
    private List<ItineraryDay> days = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"trip"})
    private List<TripMember> members = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Trip() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }

    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

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

    public Double getTotalEstimatedCost() { return totalEstimatedCost; }
    public void setTotalEstimatedCost(Double totalEstimatedCost) { this.totalEstimatedCost = totalEstimatedCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTravelStyle() { return travelStyle; }
    public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getFoodPreference() { return foodPreference; }
    public void setFoodPreference(String foodPreference) { this.foodPreference = foodPreference; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public List<ItineraryDay> getDays() { return days; }
    public void setDays(List<ItineraryDay> days) { this.days = days; }

    public String getStartingLocation() { return startingLocation; }
    public void setStartingLocation(String startingLocation) { this.startingLocation = startingLocation; }

    public String getTransitPreference() { return transitPreference; }
    public void setTransitPreference(String transitPreference) { this.transitPreference = transitPreference; }

    public String getTransportNotice() { return transportNotice; }
    public void setTransportNotice(String transportNotice) { this.transportNotice = transportNotice; }

    public Double getOriginLatitude() { return originLatitude; }
    public void setOriginLatitude(Double originLatitude) { this.originLatitude = originLatitude; }

    public Double getOriginLongitude() { return originLongitude; }
    public void setOriginLongitude(Double originLongitude) { this.originLongitude = originLongitude; }

    public Double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }

    public Double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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

    public String getLastUpdateNotice() { return lastUpdateNotice; }
    public void setLastUpdateNotice(String lastUpdateNotice) { this.lastUpdateNotice = lastUpdateNotice; }

    public List<TripMember> getMembers() { return members; }
    public void setMembers(List<TripMember> members) { this.members = members; }
}
