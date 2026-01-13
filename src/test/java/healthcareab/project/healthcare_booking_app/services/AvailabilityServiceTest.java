package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.AvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AvailabilityServiceTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AvailabilityService availabilityService;

    private User providerUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        providerUser = new User("provider", "encodedPassword", "provider@example.com", "John", "Doe", "Doctor");
        providerUser.setId("provider-id-123");
        providerUser.setRoles(Set.of(Role.PROVIDER));
    }

    @Test
    void createAvailability_shouldSaveAvailability_whenValidRequest() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        when(userService.isCurrentUserAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(providerUser);
        
        ArgumentCaptor<Availability> captor = ArgumentCaptor.forClass(Availability.class);
        when(availabilityRepository.save(captor.capture())).thenAnswer(i -> {
            Availability av = i.getArgument(0);
            av.setId("availability-id-123");
            return av;
        });

        // Act
        Availability result = availabilityService.createAvailability(date, startTime, endTime);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProviderId()).isEqualTo("provider-id-123");
        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getStartTime()).isEqualTo(startTime);
        assertThat(result.getEndTime()).isEqualTo(endTime);
        assertThat(result.getIsAvailable()).isTrue();

        Availability savedAvailability = captor.getValue();
        assertThat(savedAvailability.getProviderId()).isEqualTo("provider-id-123");
        assertThat(savedAvailability.getDate()).isEqualTo(date);
        assertThat(savedAvailability.getStartTime()).isEqualTo(startTime);
        assertThat(savedAvailability.getEndTime()).isEqualTo(endTime);
        assertThat(savedAvailability.getIsAvailable()).isTrue();

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(userService, times(1)).getCurrentUser();
        verify(availabilityRepository, times(1)).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldThrow_whenStartTimeIsAfterEndTime() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(11, 0);
        LocalTime endTime = LocalTime.of(9, 0);

        when(userService.isCurrentUserAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(providerUser);

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.createAvailability(date, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(availabilityRepository, never()).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldThrow_whenStartTimeEqualsEndTime() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(10, 0);

        when(userService.isCurrentUserAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(providerUser);

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.createAvailability(date, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(availabilityRepository, never()).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldThrow_whenStartTimeBefore8AM() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(7, 30);
        LocalTime endTime = LocalTime.of(10, 0);

        when(userService.isCurrentUserAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(providerUser);

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.createAvailability(date, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be at or after 08:00");

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(availabilityRepository, never()).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldThrow_whenEndTimeAfter5PM() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        when(userService.isCurrentUserAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(providerUser);

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.createAvailability(date, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be at or before 17:00");

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(availabilityRepository, never()).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldAcceptBoundaryTimes() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        when(userService.isCurrentUserAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(providerUser);
        
        when(availabilityRepository.save(any(Availability.class))).thenAnswer(i -> {
            Availability av = i.getArgument(0);
            av.setId("availability-id-123");
            return av;
        });

        // Act
        Availability result = availabilityService.createAvailability(date, startTime, endTime);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(17, 0));
        verify(availabilityRepository, times(1)).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldThrow_whenUserNotAuthenticated() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        doThrow(new UnauthorizedException("You are not authenticated"))
                .when(userService).assertCurrentUserAuthenticated();

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.createAvailability(date, startTime, endTime))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not authenticated");

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(userService, never()).getCurrentUser();
        verify(availabilityRepository, never()).save(any(Availability.class));
    }

    @Test
    void createAvailability_shouldThrow_whenUserNotProvider() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        User patientUser = new User("patient", "encodedPassword", "patient@example.com", "Jane", "Doe", null);
        patientUser.setId("patient-id-123");
        patientUser.setRoles(Set.of(Role.PATIENT));

        when(userService.isCurrentUserAuthenticated()).thenReturn(false);
        doThrow(new UnauthorizedException("You are not authenticated"))
                .when(userService).assertCurrentUserAuthenticated();

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.createAvailability(date, startTime, endTime))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not authenticated");

        verify(userService, times(1)).assertCurrentUserAuthenticated();
        verify(availabilityRepository, never()).save(any(Availability.class));
    }
}
