package healthcareab.project.healthcare_booking_app.controllers;

import healthcareab.project.healthcare_booking_app.dto.AvailabilityRequest;
import healthcareab.project.healthcare_booking_app.dto.AvailabilityResponse;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.services.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/create")
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @Valid @RequestBody AvailabilityRequest request) {

        Availability availability = availabilityService.createAvailability(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        AvailabilityResponse response = AvailabilityResponse.fromEntity(availability);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}