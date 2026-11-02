package com.travelmate.dto;

public class ReplanRequest {
    private String reason; // WEATHER_CHANGE, ATTRACTION_CLOSED, TRANSIT_DELAY, BUDGET_OVERRUN, PREFERENCE_CHANGE
    private Integer targetDayNumber;
    private String simulatedWeatherCondition; // "Heavy Rain"
    private Double simulatedRainProbability;   // 0.85
    private Long closedAttractionId;
    private String newArrivalTime;            // "19:00"
    private Double newBudgetInr;
    private String newActivityLevel;
    private String newTravelStyle;

    public ReplanRequest() {}

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getTargetDayNumber() { return targetDayNumber; }
    public void setTargetDayNumber(Integer targetDayNumber) { this.targetDayNumber = targetDayNumber; }

    public String getSimulatedWeatherCondition() { return simulatedWeatherCondition; }
    public void setSimulatedWeatherCondition(String simulatedWeatherCondition) { this.simulatedWeatherCondition = simulatedWeatherCondition; }

    public Double getSimulatedRainProbability() { return simulatedRainProbability; }
    public void setSimulatedRainProbability(Double simulatedRainProbability) { this.simulatedRainProbability = simulatedRainProbability; }

    public Long getClosedAttractionId() { return closedAttractionId; }
    public void setClosedAttractionId(Long closedAttractionId) { this.closedAttractionId = closedAttractionId; }

    public String getNewArrivalTime() { return newArrivalTime; }
    public void setNewArrivalTime(String newArrivalTime) { this.newArrivalTime = newArrivalTime; }

    public Double getNewBudgetInr() { return newBudgetInr; }
    public void setNewBudgetInr(Double newBudgetInr) { this.newBudgetInr = newBudgetInr; }

    public String getNewActivityLevel() { return newActivityLevel; }
    public void setNewActivityLevel(String newActivityLevel) { this.newActivityLevel = newActivityLevel; }

    public String getNewTravelStyle() { return newTravelStyle; }
    public void setNewTravelStyle(String newTravelStyle) { this.newTravelStyle = newTravelStyle; }
}
