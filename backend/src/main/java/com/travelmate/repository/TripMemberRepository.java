package com.travelmate.repository;

import com.travelmate.entity.InvitationStatus;
import com.travelmate.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripMemberRepository extends JpaRepository<TripMember, Long> {
    
    @Query("SELECT m FROM TripMember m WHERE m.trip.id = :tripId")
    List<TripMember> findByTripId(@Param("tripId") Long tripId);

    @Query("SELECT m FROM TripMember m WHERE m.user.id = :userId AND m.status = :status")
    List<TripMember> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") InvitationStatus status);

    @Query("SELECT m FROM TripMember m WHERE m.user.email = :email AND m.status = :status")
    List<TripMember> findByUserEmailAndStatus(@Param("email") String email, @Param("status") InvitationStatus status);

    @Query("SELECT m FROM TripMember m WHERE m.trip.id = :tripId AND m.user.id = :userId")
    Optional<TripMember> findByTripIdAndUserId(@Param("tripId") Long tripId, @Param("userId") Long userId);

    @Query("SELECT m FROM TripMember m WHERE m.trip.id = :tripId AND LOWER(m.user.email) = LOWER(:email)")
    Optional<TripMember> findByTripIdAndUserEmail(@Param("tripId") Long tripId, @Param("email") String email);

    @Modifying
    @Query("DELETE FROM TripMember m WHERE m.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);
}
