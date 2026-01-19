package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.Appointment;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.AppointmentStatus;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.AppointmentRepository;
import healthcareab.project.healthcare_booking_app.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final UserService userService;
    private final UserRepository userRepository;
    
    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            UserService userService,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.userService = userService;
        this.userRepository = userRepository;
    }
    
    public Appointment createAppointment(String providerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // make sure user that books appointment is a patient
        User patient = userService.getCurrentUser();
        if(!patient.getRoles().contains(Role.PATIENT)) {
            throw new UnauthorizedException("Only patients can book appointments");
        }
        
        // validate provider
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new UnauthorizedException("Provider not found"));
        
        if(!provider.getRoles().contains(Role.PROVIDER)) {
            throw new UnauthorizedException("Selected user is not a provider");
        }
        
        // validate time
        if(startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.getId());
        appointment.setProviderId(providerId);
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.BOOKED);
        return appointmentRepository.save(appointment);
    }
}