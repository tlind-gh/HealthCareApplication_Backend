package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.models.Appointment;
import healthcareab.project.healthcare_booking_app.models.supportClasses.AppointmentStatus;
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
class AppointmentRepositoryIntegrationTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    private static final String PROVIDER_ID = "provider-1";
    private static final String PATIENT_ID = "patient-1";
    private static final LocalDate DATE = LocalDate.of(2026, 1, 20);

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();

        Appointment appointment = new Appointment();
        appointment.setPatientId(PATIENT_ID);
        appointment.setProviderId(PROVIDER_ID);
        appointment.setDate(DATE);
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(17, 0));
        appointment.setStatus(AppointmentStatus.APPROVED);

        appointmentRepository.save(appointment);
    }

    // -------------------- FIND METHODS --------------------

    @Test
    void findByProviderId_shouldReturnAppointments() {
        List<Appointment> results =
                appointmentRepository.findByProviderId(PROVIDER_ID);

        assertThat(results).hasSize(1);
    }

    @Test
    void findByProviderId_shouldReturnEmptyList_whenProviderNotFound() {
        List<Appointment> results =
                appointmentRepository.findByProviderId("unknown-provider");

        assertThat(results).isEmpty();
    }

    @Test
    void findByPatientId_shouldReturnAppointments() {
        List<Appointment> results =
                appointmentRepository.findByPatientId(PATIENT_ID);

        assertThat(results).hasSize(1);
    }

    @Test
    void findByPatientId_shouldReturnEmptyList_whenPatientNotFound() {
        List<Appointment> results =
                appointmentRepository.findByPatientId("unknown-patient");

        assertThat(results).isEmpty();
    }

}
