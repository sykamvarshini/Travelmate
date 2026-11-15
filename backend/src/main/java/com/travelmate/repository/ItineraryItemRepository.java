package com.travelmate.repository;

import com.travelmate.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {
    List<ItineraryItem> findByItineraryDayIdOrderByStartTimeAsc(@NonNull Long itineraryDayId);
}
