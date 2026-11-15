package com.travelmate.repository;

import com.travelmate.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
    Optional<State> findByName(@NonNull String name);
    Optional<State> findByNameIgnoreCase(@NonNull String name);
}
