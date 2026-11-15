package com.travelmate.repository;

import com.travelmate.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
    List<Attraction> findByCityNameIgnoreCase(@NonNull String cityName);
    List<Attraction> findByCityNameIgnoreCaseAndIsAvailableTrue(@NonNull String cityName);
    List<Attraction> findByFeaturedTrue();
    Optional<Attraction> findByNameIgnoreCase(@NonNull String name);
    List<Attraction> findByCityStateNameIgnoreCase(@NonNull String stateName);
    List<Attraction> findByCategorySlugIgnoreCase(@NonNull String slug);
    List<Attraction> findByCategoryNameIgnoreCase(@NonNull String categoryName);

    @Query("SELECT a FROM Attraction a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.city.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.city.state.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.category.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.tags) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Attraction> searchAttractions(@Param("query") String query);
}
