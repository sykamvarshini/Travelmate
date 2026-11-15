package com.travelmate.repository;

import com.travelmate.entity.AttractionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttractionCategoryRepository extends JpaRepository<AttractionCategory, Long> {
    Optional<AttractionCategory> findBySlug(@NonNull String slug);
    Optional<AttractionCategory> findByNameIgnoreCase(@NonNull String name);
}
