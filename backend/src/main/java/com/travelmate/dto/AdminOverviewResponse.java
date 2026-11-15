package com.travelmate.dto;

import com.travelmate.entity.AdminActivityLog;

import java.util.List;

public class AdminOverviewResponse {
    private long totalUsers;
    private long totalTrips;
    private long totalDestinations;
    private long totalItineraries;
    private List<AdminUserDto> recentUsers;
    private List<AdminTripDto> recentTrips;
    private List<AdminActivityLog> recentActivity;

    public AdminOverviewResponse() {}

    public AdminOverviewResponse(long totalUsers, long totalTrips, long totalDestinations,
                                 long totalItineraries, List<AdminUserDto> recentUsers,
                                 List<AdminTripDto> recentTrips, List<AdminActivityLog> recentActivity) {
        this.totalUsers = totalUsers;
        this.totalTrips = totalTrips;
        this.totalDestinations = totalDestinations;
        this.totalItineraries = totalItineraries;
        this.recentUsers = recentUsers;
        this.recentTrips = recentTrips;
        this.recentActivity = recentActivity;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(long totalTrips) {
        this.totalTrips = totalTrips;
    }

    public long getTotalDestinations() {
        return totalDestinations;
    }

    public void setTotalDestinations(long totalDestinations) {
        this.totalDestinations = totalDestinations;
    }

    public long getTotalItineraries() {
        return totalItineraries;
    }

    public void setTotalItineraries(long totalItineraries) {
        this.totalItineraries = totalItineraries;
    }

    public List<AdminUserDto> getRecentUsers() {
        return recentUsers;
    }

    public void setRecentUsers(List<AdminUserDto> recentUsers) {
        this.recentUsers = recentUsers;
    }

    public List<AdminTripDto> getRecentTrips() {
        return recentTrips;
    }

    public void setRecentTrips(List<AdminTripDto> recentTrips) {
        this.recentTrips = recentTrips;
    }

    public List<AdminActivityLog> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<AdminActivityLog> recentActivity) {
        this.recentActivity = recentActivity;
    }
}
