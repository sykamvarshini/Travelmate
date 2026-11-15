package com.travelmate.service;

import com.travelmate.dto.AdminOverviewResponse;
import com.travelmate.dto.AdminTripDto;
import com.travelmate.dto.AdminUserDto;
import com.travelmate.entity.AdminActivityLog;
import com.travelmate.entity.Trip;
import com.travelmate.entity.User;
import com.travelmate.repository.DestinationRepository;
import com.travelmate.repository.ItineraryDayRepository;
import com.travelmate.repository.TripRepository;
import com.travelmate.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final AdminActivityLogService adminActivityLogService;

    public AdminService(UserRepository userRepository,
                        TripRepository tripRepository,
                        DestinationRepository destinationRepository,
                        ItineraryDayRepository itineraryDayRepository,
                        AdminActivityLogService adminActivityLogService) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.adminActivityLogService = adminActivityLogService;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        long totalUsers = userRepository.count();
        long totalTrips = tripRepository.count();
        long totalDestinations = destinationRepository.count();
        
        long totalItineraries = 0;
        try {
            totalItineraries = itineraryDayRepository.countDistinctTripsWithItinerary();
        } catch (Exception e) {
            // Fallback if no distinct count
            totalItineraries = itineraryDayRepository.count();
        }

        // Fetch recent users
        List<User> recentUserEntities = userRepository.findTop10ByOrderByCreatedAtDesc();
        List<AdminUserDto> recentUsers = recentUserEntities.stream()
                .map(u -> new AdminUserDto(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().name() : "ROLE_USER",
                        u.getAge(),
                        u.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // Fetch recent trips with creator info
        List<Trip> recentTripEntities = new ArrayList<>();
        try {
            recentTripEntities = tripRepository.findRecentTripsWithUser(PageRequest.of(0, 10));
        } catch (Exception e) {
            recentTripEntities = tripRepository.findTop10ByOrderByCreatedAtDesc();
        }

        List<AdminTripDto> recentTrips = recentTripEntities.stream()
                .map(t -> new AdminTripDto(
                        t.getId(),
                        t.getTitle(),
                        t.getDestinationCity(),
                        t.getStartDate(),
                        t.getEndDate(),
                        t.getNumDays(),
                        t.getBudgetInr(),
                        t.getStatus(),
                        t.getUser() != null ? t.getUser().getFullName() : "Unknown User",
                        t.getUser() != null ? t.getUser().getEmail() : "Unknown Email",
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // Fetch recent activity logs
        List<AdminActivityLog> recentActivity = adminActivityLogService.getRecentLogs();

        return new AdminOverviewResponse(
                totalUsers,
                totalTrips,
                totalDestinations,
                totalItineraries,
                recentUsers,
                recentTrips,
                recentActivity
        );
    }
}
