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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

    @ActiveProfiles("test")
    class AppointmentServiceTest {

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private AvailabilityService availabilityService;

        @Mock
        private UserService userService;

        @Mock
        private UserRepository userRepository;

        @Mock
        private AvailabilityRepository availabilityRepository;

        @InjectMocks
        private AppointmentService appointmentService;

        private User patient;
        private User provider;
        private AppointmentRequest validRequest;
        private Availability availableSlot;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.openMocks(this);

            // Setup patient
            patient = new User("patientUser", "Pass1!", "patient@test.com", "Anna", "Svensson", null);
            patient.setId("patient-id-123");
            patient.setRoles(Set.of(Role.PATIENT));

            // Setup provider
            provider = new User("providerUser", "Pass1!", "provider@test.com", "Dr", "Berg", "Läkare");
            provider.setId("provider-id-456");
            provider.setRoles(Set.of(Role.PROVIDER));

            // Setup valid request
            validRequest = new AppointmentRequest(
                    "provider-id-456",
                    LocalDate.of(2025, 6, 10),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0)
            );

            // Setup available slot
            availableSlot = new Availability();
            availableSlot.setId("slot-id-789");
            availableSlot.setProviderId("provider-id-456");
            availableSlot.setDate(LocalDate.of(2025, 6, 10));
            availableSlot.setStartTime(LocalTime.of(9, 0));
            availableSlot.setEndTime(LocalTime.of(10, 0));
            availableSlot.setIsAvailable(true);
        }

        // -------------------------------------------------------
        // TEST 1: Happy path — patient bokar ett ledigt pass
        // -------------------------------------------------------
        @Test
        void createAppointment_withValidPatientAndAvailableSlot_shouldReturnAppointmentResponse() {
            // Arrange
            when(userService.getCurrentUser()).thenReturn(patient);
            when(userRepository.findById("provider-id-456")).thenReturn(Optional.of(provider));
            when(availabilityService.isTimeAvailable(any(), any(), any(), any())).thenReturn(true);
            when(availabilityService.getAvailableSlot(any(), any(), any(), any())).thenReturn(availableSlot);

            Appointment savedAppointment = new Appointment();
            savedAppointment.setId("appt-id-001");
            savedAppointment.setPatientId("patient-id-123");
            savedAppointment.setProviderId("provider-id-456");
            savedAppointment.setDate(LocalDate.of(2025, 6, 10));
            savedAppointment.setStartTime(LocalTime.of(9, 0));
            savedAppointment.setEndTime(LocalTime.of(10, 0));
            savedAppointment.setStatus(AppointmentStatus.BOOKED);

            when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

            // Act
            AppointmentResponse response = appointmentService.createAppointment(validRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo("patient-id-123");
            assertThat(response.getProviderId()).isEqualTo("provider-id-456");
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.BOOKED);

            verify(availabilityRepository).save(availableSlot);
            verify(appointmentRepository).save(any(Appointment.class));
        }

        // -------------------------------------------------------
        // TEST 2: Fel roll — ADMIN försöker boka → UnauthorizedException
        // -------------------------------------------------------
        @Test
        void createAppointment_withNonPatientRole_shouldThrowUnauthorizedException() {
            // Arrange
            User admin = new User("adminUser", "Pass1!", "admin@test.com", "Admin", "User", null);
            admin.setId("admin-id-999");
            admin.setRoles(Set.of(Role.ADMIN));

            when(userService.getCurrentUser()).thenReturn(admin);

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.createAppointment(validRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Only patients can book appointments");

            verifyNoInteractions(userRepository, availabilityService, appointmentRepository);
        }

        // -------------------------------------------------------
        // TEST 3: Provider finns inte i databasen → IllegalArgumentException
        // -------------------------------------------------------
        @Test
        void createAppointment_withNonExistentProvider_shouldThrowIllegalArgumentException() {
            // Arrange
            when(userService.getCurrentUser()).thenReturn(patient);
            when(userRepository.findById("provider-id-456")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.createAppointment(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Provider not found");

            verifyNoInteractions(availabilityService, appointmentRepository);
        }

        // -------------------------------------------------------
        // TEST 4: Angiven "provider" har inte rollen PROVIDER
        // -------------------------------------------------------
        @Test
        void createAppointment_whenProviderUserLacksProviderRole_shouldThrowUnauthorizedException() {
            // Arrange
            User fakeProvider = new User("fakeProvider", "Pass1!", "fake@test.com", "Fake", "User", null);
            fakeProvider.setId("provider-id-456");
            fakeProvider.setRoles(Set.of(Role.PATIENT)); // fel roll

            when(userService.getCurrentUser()).thenReturn(patient);
            when(userRepository.findById("provider-id-456")).thenReturn(Optional.of(fakeProvider));

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.createAppointment(validRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Only providers can book appointments");

            verifyNoInteractions(availabilityService, appointmentRepository);
        }

        // -------------------------------------------------------
        // TEST 5: Starttid är EFTER sluttid → UnauthorizedException
        // -------------------------------------------------------
        @Test
        void createAppointment_whenStartTimeIsAfterEndTime_shouldThrowUnauthorizedException() {
            // Arrange
            AppointmentRequest badTimeRequest = new AppointmentRequest(
                    "provider-id-456",
                    LocalDate.of(2025, 6, 10),
                    LocalTime.of(11, 0),  // start är efter end
                    LocalTime.of(9, 0)
            );

            when(userService.getCurrentUser()).thenReturn(patient);
            when(userRepository.findById("provider-id-456")).thenReturn(Optional.of(provider));

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.createAppointment(badTimeRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Start time must be before end time");

            verifyNoInteractions(availabilityService, appointmentRepository);
        }

        // -------------------------------------------------------
        // TEST 6: Tidsslot är inte tillgängligt → IllegalArgumentException
        // -------------------------------------------------------
        @Test
        void createAppointment_whenTimeSlotNotAvailable_shouldThrowIllegalArgumentException() {
            // Arrange
            when(userService.getCurrentUser()).thenReturn(patient);
            when(userRepository.findById("provider-id-456")).thenReturn(Optional.of(provider));
            when(availabilityService.isTimeAvailable(any(), any(), any(), any())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.createAppointment(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Selected time is not available");

            verify(appointmentRepository, never()).save(any());
        }

        // -------------------------------------------------------
        // TEST 7: Verifiera att availability
        // -------------------------------------------------------
        @Test
        void createAppointment_shouldMarkAvailabilitySlotAsBooked() {
            // Arrange
            when(userService.getCurrentUser()).thenReturn(patient);
            when(userRepository.findById("provider-id-456")).thenReturn(Optional.of(provider));
            when(availabilityService.isTimeAvailable(any(), any(), any(), any())).thenReturn(true);
            when(availabilityService.getAvailableSlot(any(), any(), any(), any())).thenReturn(availableSlot);

            Appointment savedAppointment = new Appointment();
            savedAppointment.setId("appt-id-001");
            savedAppointment.setPatientId("patient-id-123");
            savedAppointment.setProviderId("provider-id-456");
            savedAppointment.setDate(LocalDate.of(2025, 6, 10));
            savedAppointment.setStartTime(LocalTime.of(9, 0));
            savedAppointment.setEndTime(LocalTime.of(10, 0));
            savedAppointment.setStatus(AppointmentStatus.BOOKED);

            when(appointmentRepository.save(any())).thenReturn(savedAppointment);

            // Act
            appointmentService.createAppointment(validRequest);

            // Assert — slottet ska vara markerat som inte tillgängligt
            assertThat(availableSlot.getIsAvailable()).isFalse();
            verify(availabilityRepository).save(availableSlot);
        }
    }


