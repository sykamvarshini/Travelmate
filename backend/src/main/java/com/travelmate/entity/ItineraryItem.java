package com.travelmate.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "itinerary_items")
public class ItineraryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    @JsonIgnore
    private ItineraryDay itineraryDay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attraction_id", nullable = true)
    private Attraction attraction;

    @Column(nullable = false, length = 150)
    private String title;

    private String startTime; // "09:00"
    private String endTime;   // "11:00"
    private Integer durationMinutes = 120;
    private Double costInr = 0.0;

    private String category;
    private String indoorOutdoor = "Outdoor";
    private Double latitude;
    private Double longitude;

    private String status = "SCHEDULED"; // SCHEDULED, REPLACED, CANCELLED, COMPLETED

    @Column(columnDefinition = "TEXT")
    private String recommendationReason;

    private String weatherAlert;
    private Integer itemOrder = 0;

    private String transportFromPrevious;
    private Double distanceFromPrevious;
    private Integer durationFromPrevious;
    private Double estimatedTransportCost;
    private String verificationStatus = "Verified Real Location";

    private Double rating;
    private Integer reviewCount;
    private String openingHours;
    private String address;
    private Double aiMatchScore;

    public ItineraryItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItineraryDay getItineraryDay() { return itineraryDay; }
    public void setItineraryDay(ItineraryDay itineraryDay) { this.itineraryDay = itineraryDay; }

    public Attraction getAttraction() { return attraction; }
    public void setAttraction(Attraction attraction) { this.attraction = attraction; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Double getCostInr() { return costInr; }
    public void setCostInr(Double costInr) { this.costInr = costInr; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIndoorOutdoor() { return indoorOutdoor; }
    public void setIndoorOutdoor(String indoorOutdoor) { this.indoorOutdoor = indoorOutdoor; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecommendationReason() { return recommendationReason; }
    public void setRecommendationReason(String recommendationReason) { this.recommendationReason = recommendationReason; }

    public String getWeatherAlert() { return weatherAlert; }
    public void setWeatherAlert(String weatherAlert) { this.weatherAlert = weatherAlert; }

    public Integer getItemOrder() { return itemOrder; }
    public void setItemOrder(Integer itemOrder) { this.itemOrder = itemOrder; }

    public String getTransportFromPrevious() { return transportFromPrevious; }
    public void setTransportFromPrevious(String transportFromPrevious) { this.transportFromPrevious = transportFromPrevious; }

    public Double getDistanceFromPrevious() { return distanceFromPrevious; }
    public void setDistanceFromPrevious(Double distanceFromPrevious) { this.distanceFromPrevious = distanceFromPrevious; }

    public Integer getDurationFromPrevious() { return durationFromPrevious; }
    public void setDurationFromPrevious(Integer durationFromPrevious) { this.durationFromPrevious = durationFromPrevious; }

    public Double getEstimatedTransportCost() { return estimatedTransportCost; }
    public void setEstimatedTransportCost(Double estimatedTransportCost) { this.estimatedTransportCost = estimatedTransportCost; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getAiMatchScore() { return aiMatchScore; }
    public void setAiMatchScore(Double aiMatchScore) { this.aiMatchScore = aiMatchScore; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItineraryItem that = (ItineraryItem) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }
}
