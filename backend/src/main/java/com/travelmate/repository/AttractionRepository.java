package com.travelmate.repository;

import com.travelmate.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
    List<Attraction> findByCityNameIgnoreCase(String cityName);
    List<Attraction> findByCityNameIgnoreCaseAndIsAvailableTrue(String cityName);
    List<Attraction> findByFeaturedTrue();
    Optional<Attraction> findByNameIgnoreCase(String name);
}
