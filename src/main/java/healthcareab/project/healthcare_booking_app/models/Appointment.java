package healthcareab.project.healthcare_booking_app.models;


import healthcareab.project.healthcare_booking_app.models.supportClasses.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

@Document(collection="appointments")
public class Appointment {
    
    @Id
    private String id;
    
    
    @NotNull(message = "A patient ID is required")
    private String patientId;
    
    @NotNull(message = "A provider ID is required")
    private String providerId;
    
    @NotNull(message = "A date is required")
    private LocalDate date;
    
    @NotNull(message = "A start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "A end time is required")
    private LocalTime endTime;
    
    @NotNull(message = "A appointment status is required")
    private AppointmentStatus status;
    
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
    
    public AppointmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}