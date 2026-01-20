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
import healthcareab.project.healthcare_booking_app.repositories.UserAuthRepository;
import healthcareab.project.healthcare_booking_app.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AvailabilityRepository availabilityRepository;


    private final UserAuthRepository userAuthRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            UserService userService,
            UserRepository userRepository,
            UserAuthRepository userAuthRepository) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
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

    public List<AppointmentResponse> getAppointmentsCurrentUser() {
        User currentUser = userAuthRepository.authenticateAndExtractUser();
        List<Appointment> appointments = new ArrayList<>();
        if (currentUser.getRoles().contains(Role.PATIENT)) {
            appointments = appointmentRepository.findByPatientId(currentUser.getId());
        } else if (currentUser.getRoles().contains(Role.PROVIDER)) {
            appointments = appointmentRepository.findByProviderId(currentUser.getId());
        }
        return appointments.stream().map(this::mapToResponse).toList();
    }

    public AppointmentResponse getAppointmentById(String id) {
        Appointment appointment = getAppointmentFromRepository(id);
        authorizeUserAccessToAppointment(appointment);
        return mapToResponse(appointment);
    }

    public AppointmentResponse cancelAppointment(String id) {
        Appointment appointment = getAppointmentFromRepository(id);
        authorizeUserAccessToAppointment(appointment);
        if (appointment.getStatus().equals(AppointmentStatus.CANCELLED)) {
            throw new UnsupportedOperationException("Appointment has already been cancelled");
        }
        //TODO: add check for last cancellation time (include in other issue).
        appointment.setStatus(AppointmentStatus.CANCELLED);
        //TODO: set availability to isAvailable again!!!
        return mapToResponse(appointmentRepository.save(appointment));
    }

    public List<AppointmentResponse> getAllAppointments() {
        if (!userAuthRepository.authenticateAndExtractUser().getRoles().contains(Role.ADMIN)) {
            throw new UnauthorizedException("User must be admin to see all appointments");
        }
        return appointmentRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    private Appointment getAppointmentFromRepository(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment Not Found"));
    }

    private void authorizeUserAccessToAppointment(Appointment appointment) {
        User currentUser = userAuthRepository.authenticateAndExtractUser();
        if (!currentUser.getId().equals(appointment.getPatientId()) && !currentUser.getId().equals(appointment.getProviderId())
                && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new UnauthorizedException("User is not the patient or provider for the appointment, nor admin");
        }
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