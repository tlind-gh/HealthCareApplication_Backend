package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.dto.AppointmentRequest;
import healthcareab.project.healthcare_booking_app.dto.AppointmentResponse;
import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.Appointment;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.AppointmentStatus;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.AppointmentRepository;
import healthcareab.project.healthcare_booking_app.repositories.AvailabilityRepository;
import healthcareab.project.healthcare_booking_app.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AvailabilityRepository availabilityRepository;
    
    
    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            UserService userService,
            UserRepository userRepository,
            AvailabilityRepository availabilityRepository) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.availabilityRepository = availabilityRepository;
    }
    
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        // role check so only patient can create booking
        User patient = userService.getCurrentUser();
        
        if(!patient.getRoles().contains(Role.PATIENT)) {
            throw new UnauthorizedException("Only patients can book appointments");
        }
        
        // validate provider
        User provider = userRepository.findById(request.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        
        if(!provider.getRoles().contains(Role.PROVIDER)) {
            throw new UnauthorizedException("Only providers can book appointments");
        }
        
        // validate time
        if(!request.getStartTime().isBefore(request.getEndTime())) {
            throw new UnauthorizedException("Start time must be before end time");
        }
        
        boolean available = availabilityService.isTimeAvailable(
                request.getProviderId(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        
        if (!available) {
            throw new IllegalArgumentException("Selected time is not available");
        }
   
        // get availability slot and make as booked
        Availability availability = availabilityService.getAvailableSlot(
                request.getProviderId(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        
        availability.setIsAvailable(false);
        availabilityRepository.save(availability);
        
        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.getId());
        appointment.setProviderId(request.getProviderId());
        appointment.setDate(request.getDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setStatus(AppointmentStatus.BOOKED);
        
        
        Appointment savedAppointment = appointmentRepository.save(appointment);
        
        return mapToResponse(savedAppointment);
    }
    
    private AppointmentResponse mapToResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getProviderId(),
                appointment.getDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
        );
    }
}