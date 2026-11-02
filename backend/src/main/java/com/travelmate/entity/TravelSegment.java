package com.travelmate.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "travel_segments")
public class TravelSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    @JsonIgnore
    private ItineraryDay itineraryDay;

    private String fromTitle;
    private String toTitle;
    private Double distanceKm;
    private Integer durationMinutes;
    private String travelMode = "Cab"; // Cab, Walk, Auto, Metro

    public TravelSegment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItineraryDay getItineraryDay() { return itineraryDay; }
    public void setItineraryDay(ItineraryDay itineraryDay) { this.itineraryDay = itineraryDay; }

    public String getFromTitle() { return fromTitle; }
    public void setFromTitle(String fromTitle) { this.fromTitle = fromTitle; }

    public String getToTitle() { return toTitle; }
    public void setToTitle(String toTitle) { this.toTitle = toTitle; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getTravelMode() { return travelMode; }
    public void setTravelMode(String travelMode) { this.travelMode = travelMode; }
}
