package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.models.Availability;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AvailabilityRepository extends MongoRepository<Availability, String> {
    List<Availability> findByProviderIdAndDateBetween(
            String providerId,
            LocalDate from,
            LocalDate to
    );
    
    
    /**
     * Returns true if there exists an availability block that fully covers
     * the requested time interval.
     */
    @Query(
            value = """
      {
        'providerId': ?0,
        'date': ?1,
        'startTime': { $lte: ?2 },
        'endTime': { $gte: ?3 }
      }
      """,
            exists = true
    )
    boolean isTimeAvailable(
            String providerId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    );
}