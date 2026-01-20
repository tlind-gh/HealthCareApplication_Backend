package healthcareab.project.healthcare_booking_app.controllers;

import healthcareab.project.healthcare_booking_app.dto.AvailabilityRequest;
import healthcareab.project.healthcare_booking_app.dto.AvailabilityResponse;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.services.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<List<AvailabilityResponse>> createAvailability(
            @Valid @RequestBody AvailabilityRequest request) {

        List<Availability> availabilities = availabilityService.createAvailability(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        List<AvailabilityResponse> response = availabilities.stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @RequestParam(required = false) String providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Availability> availabilities;

        if (providerId == null || providerId.isBlank()) {
            // Use currently authenticated provider
            availabilities = availabilityService.getAvailabilitiesForCurrentProvider(from, to);
        } else {
            // Admin asking for a specific provider
            availabilities = availabilityService.getAvailabilitiesForProvider(providerId, from, to);
        }

        List<AvailabilityResponse> responses = availabilities.stream()
                .map(AvailabilityResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @PathVariable String id,
            @Valid @RequestBody AvailabilityRequest request) {

        Availability availability = availabilityService.updateAvailability(
                id,
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        AvailabilityResponse response = AvailabilityResponse.fromEntity(availability);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<String> deleteAvailability(@PathVariable String id) {
        availabilityService.deleteAvailability(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body("Availability deleted successfully");
    }
}