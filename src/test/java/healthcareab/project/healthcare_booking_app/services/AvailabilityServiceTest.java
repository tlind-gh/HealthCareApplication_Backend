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
import java.util.List;
import java.util.Optional;
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
    private User adminUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        providerUser = new User("provider", "pw", "provider@test.com", "John", "Doe", "Doctor");
        providerUser.setId("provider-id");
        providerUser.setRoles(Set.of(Role.PROVIDER));

        adminUser = new User("admin", "pw", "admin@test.com", "Admin", "User", null);
        adminUser.setId("admin-id");
        adminUser.setRoles(Set.of(Role.ADMIN));
    }

    // ------------------------------------------------------------------
    // CREATE AVAILABILITY (SLOT-BASED)
    // ------------------------------------------------------------------

    @Test
    void createAvailability_shouldCreateHourlySlots_whenValidInput() {
        when(userService.getCurrentUser()).thenReturn(providerUser);

        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0); // 2 slots

        ArgumentCaptor<List<Availability>> captor =
                ArgumentCaptor.forClass(List.class);

        when(availabilityRepository.saveAll(captor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        List<Availability> result =
                availabilityService.createAvailability(date, start, end);

        assertThat(result).hasSize(2);

        Availability first = result.get(0);
        Availability second = result.get(1);

        assertThat(first.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(first.getEndTime()).isEqualTo(LocalTime.of(10, 0));

        assertThat(second.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(second.getEndTime()).isEqualTo(LocalTime.of(11, 0));

        assertThat(first.getProviderId()).isEqualTo("provider-id");
        assertThat(first.getIsAvailable()).isTrue();

        verify(userService).assertCurrentUserAuthenticated();
        verify(availabilityRepository).saveAll(any());
    }

    @Test
    void createAvailability_shouldAcceptBoundaryTimes_andCreateNineSlots() {
        when(userService.getCurrentUser()).thenReturn(providerUser);

        when(availabilityRepository.saveAll(any()))
                .thenAnswer(i -> i.getArgument(0));

        List<Availability> result = availabilityService.createAvailability(
                LocalDate.now(),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );

        assertThat(result).hasSize(9);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.get(8).getEndTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void createAvailability_shouldThrow_whenStartAfterEnd() {
        when(userService.getCurrentUser()).thenReturn(providerUser);

        assertThatThrownBy(() ->
                availabilityService.createAvailability(
                        LocalDate.now(),
                        LocalTime.of(11, 0),
                        LocalTime.of(9, 0)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");

        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void createAvailability_shouldThrow_whenNotFullHour() {
        when(userService.getCurrentUser()).thenReturn(providerUser);

        assertThatThrownBy(() ->
                availabilityService.createAvailability(
                        LocalDate.now(),
                        LocalTime.of(9, 30),
                        LocalTime.of(10, 0)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1-hour blocks");

        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void createAvailability_shouldThrow_whenUserNotAuthenticated() {
        doThrow(new UnauthorizedException("You are not authenticated"))
                .when(userService).assertCurrentUserAuthenticated();

        assertThatThrownBy(() ->
                availabilityService.createAvailability(
                        LocalDate.now(),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not authenticated");

        verify(availabilityRepository, never()).saveAll(any());
    }

    // ------------------------------------------------------------------
    // GET AVAILABILITIES
    // ------------------------------------------------------------------

    @Test
    void getAvailabilitiesForProvider_shouldReturnSortedList() {
        Availability a1 = availability("1", LocalDate.of(2026, 2, 2), LocalTime.of(10, 0));
        Availability a2 = availability("2", LocalDate.of(2026, 2, 1), LocalTime.of(11, 0));
        Availability a3 = availability("3", LocalDate.of(2026, 2, 1), LocalTime.of(9, 0));

        when(availabilityRepository.findByProviderIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(a1, a2, a3));

        List<Availability> result =
                availabilityService.getAvailabilitiesForProvider(
                        "provider-id",
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 2, 3)
                );

        assertThat(result).containsExactly(a3, a2, a1);
    }

    // ------------------------------------------------------------------
    // UPDATE AVAILABILITY
    // ------------------------------------------------------------------

    @Test
    void updateAvailability_shouldUpdate_whenOwner() {
        Availability availability = availability("av-1", LocalDate.now(), LocalTime.of(9, 0));
        availability.setProviderId(providerUser.getId());

        when(userService.getCurrentUser()).thenReturn(providerUser);
        when(availabilityRepository.findById("av-1")).thenReturn(Optional.of(availability));
        when(availabilityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Availability result = availabilityService.updateAvailability(
                "av-1",
                LocalDate.of(2026, 2, 10),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void updateAvailability_shouldThrow_whenNotOwner() {
        Availability availability = availability("av-1", LocalDate.now(), LocalTime.of(9, 0));
        availability.setProviderId("another-provider");

        when(userService.getCurrentUser()).thenReturn(providerUser);
        when(availabilityRepository.findById("av-1")).thenReturn(Optional.of(availability));

        assertThatThrownBy(() ->
                availabilityService.updateAvailability(
                        "av-1",
                        LocalDate.now(),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("own availability");
    }

    // ------------------------------------------------------------------
    // DELETE AVAILABILITY
    // ------------------------------------------------------------------

    @Test
    void deleteAvailability_shouldDelete_whenOwner() {
        Availability availability = availability("av-1", LocalDate.now(), LocalTime.of(9, 0));
        availability.setProviderId(providerUser.getId());

        when(userService.getCurrentUser()).thenReturn(providerUser);
        when(availabilityRepository.findById("av-1")).thenReturn(Optional.of(availability));

        availabilityService.deleteAvailability("av-1");

        verify(availabilityRepository).delete(availability);
    }

    @Test
    void deleteAvailability_shouldDelete_whenAdmin() {
        Availability availability = availability("av-1", LocalDate.now(), LocalTime.of(9, 0));
        availability.setProviderId("provider-id");

        when(userService.getCurrentUser()).thenReturn(adminUser);
        when(availabilityRepository.findById("av-1")).thenReturn(Optional.of(availability));

        availabilityService.deleteAvailability("av-1");

        verify(availabilityRepository).delete(availability);
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private Availability availability(String id, LocalDate date, LocalTime start) {
        Availability availability = new Availability();
        availability.setId(id);
        availability.setDate(date);
        availability.setStartTime(start);
        availability.setEndTime(start.plusHours(1));
        availability.setIsAvailable(true);
        return availability;
    }
}
