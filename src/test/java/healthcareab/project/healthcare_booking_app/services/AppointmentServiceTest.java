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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;
    private User provider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        patient = new User();
        patient.setId("patient-1");
        patient.setRoles(Set.of(Role.PATIENT));

        provider = new User();
        provider.setId("provider-1");
        provider.setRoles(Set.of(Role.PROVIDER));
    }

    // -------------------- CREATE APPOINTMENT --------------------

    @Test
    void createAppointment_shouldThrow_whenUserIsNotPatient() {
        patient.setRoles(Set.of(Role.PROVIDER));
        when(userService.getCurrentUser()).thenReturn(patient);

        AppointmentRequest request = mock(AppointmentRequest.class);

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only patients");
    }

    @Test
    void createAppointment_shouldThrow_whenProviderNotFound() {
        when(userService.getCurrentUser()).thenReturn(patient);
        when(userRepository.findById("provider-1"))
                .thenReturn(Optional.empty());

        AppointmentRequest request = new AppointmentRequest(
                "provider-1",
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");
    }

    // -------------------- GET APPOINTMENTS CURRENT USER --------------------

    @Test
    void getAppointmentsCurrentUser_shouldReturnPatientAppointments() {
        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(patient);
        when(appointmentRepository.findByPatientId(patient.getId()))
                .thenReturn(List.of(new Appointment()));

        List<AppointmentResponse> responses =
                appointmentService.getAppointmentsCurrentUser();

        assertThat(responses).hasSize(1);
    }

    // -------------------- GET APPOINTMENT BY ID --------------------

    @Test
    void getAppointmentById_shouldReturnAppointment_whenAuthorized() {
        Appointment appointment = new Appointment();
        appointment.setId("a1");
        appointment.setPatientId(patient.getId());

        when(appointmentRepository.findById("a1"))
                .thenReturn(Optional.of(appointment));
        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(patient);

        AppointmentResponse response =
                appointmentService.getAppointmentById("a1");

        assertThat(response.getId()).isEqualTo("a1");
    }

    @Test
    void getAppointmentById_shouldThrow_whenUnauthorized() {
        Appointment appointment = new Appointment();
        appointment.setPatientId("someone-else");
        appointment.setProviderId("someone-else");

        User otherUser = new User();
        otherUser.setId("unauthorized");
        otherUser.setRoles(Set.of(Role.PATIENT));

        when(appointmentRepository.findById("a1"))
                .thenReturn(Optional.of(appointment));
        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(otherUser);

        assertThatThrownBy(() ->
                appointmentService.getAppointmentById("a1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // -------------------- CANCEL APPOINTMENT --------------------

    @Test
    void cancelAppointment_shouldCancelSuccessfully() {
        Appointment appointment = new Appointment();
        appointment.setId("a1");
        appointment.setPatientId(patient.getId());
        appointment.setProviderId(provider.getId());
        appointment.setDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(11, 0));
        appointment.setStatus(AppointmentStatus.BOOKED);

        Availability slot = new Availability();
        slot.setIsAvailable(false);

        when(appointmentRepository.findById("a1"))
                .thenReturn(Optional.of(appointment));
        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(patient);
        when(availabilityService.getBookedSlot(any(), any(), any(), any()))
                .thenReturn(slot);
        when(appointmentRepository.save(any()))
                .thenReturn(appointment);

        AppointmentResponse response =
                appointmentService.cancelAppointment("a1");

        assertThat(response.getStatus())
                .isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.getIsAvailable()).isTrue();
    }

    @Test
    void cancelAppointment_shouldThrow_whenAlreadyCancelled() {
        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.getId());
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById("a1"))
                .thenReturn(Optional.of(appointment));
        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(patient);

        assertThatThrownBy(() ->
                appointmentService.cancelAppointment("a1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -------------------- GET ALL APPOINTMENTS --------------------

    @Test
    void getAllAppointments_shouldReturnAppointments_whenAdmin() {
        User admin = new User();
        admin.setRoles(Set.of(Role.ADMIN));

        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(admin);
        when(appointmentRepository.findAll())
                .thenReturn(List.of(new Appointment()));

        List<AppointmentResponse> responses =
                appointmentService.getAllAppointments();

        assertThat(responses).hasSize(1);
    }

    @Test
    void getAllAppointments_shouldThrow_whenNotAdmin() {
        when(userAuthRepository.authenticateAndExtractUser())
                .thenReturn(patient);

        assertThatThrownBy(() ->
                appointmentService.getAllAppointments())
                .isInstanceOf(UnauthorizedException.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(
                appointmentRepository,
                availabilityService,
                availabilityRepository,
                userService,
                userRepository,
                userAuthRepository
        );
    }
}
