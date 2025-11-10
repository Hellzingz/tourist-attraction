package com.techup.spring_tourist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.techup.spring_tourist.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> { 
}
