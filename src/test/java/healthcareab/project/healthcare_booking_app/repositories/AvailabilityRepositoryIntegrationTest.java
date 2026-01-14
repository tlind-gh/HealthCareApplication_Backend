package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.models.Availability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataMongoTest
class AvailabilityRepositoryIntegrationTest {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    private Availability availability1;
    private Availability availability2;
    private Availability availabilityOutsideRange;

    @BeforeEach
    void setUp() {
        availabilityRepository.deleteAll();

        availability1 = new Availability();
        availability1.setProviderId("provider-1");
        availability1.setDate(LocalDate.of(2026, 1, 10));
        availability1.setStartTime(LocalTime.of(8, 0));
        availability1.setEndTime(LocalTime.of(9, 0));
        availability1.setIsAvailable(true);

        availability2 = new Availability();
        availability2.setProviderId("provider-1");
        availability2.setDate(LocalDate.of(2026, 1, 15));
        availability2.setStartTime(LocalTime.of(8, 0));
        availability2.setEndTime(LocalTime.of(9, 0));
        availability2.setIsAvailable(true);

        availabilityOutsideRange = new Availability();
        availabilityOutsideRange.setProviderId("provider-1");
        availabilityOutsideRange.setDate(LocalDate.of(2026, 2, 1));
        availabilityOutsideRange.setStartTime(LocalTime.of(8, 0));
        availabilityOutsideRange.setEndTime(LocalTime.of(9, 0));
        availabilityOutsideRange.setIsAvailable(true);

        availabilityRepository.saveAll(
                List.of(availability1, availability2, availabilityOutsideRange)
        );
    }

    @Test
    void findByProviderIdAndDateBetween_shouldReturnAvailabilitiesWithinRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        List<Availability> result =
                availabilityRepository.findByProviderIdAndDateBetween(
                        "provider-1",
                        from,
                        to
                );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Availability::getDate)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2026, 1, 10),
                        LocalDate.of(2026, 1, 15)
                );
    }

    @Test
    void findByProviderIdAndDateBetween_shouldReturnEmpty_whenNoMatches() {
        List<Availability> result =
                availabilityRepository.findByProviderIdAndDateBetween(
                        "unknown-provider",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                );

        assertThat(result).isEmpty();
    }

    @Test
    void findByProviderIdAndDateBetween_shouldExcludeOutsideDateRange() {
        List<Availability> result =
                availabilityRepository.findByProviderIdAndDateBetween(
                        "provider-1",
                        LocalDate.of(2026, 1, 20),
                        LocalDate.of(2026, 1, 25)
                );
        assertThat(result).isEmpty();
    }
}

