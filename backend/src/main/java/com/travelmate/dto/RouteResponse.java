package com.travelmate.dto;

import java.util.List;

public class RouteResponse {
    private Double totalDistanceKm;
    private Integer totalDurationMinutes;
    private String routeSummary;
    private List<List<Double>> polylineCoordinates; // [[lat, lon], ...]
    private Boolean isLiveRouting;

    public RouteResponse() {}
    public RouteResponse(Double totalDistanceKm, Integer totalDurationMinutes, String routeSummary, List<List<Double>> polylineCoordinates, Boolean isLiveRouting) {
        this.totalDistanceKm = totalDistanceKm;
        this.totalDurationMinutes = totalDurationMinutes;
        this.routeSummary = routeSummary;
        this.polylineCoordinates = polylineCoordinates;
        this.isLiveRouting = isLiveRouting;
    }

    public Double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(Double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public Integer getTotalDurationMinutes() { return totalDurationMinutes; }
    public void setTotalDurationMinutes(Integer totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }

    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }

    public List<List<Double>> getPolylineCoordinates() { return polylineCoordinates; }
    public void setPolylineCoordinates(List<List<Double>> polylineCoordinates) { this.polylineCoordinates = polylineCoordinates; }

    public Boolean getIsLiveRouting() { return isLiveRouting; }
    public void setIsLiveRouting(Boolean isLiveRouting) { this.isLiveRouting = isLiveRouting; }
}
