package com.travelmate.controller;

import com.travelmate.dto.InviteRequest;
import com.travelmate.dto.TripCreateRequest;
import com.travelmate.entity.Trip;
import com.travelmate.entity.TripMember;
import com.travelmate.entity.TripRole;
import com.travelmate.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Trip> getTrip(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : null;
        Trip trip = tripService.getTripById(email, id);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/adapt")
    public ResponseEntity<Trip> adaptTripDay(
            @PathVariable Long id, 
            @RequestParam Long dayId, 
            @RequestParam String weatherCondition, 
            @RequestParam Double rainProb, 
            @RequestParam Double temp) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Trip adaptedTrip = tripService.adaptTripDay(email, id, dayId, weatherCondition, rainProb, temp);
        return ResponseEntity.ok(adaptedTrip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrip(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : "traveler@travelmate.com";
        tripService.deleteTrip(email, id);
        return ResponseEntity.ok(java.util.Map.of("message", "Trip deleted successfully"));
    }

    @PostMapping("/{id}/activities/{activityId}/complete")
    public ResponseEntity<Trip> completeActivity(@PathVariable Long id, @PathVariable Long activityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.markActivityCompleted(email, id, activityId);
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/{id}/activities/{activityId}/next-check")
    public ResponseEntity<java.util.Map<String, Object>> checkNextActivity(
            @PathVariable Long id, 
            @PathVariable Long activityId,
            @RequestParam(required = false) String weatherOverride) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        java.util.Map<String, Object> check = tripService.checkNextActivity(email, id, activityId, weatherOverride);
        return ResponseEntity.ok(check);
    }

    @GetMapping("/{id}/activities/{activityId}/alternatives")
    public ResponseEntity<List<java.util.Map<String, Object>>> getAlternatives(
            @PathVariable Long id, 
            @PathVariable Long activityId,
            @RequestParam(required = false, defaultValue = "Weather concern") String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        List<java.util.Map<String, Object>> alts = tripService.getAlternatives(email, id, activityId, reason);
        return ResponseEntity.ok(alts);
    }

    @PostMapping("/{id}/activities/{activityId}/replace")
    public ResponseEntity<Trip> replaceActivity(
            @PathVariable Long id, 
            @PathVariable Long activityId,
            @RequestBody java.util.Map<String, Object> replacementData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.replaceActivity(email, id, activityId, replacementData);
        return ResponseEntity.ok(trip);
    }

    @DeleteMapping("/{id}/activities/{activityId}")
    public ResponseEntity<Trip> removeActivity(@PathVariable Long id, @PathVariable Long activityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.removeActivity(email, id, activityId);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable Long id, @RequestBody TripCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        Trip trip = tripService.updateTrip(email, id, request);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<TripMember> inviteMember(@PathVariable Long id, @RequestBody InviteRequest request) {
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
    public ResponseEntity<?> respondToInvitation(@PathVariable Long inviteId, @RequestParam String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        java.util.Map<String, Object> resp = tripService.respondToInvitation(email, inviteId, action);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        tripService.removeMember(email, id, memberId);
        return ResponseEntity.ok(java.util.Map.of("message", "Member removed successfully"));
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<TripMember> updateMemberRole(@PathVariable Long id, @PathVariable Long memberId, @RequestBody java.util.Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && !"anonymousUser".equals(authentication.getName())) 
                ? authentication.getName() 
                : "traveler@travelmate.com";
        TripRole role = TripRole.valueOf(body.getOrDefault("role", "VIEWER").toUpperCase());
        TripMember updated = tripService.updateMemberRole(email, id, memberId, role);
        return ResponseEntity.ok(updated);
    }
}
