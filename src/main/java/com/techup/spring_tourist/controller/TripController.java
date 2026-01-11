package com.techup.spring_tourist.controller;

import java.util.List;
import java.util.Map;
import com.techup.spring_tourist.dto.PaginatedResponse;
import com.techup.spring_tourist.entity.Trip;
import com.techup.spring_tourist.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ResponseEntity<?> getAllTrips(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (limit <= 0) {
            limit = 10;
        }
        if (page < 0) {
            page = 0;
        }
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            PaginatedResponse<Trip> response = tripService.searchTrips(keyword, page, limit);
            return ResponseEntity.ok(response);
        }
        
        PaginatedResponse<Trip> response = tripService.getAllTrips(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-trips")
    public ResponseEntity<?> getMyTrips(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            if (limit <= 0) {
                limit = 10;
            }
            if (page < 0) {
                page = 0;
            }
            
            String userEmail = authentication.getName();
            PaginatedResponse<Trip> response = tripService.getTripsByUserEmail(userEmail, page, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public Trip getTripById(@PathVariable Long id) {
        return tripService.getTripById(id);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createTrip(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("photos") MultipartFile[] photos,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam("location") String location,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            Authentication authentication) {

        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Title is required"));
            }
            if (location == null || location.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Location is required"));
            }

            if (photos == null || photos.length == 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "At least 1 photo is required"));
            }
            if (photos.length > 5) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Maximum 5 photos allowed"));
            }

            for (int i = 0; i < photos.length; i++) {
                if (photos[i] == null || photos[i].isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Photo at index " + i + " is empty"));
                }
            }

            String userEmail = authentication.getName();

            Trip trip = tripService.createTripWithPhotos(
                    title, description, photos, tags, location, latitude, longitude, userEmail);

            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create trip: " + e.getMessage(),
                            "type", e.getClass().getSimpleName()));
        }
    }

    @PostMapping(value = "/json", consumes = "application/json")
    public Trip createTripJson(@RequestBody Trip trip) {
        return tripService.createTrip(trip);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateTrip(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "photos", required = false) MultipartFile[] photos,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam("location") String location,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            Authentication authentication) {
        
        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Title is required"));
            }
            if (location == null || location.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Location is required"));
            }

            if (photos != null && photos.length > 0) {
                if (photos.length > 5) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Maximum 5 photos allowed"));
                }

                for (int i = 0; i < photos.length; i++) {
                    if (photos[i] == null || photos[i].isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Photo at index " + i + " is empty"));
                    }
                }
            }

            String userEmail = authentication.getName();

            Trip trip = tripService.updateTripWithPhotos(
                    id, title, description, photos, tags, location, latitude, longitude, userEmail);

            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason() != null ? e.getReason() : e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update trip: " + e.getMessage(),
                            "type", e.getClass().getSimpleName()));
        }
    }

    @PutMapping(value = "/{id}/json", consumes = "application/json")
    public Trip updateTripJson(@PathVariable Long id, @RequestBody Trip trip) {
        return tripService.updateTrip(id, trip);
    }

    @DeleteMapping("/{id}")
    public void deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
    }

}
