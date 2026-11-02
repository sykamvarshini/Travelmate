package com.travelmate.repository;

import com.travelmate.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCase(String name);
    List<City> findByPopularTrue();
    List<City> findByStateId(Long stateId);
}
