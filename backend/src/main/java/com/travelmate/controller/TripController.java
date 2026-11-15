package com.travelmate.controller;

import com.travelmate.dto.InviteRequest;
import com.travelmate.dto.TripCreateRequest;
import com.travelmate.entity.Trip;
import com.travelmate.entity.TripMember;
import com.travelmate.entity.TripRole;
import com.travelmate.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody TripCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.createTrip(email, request);
        return ResponseEntity.ok(trip);
    }

    @GetMapping
    public ResponseEntity<List<Trip>> getUserTrips() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        List<Trip> trips = tripService.getUserTrips(email);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTrip(@PathVariable @NonNull Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : null;
        Trip trip = tripService.getTripById(email, id);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/adapt")
    public ResponseEntity<Trip> adaptTripDay(
            @PathVariable @NonNull Long id, 
            @RequestParam @NonNull Long dayId, 
            @RequestParam @NonNull String weatherCondition, 
            @RequestParam @NonNull Double rainProb, 
            @RequestParam @NonNull Double temp) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : "traveler@travelmate.com";
        Trip adaptedTrip = tripService.adaptTripDay(email, id, dayId, weatherCondition, rainProb, temp);
        return ResponseEntity.ok(adaptedTrip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTrip(@PathVariable @NonNull Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : "traveler@travelmate.com";
        tripService.deleteTrip(email, id);
        return ResponseEntity.ok(Map.of("message", "Trip deleted successfully"));
    }

    @PostMapping("/{id}/activities/{activityId}/complete")
    public ResponseEntity<Trip> completeActivity(@PathVariable @NonNull Long id, @PathVariable @NonNull Long activityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.markActivityCompleted(email, id, activityId);
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/{id}/activities/{activityId}/next-check")
    public ResponseEntity<Map<String, Object>> checkNextActivity(
            @PathVariable @NonNull Long id, 
            @PathVariable @NonNull Long activityId,
            @RequestParam(required = false) String weatherOverride) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Map<String, Object> check = tripService.checkNextActivity(email, id, activityId, weatherOverride);
        return ResponseEntity.ok(check);
    }

    @GetMapping("/{id}/activities/{activityId}/alternatives")
    public ResponseEntity<List<Map<String, Object>>> getAlternatives(
            @PathVariable @NonNull Long id, 
            @PathVariable @NonNull Long activityId,
            @RequestParam(required = false, defaultValue = "Weather concern") String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        List<Map<String, Object>> alts = tripService.getAlternatives(email, id, activityId, reason);
        return ResponseEntity.ok(alts);
    }

    @PostMapping("/{id}/activities/{activityId}/replace")
    public ResponseEntity<Trip> replaceActivity(
            @PathVariable @NonNull Long id, 
            @PathVariable @NonNull Long activityId,
            @RequestBody Map<String, Object> replacementData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.replaceActivity(email, id, activityId, replacementData);
        return ResponseEntity.ok(trip);
    }

    @DeleteMapping("/{id}/activities/{activityId}")
    public ResponseEntity<Trip> removeActivity(@PathVariable @NonNull Long id, @PathVariable @NonNull Long activityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.removeActivity(email, id, activityId);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{id}/activities/{activityId}")
    public ResponseEntity<Trip> updateActivity(
            @PathVariable @NonNull Long id,
            @PathVariable @NonNull Long activityId,
            @RequestBody Map<String, Object> updateData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.updateActivity(email, id, activityId, updateData);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/days/{dayId}/activities")
    public ResponseEntity<Trip> addActivity(
            @PathVariable @NonNull Long id,
            @PathVariable @NonNull Long dayId,
            @RequestBody Map<String, Object> activityData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.addActivityToDay(email, id, dayId, activityData);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/days/{dayId}/reorder")
    public ResponseEntity<Trip> reorderActivities(
            @PathVariable @NonNull Long id,
            @PathVariable @NonNull Long dayId,
            @RequestBody Map<String, Object> payload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        List<Long> activityIds = new ArrayList<>();
        if (payload != null && payload.containsKey("activityIds")) {
            Object obj = payload.get("activityIds");
            if (obj instanceof List<?>) {
                for (Object item : (List<?>) obj) {
                    if (item instanceof Number) {
                        activityIds.add(((Number) item).longValue());
                    } else if (item != null) {
                        try {
                            activityIds.add(Long.parseLong(item.toString()));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        Trip trip = tripService.reorderDayActivities(email, id, dayId, activityIds);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/activities/{activityId}/move-day")
    public ResponseEntity<Trip> moveActivityToDay(
            @PathVariable @NonNull Long id,
            @PathVariable @NonNull Long activityId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";

        Long targetDayId = null;
        Integer targetDayNumber = null;
        Integer targetPosition = null;

        if (payload != null) {
            // Target Day ID resolution
            if (payload.containsKey("targetDayId") && payload.get("targetDayId") != null) {
                try {
                    targetDayId = Long.parseLong(payload.get("targetDayId").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("dayId") && payload.get("dayId") != null) {
                try {
                    targetDayId = Long.parseLong(payload.get("dayId").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("destinationDayId") && payload.get("destinationDayId") != null) {
                try {
                    targetDayId = Long.parseLong(payload.get("destinationDayId").toString());
                } catch (Exception ignored) {}
            }

            // Target Day Number resolution (e.g. Day 1, Day 2)
            if (payload.containsKey("targetDayNumber") && payload.get("targetDayNumber") != null) {
                try {
                    targetDayNumber = Integer.parseInt(payload.get("targetDayNumber").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("dayNumber") && payload.get("dayNumber") != null) {
                try {
                    targetDayNumber = Integer.parseInt(payload.get("dayNumber").toString());
                } catch (Exception ignored) {}
            }

            // Target Position resolution (1-based position or 0-based itemOrder)
            if (payload.containsKey("targetPosition") && payload.get("targetPosition") != null) {
                try {
                    targetPosition = Integer.parseInt(payload.get("targetPosition").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("position") && payload.get("position") != null) {
                try {
                    targetPosition = Integer.parseInt(payload.get("position").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("pos") && payload.get("pos") != null) {
                try {
                    targetPosition = Integer.parseInt(payload.get("pos").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("newPosition") && payload.get("newPosition") != null) {
                try {
                    targetPosition = Integer.parseInt(payload.get("newPosition").toString());
                } catch (Exception ignored) {}
            } else if (payload.containsKey("itemOrder") && payload.get("itemOrder") != null) {
                try {
                    targetPosition = Integer.parseInt(payload.get("itemOrder").toString()) + 1;
                } catch (Exception ignored) {}
            }
        }

        Trip trip = tripService.moveActivityToDay(email, id, activityId, targetDayId, targetDayNumber, targetPosition);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable @NonNull Long id, @RequestBody TripCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.updateTrip(email, id, request);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<TripMember> inviteMember(@PathVariable @NonNull Long id, @RequestBody InviteRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        TripMember member = tripService.inviteMember(email, id, request.getEmail(), request.getRole());
        return ResponseEntity.ok(member);
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<TripMember>> getUserInvitations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        List<TripMember> invites = tripService.getUserInvitations(email);
        return ResponseEntity.ok(invites);
    }

    @PostMapping("/invitations/{inviteId}/respond")
    public ResponseEntity<Map<String, Object>> respondToInvitation(@PathVariable @NonNull Long inviteId, @RequestParam @NonNull String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Map<String, Object> resp = tripService.respondToInvitation(email, inviteId, action);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Map<String, Object>> removeMember(@PathVariable @NonNull Long id, @PathVariable @NonNull Long memberId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        tripService.removeMember(email, id, memberId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<TripMember> updateMemberRole(@PathVariable @NonNull Long id, @PathVariable @NonNull Long memberId, @RequestBody Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        TripRole role = TripRole.valueOf(body.getOrDefault("role", "VIEWER").toUpperCase());
        TripMember updated = tripService.updateMemberRole(email, id, memberId, role);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/optimize-route")
    public ResponseEntity<Trip> optimizeTripRoute(@PathVariable @NonNull Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.optimizeTripRoute(email, id);
        return ResponseEntity.ok(trip);
    }
}
