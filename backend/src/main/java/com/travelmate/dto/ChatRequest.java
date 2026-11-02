package com.travelmate.dto;

public class ChatRequest {
    private String message;
    private Long tripId;
    private String currentCity;

    public ChatRequest() {}
    public ChatRequest(String message, Long tripId, String currentCity) {
        this.message = message;
        this.tripId = tripId;
        this.currentCity = currentCity;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public String getCurrentCity() { return currentCity; }
    public void setCurrentCity(String currentCity) { this.currentCity = currentCity; }
}
