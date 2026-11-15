package com.travelmate.repository;

import com.travelmate.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    Optional<Destination> findByNameIgnoreCase(@NonNull String name);
    List<Destination> findByFeaturedTrue();
    List<Destination> findByCityNameIgnoreCase(@NonNull String cityName);
}
