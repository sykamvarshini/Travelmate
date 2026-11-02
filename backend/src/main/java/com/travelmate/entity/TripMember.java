package com.travelmate.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_members")
public class TripMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnoreProperties({"members", "days", "user", "hibernateLazyInitializer", "handler"})
    private Trip trip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "trips", "preference", "hibernateLazyInitializer", "handler"})
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRole role = TripRole.VIEWER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(length = 150)
    private String invitedByEmail;

    @Column(length = 150)
    private String invitedByName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public TripMember() {}

    public TripMember(Trip trip, User user, TripRole role, InvitationStatus status, String invitedByEmail, String invitedByName) {
        this.trip = trip;
        this.user = user;
        this.role = role;
        this.status = status;
        this.invitedByEmail = invitedByEmail;
        this.invitedByName = invitedByName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TripRole getRole() { return role; }
    public void setRole(TripRole role) { this.role = role; }

    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }

    public String getInvitedByEmail() { return invitedByEmail; }
    public void setInvitedByEmail(String invitedByEmail) { this.invitedByEmail = invitedByEmail; }

    public String getInvitedByName() { return invitedByName; }
    public void setInvitedByName(String invitedByName) { this.invitedByName = invitedByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getTripId() { return trip != null ? trip.getId() : null; }
    public String getTripTitle() { return trip != null ? trip.getTitle() : null; }
    public String getTripDestination() { return trip != null ? trip.getDestinationCity() : null; }
    public String getTripStartingLocation() { return trip != null ? trip.getStartingLocation() : null; }
    public java.time.LocalDate getTripStartDate() { return trip != null ? trip.getStartDate() : null; }
    public java.time.LocalDate getTripEndDate() { return trip != null ? trip.getEndDate() : null; }
    public Double getTripBudget() { return trip != null ? trip.getBudgetInr() : null; }
}
