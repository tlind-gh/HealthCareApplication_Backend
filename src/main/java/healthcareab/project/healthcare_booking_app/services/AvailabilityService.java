package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.NotFoundException;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.AvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        
        LocalTime minTime = LocalTime.of(8, 0);
        LocalTime maxTime = LocalTime.of(17, 0);
        
        if(startTime.isBefore(minTime)) {
            throw new IllegalArgumentException("Start time must be at or after 08:00");
        }
        
        if(endTime.isAfter(maxTime)) {
            throw new IllegalArgumentException("End time must be at or before 17:00");
        }
        
        Availability availability = new Availability();
        availability.setProviderId(user.getId());
        availability.setDate(date);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setIsAvailable(true);
        
        return availabilityRepository.save(availability);
    }
    
    public List<Availability> getAvailabilitiesForProvider(String providerId, LocalDate from, LocalDate to) {
        List<Availability> availabilities = availabilityRepository.findByProviderIdAndDateBetween(providerId, from, to);
        
        // Sort first by date, then by startTime
        return availabilities.stream()
                .sorted(Comparator.comparing(Availability::getDate)
                        .thenComparing(Availability::getStartTime))
                .collect(Collectors.toList());
    }
    
    public Availability updateAvailability(String id, LocalDate date, LocalTime startTime, LocalTime endTime) {
        userService.assertCurrentUserAuthenticated();
        User currentUser = userService.getCurrentUser();
        
        Availability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Availability not found"));
        
        // Verify that the availability belongs to the logged-in healthcare provider
        if (!availability.getProviderId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only update your own availability");
        }
        
        // Validate startTime < endTime
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        // Validate time constraints (8:00-17:00)
        LocalTime minTime = LocalTime.of(8, 0);
        LocalTime maxTime = LocalTime.of(17, 0);
        
        if (startTime.isBefore(minTime)) {
            throw new IllegalArgumentException("Start time must be at or after 08:00");
        }
        
        if (endTime.isAfter(maxTime)) {
            throw new IllegalArgumentException("End time must be at or before 17:00");
        }
        
        // Update the availability
        availability.setDate(date);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        
        return availabilityRepository.save(availability);
    }
    
    public void deleteAvailability(String id) {
        userService.assertCurrentUserAuthenticated();
        User currentUser = userService.getCurrentUser();
        
        Availability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Availability not found"));
        
        // Verify that availability belongs to the logged-in healthcare provider (unless the user is admin)
        boolean isAdmin = currentUser.getRoles().contains(Role.ADMIN);
        boolean isProvider = availability.getProviderId().equals(currentUser.getId());
        
        if (!isAdmin && !isProvider) {
            throw new UnauthorizedException("You can only delete your own availability");
        }
        
        availabilityRepository.delete(availability);
    }
    // helper method for getting availbility in frontend
    public List<Availability> getAvailabilitiesForCurrentProvider(LocalDate from, LocalDate to) {
        userService.assertCurrentUserAuthenticated();
        User currentUser = userService.getCurrentUser();
        return getAvailabilitiesForProvider(currentUser.getId(), from, to);
    }
}