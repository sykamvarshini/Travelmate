package com.travelmate.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminTripDto {
    private Long id;
    private String title;
    private String destinationCity;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numDays;
    private Double budgetInr;
    private String status;
    private String userName;
    private String userEmail;
    private LocalDateTime createdAt;

    public AdminTripDto() {}

    public AdminTripDto(Long id, String title, String destinationCity, LocalDate startDate,
                        LocalDate endDate, Integer numDays, Double budgetInr, String status,
                        String userName, String userEmail, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.destinationCity = destinationCity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numDays = numDays;
        this.budgetInr = budgetInr;
        this.status = status;
        this.userName = userName;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getNumDays() {
        return numDays;
    }

    public void setNumDays(Integer numDays) {
        this.numDays = numDays;
    }

    public Double getBudgetInr() {
        return budgetInr;
    }

    public void setBudgetInr(Double budgetInr) {
        this.budgetInr = budgetInr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
