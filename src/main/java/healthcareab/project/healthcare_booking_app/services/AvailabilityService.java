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
import java.util.ArrayList;
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
    
    public List<Availability> createAvailability(LocalDate date, LocalTime startTime, LocalTime endTime) {
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

        // Ensure full 1-hour slots
        if (startTime.getMinute() != 0 || endTime.getMinute() != 0) {
            throw new IllegalArgumentException("Availability must be in full 1-hour blocks");
        }

        List<Availability> timeSlots = new ArrayList<>();

        LocalTime timeSlotStart = startTime;
        while (timeSlotStart.isBefore(endTime)) {
            LocalTime timeSlotEnd = timeSlotStart.plusHours(1);

            Availability availability = new Availability();
            availability.setProviderId(user.getId());
            availability.setDate(date);
            availability.setStartTime(timeSlotStart);
            availability.setEndTime(timeSlotEnd);
            availability.setIsAvailable(true);

            timeSlots.add(availability);

            timeSlotStart = timeSlotEnd;
        }

        return availabilityRepository.saveAll(timeSlots);
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
    
    public boolean isTimeAvailable(String providerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return availabilityRepository.isTimeAvailable(
                providerId,
                date,
                startTime,
                endTime
        );
    }
    
    public Availability getAvailableSlot(String providerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return availabilityRepository.findAvailableSlot(providerId, date, startTime, endTime)
                .orElseThrow(() -> new NotFoundException("Availability not found"));
    }

    public Availability getBookedSlot(String providerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return availabilityRepository.findBookedSlot(providerId, date, startTime, endTime)
                .orElseThrow(() -> new NotFoundException("Availability not found"));
    }
}