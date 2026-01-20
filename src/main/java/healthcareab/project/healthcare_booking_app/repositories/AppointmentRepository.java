package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.models.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    /**
     * Checks whether a provider has availability that fully covers
     * the requested appointment time on the given date.
     **/
    @Query("""
{
    'providerId': ?0,
    'date': ?1,
    'startTime': {$lte: ?2},
    'endTime': {$gte: ?3}
}
""")
    boolean isAvailable(String providerId, LocalDate date, LocalTime startTime, LocalTime endTime);
}