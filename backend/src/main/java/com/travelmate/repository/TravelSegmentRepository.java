package com.travelmate.repository;

import com.travelmate.entity.TravelSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelSegmentRepository extends JpaRepository<TravelSegment, Long> {
    List<TravelSegment> findByItineraryDayId(Long itineraryDayId);
}
