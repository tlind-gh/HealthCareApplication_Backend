package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.models.Availability;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilityRepository extends MongoRepository<Availability, String> {
    List<Availability> findByProviderIdAndDateBetween(
            String providerId,
            LocalDate from,
            LocalDate to
    );
}