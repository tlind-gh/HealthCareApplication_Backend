package healthcareab.project.healthcare_booking_app.dto;

import healthcareab.project.healthcare_booking_app.models.Availability;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailabilityResponse {

    private String id;
    private String providerId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAvailable;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(String id, String providerId, LocalDate date,
                                LocalTime startTime, LocalTime endTime, Boolean isAvailable) {
        this.id = id;
        this.providerId = providerId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAvailable = isAvailable;
    }

    public static AvailabilityResponse fromEntity(Availability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getProviderId(),
                availability.getDate(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getIsAvailable()
        );
    }

    public String getId() {
        return id;
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

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}