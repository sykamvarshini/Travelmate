package com.travelmate.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;

    @Column(nullable = false, length = 30)
    private String type; // FLIGHT, TRAIN, HOTEL, ACTIVITY

    private String provider;
    private String bookingRef;
    private String scheduledArrival; // "12:00"
    private String actualArrival;    // "19:00"
    private String status = "CONFIRMED"; // CONFIRMED, DELAYED, CANCELLED
    private Double costInr = 0.0;
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Booking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public String getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(String scheduledArrival) { this.scheduledArrival = scheduledArrival; }

    public String getActualArrival() { return actualArrival; }
    public void setActualArrival(String actualArrival) { this.actualArrival = actualArrival; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getCostInr() { return costInr; }
    public void setCostInr(Double costInr) { this.costInr = costInr; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
