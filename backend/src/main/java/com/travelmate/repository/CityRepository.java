package com.travelmate.repository;

import com.travelmate.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCase(@NonNull String name);
    List<City> findByPopularTrue();
    List<City> findByStateId(@NonNull Long stateId);
    List<City> findByStateNameIgnoreCase(@NonNull String stateName);

    @Query("SELECT c FROM City c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.state.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<City> searchCities(@Param("query") String query);
}
