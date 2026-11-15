package com.travelmate.repository;

import com.travelmate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(@NonNull String email);
    Boolean existsByEmail(@NonNull String email);
    List<User> findTop10ByOrderByCreatedAtDesc();
}
