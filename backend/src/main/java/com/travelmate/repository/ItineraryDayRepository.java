package com.travelmate.repository;

import com.travelmate.entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    List<ItineraryDay> findByTripIdOrderByDayNumberAsc(@NonNull Long tripId);

    @Query("SELECT COUNT(DISTINCT d.trip.id) FROM ItineraryDay d")
    long countDistinctTripsWithItinerary();
}
