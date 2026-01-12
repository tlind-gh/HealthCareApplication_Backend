package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repository.AvailabilityRepository;
import healthcareab.project.healthcare_booking_app.repository.UserAuthRepository;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;


import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final UserService userService;
    
    public AvailabilityService(AvailabilityRepository availabilityRepository, UserService userService) {
        this.availabilityRepository = availabilityRepository;
        this.userService = userService;
    }
    
    public Availability createAvailability(LocalDate date, LocalTime startTime, LocalTime endTime) {
        userService.assertCurrentUserAuthenticated();
        User user = userService.getCurrentUser();
        
        if(!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        Availability availability = new Availability();
        availability.setProviderId(user.getId());
        availability.setDate(date);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setIsAvailable(true);
        
        return availabilityRepository.save(availability);
    }
}