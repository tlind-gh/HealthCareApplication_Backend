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
    private final UserAuthRepository userAuthRepository;
    
    public AvailabilityService(AvailabilityRepository availabilityRepository, UserAuthRepository userAuthRepository) {
        this.availabilityRepository = availabilityRepository;
        this.userAuthRepository = userAuthRepository;
        
    }
    
    public Availability createAvailability(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    )  {
        User user = userAuthRepository.authenticateAndExtractUser();
        
        if(user.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can create availability");
        }
        
        if(!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        Availability availability = new Availability();
        availability.setDate(date);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setIsAvailable(true);
        
        return availabilityRepository.save(availability);
    }
}