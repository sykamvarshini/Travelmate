package com.travelmate.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;

    private Integer dayNumber;
    private LocalDate date;

    private String weatherCondition = "Sunny";
    private Double rainProbability = 0.10;
    private Double maxTempC = 30.0;
    private Double minTempC = 23.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("startTime ASC")
    private List<ItineraryItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TravelSegment> travelSegments = new ArrayList<>();

    public ItineraryDay() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public Double getRainProbability() { return rainProbability; }
    public void setRainProbability(Double rainProbability) { this.rainProbability = rainProbability; }

    public Double getMaxTempC() { return maxTempC; }
    public void setMaxTempC(Double maxTempC) { this.maxTempC = maxTempC; }

    public Double getMinTempC() { return minTempC; }
    public void setMinTempC(Double minTempC) { this.minTempC = minTempC; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<ItineraryItem> getItems() { return items; }
    public void setItems(List<ItineraryItem> items) { this.items = items; }

    public List<TravelSegment> getTravelSegments() { return travelSegments; }
    public void setTravelSegments(List<TravelSegment> travelSegments) { this.travelSegments = travelSegments; }
}
