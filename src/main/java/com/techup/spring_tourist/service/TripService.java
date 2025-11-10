package com.techup.spring_tourist.service;

import org.springframework.stereotype.Service;
import java.util.List;
import com.techup.spring_tourist.entity.Trip;
import com.techup.spring_tourist.repository.TripRepository;

@Service
public class TripService {

    private final TripRepository tripRepository;
    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }
}
