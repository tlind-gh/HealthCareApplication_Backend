package healthcareab.project.healthcare_booking_app.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRequest {
    private String providerId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    
    public AppointmentRequest(String providerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.providerId = providerId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
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
}