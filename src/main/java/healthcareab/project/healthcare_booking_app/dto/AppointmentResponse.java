package healthcareab.project.healthcare_booking_app.dto;

import healthcareab.project.healthcare_booking_app.models.supportClasses.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentResponse {
    private final String id;
    private final String patientId;
    private final String providerId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final AppointmentStatus status;
    
    public AppointmentResponse(String id, String patientId, String providerId, LocalDate date, LocalTime startTime, LocalTime endTime, AppointmentStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.providerId = providerId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    
    public String getId() {
        return id;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
    
    public AppointmentStatus getStatus() {
        return status;
    }
    
    
}