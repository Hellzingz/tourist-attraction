package com.techup.spring_tourist.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.techup.spring_tourist.entity.Trip;
import com.techup.spring_tourist.entity.User;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByAuthorOrderByCreatedAtDesc(User author);

    Page<Trip> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    @Query(value = "SELECT * FROM trips WHERE " +
           "LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM unnest(tags) AS tag WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY created_at DESC", 
           nativeQuery = true)
    List<Trip> findByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT * FROM trips WHERE " +
           "LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM unnest(tags) AS tag WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM trips WHERE " +
           "LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM unnest(tags) AS tag WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           nativeQuery = true)
    Page<Trip> findByKeywordPaged(@Param("keyword") String keyword, Pageable pageable);
}
