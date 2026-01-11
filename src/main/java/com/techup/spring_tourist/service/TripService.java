package com.techup.spring_tourist.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import com.techup.spring_tourist.dto.PaginatedResponse;
import com.techup.spring_tourist.entity.Trip;
import com.techup.spring_tourist.entity.User;
import com.techup.spring_tourist.repository.TripRepository;
import com.techup.spring_tourist.repository.UserRepository;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService supabaseStorageService;

    public TripService(TripRepository tripRepository, UserRepository userRepository, SupabaseStorageService supabaseStorageService) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Transactional(readOnly = true)
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Trip> getAllTrips(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<Trip> tripPage = tripRepository.findAll(pageable);
        
        return new PaginatedResponse<>(
            tripPage.getContent(),
            page,
            limit,
            tripPage.getTotalElements(),
            tripPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Trip getTripById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
    }

    @Transactional(readOnly = true)
    public List<Trip> getTripsByUserEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));
        return tripRepository.findByAuthorOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Trip> getTripsByUserEmail(String userEmail, int page, int limit) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));
        Pageable pageable = PageRequest.of(page, limit);
        Page<Trip> tripPage = tripRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
        
        return new PaginatedResponse<>(
            tripPage.getContent(),
            page,
            limit,
            tripPage.getTotalElements(),
            tripPage.getTotalPages()
        );
    }

    @Transactional
    public Trip createTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    public List<Trip> searchTrips(String keyword) {
        return tripRepository.findByKeyword(keyword);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Trip> searchTrips(String keyword, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<Trip> tripPage = tripRepository.findByKeywordPaged(keyword, pageable);
        
        return new PaginatedResponse<>(
            tripPage.getContent(),
            page,
            limit,
            tripPage.getTotalElements(),
            tripPage.getTotalPages()
        );
    }

    @Transactional
    public Trip createTripWithPhotos(
            String title,
            String description,
            MultipartFile[] photos,
            String[] tags,
            String location,
            Double latitude,
            Double longitude,
            String userEmail) {
        
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));

        String[] photoUrls;
        try {
            photoUrls = supabaseStorageService.uploadFiles(photos);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to upload photos: " + e.getMessage(), e);
        }

        Trip trip = new Trip();
        trip.setTitle(title);
        trip.setDescription(description);
        trip.setPhotos(photoUrls);
        trip.setTags(tags != null ? tags : new String[0]);
        trip.setLocation(location);
        trip.setLatitude(latitude);
        trip.setLongitude(longitude);
        trip.setAuthor(author);

        try {
            return tripRepository.save(trip);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to save trip: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Trip updateTrip(Long id, Trip trip) {
        Trip existingTrip = getTripById(id);
        existingTrip.setTitle(trip.getTitle());
        existingTrip.setDescription(trip.getDescription());
        existingTrip.setPhotos(trip.getPhotos());
        existingTrip.setTags(trip.getTags());
        existingTrip.setLatitude(trip.getLatitude());
        existingTrip.setLongitude(trip.getLongitude());
        return tripRepository.save(existingTrip);
    }

    @Transactional
    public Trip updateTripWithPhotos(
            Long id,
            String title,
            String description,
            MultipartFile[] photos,
            String[] tags,
            String location,
            Double latitude,
            Double longitude,
            String userEmail) {
        
        Trip existingTrip = getTripById(id);
        
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));
        
        if (existingTrip.getAuthor() == null || !existingTrip.getAuthor().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You can only edit your own trips");
        }
        
        String[] photoUrls;
        if (photos != null && photos.length > 0) {
            boolean hasValidFiles = false;
            for (MultipartFile photo : photos) {
                if (photo != null && !photo.isEmpty() && photo.getSize() > 0) {
                    hasValidFiles = true;
                    break;
                }
            }
            
            if (hasValidFiles) {
                try {
                    photoUrls = supabaseStorageService.uploadFiles(photos);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Failed to upload photos: " + e.getMessage(), e);
                }
            } else {
                photoUrls = existingTrip.getPhotos() != null ? existingTrip.getPhotos() : new String[0];
            }
        } else {
            photoUrls = existingTrip.getPhotos() != null ? existingTrip.getPhotos() : new String[0];
        }
        
        existingTrip.setTitle(title);
        existingTrip.setDescription(description);
        existingTrip.setPhotos(photoUrls);
        existingTrip.setTags(tags != null ? tags : new String[0]);
        existingTrip.setLocation(location);
        existingTrip.setLatitude(latitude);
        existingTrip.setLongitude(longitude);
        
        try {
            return tripRepository.save(existingTrip);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to save trip: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }
}
