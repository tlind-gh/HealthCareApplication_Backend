package healthcareab.project.healthcare_booking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import healthcareab.project.healthcare_booking_app.dto.AvailabilityRequest;
import healthcareab.project.healthcare_booking_app.exceptions.GlobalExceptionHandler;
import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.NotFoundException;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.services.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
class AvailabilityControllerTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AvailabilityController availabilityController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configure ObjectMapper with JavaTimeModule for LocalDate/LocalTime support
        // Disable WRITE_DATES_AS_TIMESTAMPS to serialize dates as ISO strings
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Create a message converter with our configured ObjectMapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        
        // Set up MockMvc with exception handler and custom message converter
        mockMvc = MockMvcBuilders.standaloneSetup(availabilityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    // ---------------------- GET /availability/all ----------------------

    @Test
    void getAvailability_shouldReturnOk_whenValidRequest() throws Exception {
        // Arrange
        String providerId = "provider-id-123";
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        Availability availability1 = createAvailability("avail-1", providerId, LocalDate.of(2024, 1, 15), LocalTime.of(9, 0), LocalTime.of(12, 0));
        Availability availability2 = createAvailability("avail-2", providerId, LocalDate.of(2024, 2, 20), LocalTime.of(10, 0), LocalTime.of(14, 0));
        List<Availability> availabilities = Arrays.asList(availability1, availability2);

        when(availabilityService.getAvailabilitiesForProvider(providerId, from, to))
                .thenReturn(availabilities);

        // Act & Assert
        mockMvc.perform(get("/availability/all")
                        .param("providerId", providerId)
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("avail-1"))
                .andExpect(jsonPath("$[0].providerId").value(providerId))
                .andExpect(jsonPath("$[0].date").value("2024-01-15"))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$[1].id").value("avail-2"))
                .andExpect(jsonPath("$[1].providerId").value(providerId));

        verify(availabilityService, times(1)).getAvailabilitiesForProvider(providerId, from, to);
    }

    @Test
    void getAvailability_shouldReturnEmptyList_whenNoAvailabilityFound() throws Exception {
        // Arrange
        String providerId = "provider-id-123";
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        when(availabilityService.getAvailabilitiesForProvider(providerId, from, to))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/availability/all")
                        .param("providerId", providerId)
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(availabilityService, times(1)).getAvailabilitiesForProvider(providerId, from, to);
    }

    @Test
    void getAvailability_shouldReturnInternalServerError_whenMissingProviderId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/availability/all")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31"))
                .andExpect(status().isInternalServerError());

        verify(availabilityService, never()).getAvailabilitiesForProvider(anyString(), any(), any());
    }

    @Test
    void getAvailability_shouldReturnInternalServerError_whenMissingFrom() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/availability/all")
                        .param("providerId", "provider-id-123")
                        .param("to", "2024-12-31"))
                .andExpect(status().isInternalServerError());

        verify(availabilityService, never()).getAvailabilitiesForProvider(anyString(), any(), any());
    }

    @Test
    void getAvailability_shouldReturnInternalServerError_whenMissingTo() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/availability/all")
                        .param("providerId", "provider-id-123")
                        .param("from", "2024-01-01"))
                .andExpect(status().isInternalServerError());

        verify(availabilityService, never()).getAvailabilitiesForProvider(anyString(), any(), any());
    }

    // ---------------------- PUT /availability/{id} ----------------------

    @Test
    void updateAvailability_shouldReturnOk_whenValidRequest() throws Exception {
        // Arrange
        String availabilityId = "avail-id-123";
        AvailabilityRequest request = new AvailabilityRequest();
        request.setDate(LocalDate.of(2024, 3, 15));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(15, 0));

        Availability updatedAvailability = createAvailability(
                availabilityId, "provider-id-123", 
                LocalDate.of(2024, 3, 15), 
                LocalTime.of(10, 0), 
                LocalTime.of(15, 0)
        );

        when(availabilityService.updateAvailability(
                eq(availabilityId),
                eq(LocalDate.of(2024, 3, 15)),
                eq(LocalTime.of(10, 0)),
                eq(LocalTime.of(15, 0))))
                .thenReturn(updatedAvailability);

        // Act & Assert
        mockMvc.perform(put("/availability/" + availabilityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(availabilityId))
                .andExpect(jsonPath("$.date").value("2024-03-15"))
                .andExpect(jsonPath("$.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.endTime").value("15:00:00"));

        verify(availabilityService, times(1)).updateAvailability(
                eq(availabilityId),
                eq(LocalDate.of(2024, 3, 15)),
                eq(LocalTime.of(10, 0)),
                eq(LocalTime.of(15, 0)));
    }

    @Test
    void updateAvailability_shouldReturnNotFound_whenAvailabilityDoesNotExist() throws Exception {
        // Arrange
        String availabilityId = "non-existent-id";
        AvailabilityRequest request = new AvailabilityRequest();
        request.setDate(LocalDate.of(2024, 3, 15));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(15, 0));

        when(availabilityService.updateAvailability(
                eq(availabilityId),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)))
                .thenThrow(new NotFoundException("Availability not found"));

        // Act & Assert
        mockMvc.perform(put("/availability/" + availabilityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Availability not found"));

        verify(availabilityService, times(1)).updateAvailability(
                eq(availabilityId),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class));
    }

    @Test
    void updateAvailability_shouldReturnUnauthorized_whenNotOwner() throws Exception {
        // Arrange
        String availabilityId = "avail-id-123";
        AvailabilityRequest request = new AvailabilityRequest();
        request.setDate(LocalDate.of(2024, 3, 15));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(15, 0));

        when(availabilityService.updateAvailability(
                eq(availabilityId),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)))
                .thenThrow(new UnauthorizedException("You can only update your own availability"));

        // Act & Assert
        mockMvc.perform(put("/availability/" + availabilityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("You can only update your own availability"));

        verify(availabilityService, times(1)).updateAvailability(
                eq(availabilityId),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class));
    }

    @Test
    void updateAvailability_shouldReturnBadRequest_whenInvalidTimeRange() throws Exception {
        // Arrange
        String availabilityId = "avail-id-123";
        AvailabilityRequest request = new AvailabilityRequest();
        request.setDate(LocalDate.of(2024, 3, 15));
        request.setStartTime(LocalTime.of(15, 0));
        request.setEndTime(LocalTime.of(10, 0)); // End before start

        when(availabilityService.updateAvailability(
                eq(availabilityId),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)))
                .thenThrow(new IllegalArgumentException("Start time must be before end time"));

        // Act & Assert
        mockMvc.perform(put("/availability/" + availabilityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Start time must be before end time"));

        verify(availabilityService, times(1)).updateAvailability(
                eq(availabilityId),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class));
    }

    @Test
    void updateAvailability_shouldReturnInternalServerError_whenMissingRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/availability/avail-id-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(availabilityService, never()).updateAvailability(anyString(), any(), any(), any());
    }

    @Test
    void updateAvailability_shouldReturnInternalServerError_whenInvalidRequestBody() throws Exception {
        // Arrange
        String invalidJson = "{ \"date\": \"invalid-date\", \"startTime\": \"invalid-time\" }";

        // Act & Assert
        mockMvc.perform(put("/availability/avail-id-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isInternalServerError());

        verify(availabilityService, never()).updateAvailability(anyString(), any(), any(), any());
    }

    // ---------------------- DELETE /availability/{id} ----------------------

    @Test
    void deleteAvailability_shouldReturnNoContent_whenValidRequest() throws Exception {
        // Arrange
        String availabilityId = "avail-id-123";
        doNothing().when(availabilityService).deleteAvailability(availabilityId);

        // Act & Assert
        mockMvc.perform(delete("/availability/" + availabilityId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").value("Availability deleted successfully"));

        verify(availabilityService, times(1)).deleteAvailability(availabilityId);
    }

    @Test
    void deleteAvailability_shouldReturnNotFound_whenAvailabilityDoesNotExist() throws Exception {
        // Arrange
        String availabilityId = "non-existent-id";
        doThrow(new NotFoundException("Availability not found"))
                .when(availabilityService).deleteAvailability(availabilityId);

        // Act & Assert
        mockMvc.perform(delete("/availability/" + availabilityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Availability not found"));

        verify(availabilityService, times(1)).deleteAvailability(availabilityId);
    }

    @Test
    void deleteAvailability_shouldReturnUnauthorized_whenNotOwnerAndNotAdmin() throws Exception {
        // Arrange
        String availabilityId = "avail-id-123";
        doThrow(new UnauthorizedException("You can only delete your own availability"))
                .when(availabilityService).deleteAvailability(availabilityId);

        // Act & Assert
        mockMvc.perform(delete("/availability/" + availabilityId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("You can only delete your own availability"));

        verify(availabilityService, times(1)).deleteAvailability(availabilityId);
    }

    // ---------------------- Helper Methods ----------------------

    private Availability createAvailability(String id, String providerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Availability availability = new Availability();
        availability.setId(id);
        availability.setProviderId(providerId);
        availability.setDate(date);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setIsAvailable(true);
        return availability;
    }
}
