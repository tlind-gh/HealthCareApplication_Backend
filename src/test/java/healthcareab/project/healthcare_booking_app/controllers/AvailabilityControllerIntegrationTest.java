package healthcareab.project.healthcare_booking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import healthcareab.project.healthcare_booking_app.dto.AvailabilityRequest;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.Availability;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.services.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvailabilityControllerIntegrationTest {

    @InjectMocks
    private AvailabilityController availabilityController;

    @Mock
    private AvailabilityService availabilityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User provider;
    private User patient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        availabilityService = mock(AvailabilityService.class);
        availabilityController = new AvailabilityController(availabilityService);

        mockMvc = MockMvcBuilders.standaloneSetup(availabilityController)
                .setControllerAdvice(new healthcareab.project.healthcare_booking_app.exceptions.GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SecurityContextHolder.clearContext();

        provider = new User("ProviderUser", "encoded", "provider@example.com", "First", "Last", null);
        provider.setRoles(Set.of(Role.PROVIDER));
        provider.setId("provider-id");

        patient = new User("PatientUser", "encoded", "patient@example.com", "First", "Last", null);
        patient.setRoles(Set.of(Role.PATIENT));
        patient.setId("patient-id");
    }

    // ------------------------------
    // Helper: mock authentication
    // ------------------------------
    private void mockAuthenticatedUser(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(r -> "ROLE_" + r.name()).toArray(String[]::new))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private AvailabilityRequest validRequest() {
        AvailabilityRequest request = new AvailabilityRequest();
        request.setDate(LocalDate.of(2026, 1, 15));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        return request;
    }

    private Availability validAvailability() {
        Availability availability = new Availability();
        availability.setId("avail-1");
        availability.setProviderId(provider.getId());
        availability.setDate(LocalDate.of(2026, 1, 15));
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(10, 0));
        availability.setIsAvailable(true);
        return availability;
    }

    // =====================================================
    // CREATE AVAILABILITY
    // =====================================================

    @Test
    void createAvailability_shouldReturnCreated() throws Exception {
        mockAuthenticatedUser(provider);

        AvailabilityRequest request = validRequest();
        Availability availability = validAvailability();

        when(availabilityService.createAvailability(request.getDate(), request.getStartTime(), request.getEndTime()))
                .thenReturn(availability);

        mockMvc.perform(post("/availability/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("avail-1"))
                .andExpect(jsonPath("$.providerId").value("provider-id"));
    }

    @Test
    void createAvailability_shouldReturnUnauthorized_whenPatientTries() throws Exception {
        mockAuthenticatedUser(patient);

        AvailabilityRequest request = validRequest();

        when(availabilityService.createAvailability(any(), any(), any()))
                .thenThrow(new UnauthorizedException("You are not authorized"));

        mockMvc.perform(post("/availability/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("You are not authorized"));
    }

    // =====================================================
    // GET AVAILABILITY
    // =====================================================

    @Test
    void getAvailability_shouldReturnOk() throws Exception {
        mockAuthenticatedUser(provider);

        Availability availability = validAvailability();

        when(availabilityService.getAvailabilitiesForCurrentProvider(availability.getDate(), availability.getDate()))
                .thenReturn(List.of(availability));

        mockMvc.perform(get("/availability/all")
                        .param("from", "2026-01-15")
                        .param("to", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("avail-1"));
    }

    // =====================================================
    // UPDATE AVAILABILITY
    // =====================================================

    @Test
    void updateAvailability_shouldReturnOk() throws Exception {
        mockAuthenticatedUser(provider);

        AvailabilityRequest request = validRequest();
        Availability availability = validAvailability();

        when(availabilityService.updateAvailability("avail-1", request.getDate(), request.getStartTime(), request.getEndTime()))
                .thenReturn(availability);

        mockMvc.perform(put("/availability/{id}", "avail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("avail-1"));
    }

    @Test
    void updateAvailability_shouldReturnUnauthorized_whenPatientTries() throws Exception {
        mockAuthenticatedUser(patient);

        AvailabilityRequest request = validRequest();

        when(availabilityService.updateAvailability(any(), any(), any(), any()))
                .thenThrow(new UnauthorizedException("You are not authorized"));

        mockMvc.perform(put("/availability/{id}", "avail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("You are not authorized"));
    }

    // =====================================================
    // DELETE AVAILABILITY
    // =====================================================

    @Test
    void deleteAvailability_shouldReturnNoContent() throws Exception {
        mockAuthenticatedUser(provider);

        doNothing().when(availabilityService).deleteAvailability("avail-1");

        mockMvc.perform(delete("/availability/{id}", "avail-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAvailability_shouldReturnUnauthorized_whenPatientTries() throws Exception {
        mockAuthenticatedUser(patient);

        doThrow(new UnauthorizedException("You are not authorized"))
                .when(availabilityService).deleteAvailability("avail-1");

        mockMvc.perform(delete("/availability/{id}", "avail-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("You are not authorized"));
    }
}
