package healthcareab.project.healthcare_booking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import healthcareab.project.healthcare_booking_app.dto.AppointmentRequest;
import healthcareab.project.healthcare_booking_app.dto.AppointmentResponse;
import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.AppointmentStatus;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.services.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
class AppointmentControllerIntegrationTest {

    @InjectMocks
    private AppointmentController appointmentController;

    @Mock
    private AppointmentService appointmentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User patient;
    private User provider;
    private User admin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        appointmentService = mock(AppointmentService.class);
        appointmentController = new AppointmentController(appointmentService);

        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new healthcareab.project.healthcare_booking_app.exceptions.GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SecurityContextHolder.clearContext();

        patient = new User("PatientUser", "encoded", "patient@example.com", "First", "Last", null);
        patient.setRoles(Set.of(Role.PATIENT));
        patient.setId("patient-id");

        provider = new User("ProviderUser", "encoded", "provider@example.com", "First", "Last", null);
        provider.setRoles(Set.of(Role.PROVIDER));
        provider.setId("provider-id");

        admin = new User("AdminUser", "encoded", "admin@example.com", "First", "Last", null);
        admin.setRoles(Set.of(Role.ADMIN));
        admin.setId("admin-id");
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

    private AppointmentRequest validRequest() {
        return new AppointmentRequest(
                provider.getId(),
                LocalDate.of(2026, 1, 20),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );
    }

    private AppointmentResponse validResponse(String id) {
        return new AppointmentResponse(
                id,
                patient.getId(),
                provider.getId(),
                LocalDate.of(2026, 1, 20),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                AppointmentStatus.BOOKED
        );
    }

    // =====================================================
    // CREATE APPOINTMENT
    // =====================================================
    @Test
    void createAppointment_shouldReturnCreated() throws Exception {
        mockAuthenticatedUser(patient);

        AppointmentRequest request = validRequest();
        AppointmentResponse response = validResponse("appt-1");

        when(appointmentService.createAppointment(any())).thenReturn(response);

        mockMvc.perform(post("/appointment/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("appt-1"))
                .andExpect(jsonPath("$.patientId").value("patient-id"))
                .andExpect(jsonPath("$.providerId").value("provider-id"))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void createAppointment_shouldReturnUnauthorized_whenProviderTries() throws Exception {
        mockAuthenticatedUser(provider);

        AppointmentRequest request = validRequest();

        when(appointmentService.createAppointment(any()))
                .thenThrow(new UnauthorizedException("Only patients can book appointments"));

        mockMvc.perform(post("/appointment/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Only patients can book appointments"));
    }

    // =====================================================
    // GET CURRENT USER APPOINTMENTS
    // =====================================================
    @Test
    void getAppointmentsCurrentUser_shouldReturnOk() throws Exception {
        mockAuthenticatedUser(patient);

        AppointmentResponse response = validResponse("appt-1");

        when(appointmentService.getAppointmentsCurrentUser()).thenReturn(List.of(response));

        mockMvc.perform(get("/appointment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("appt-1"))
                .andExpect(jsonPath("$[0].patientId").value("patient-id"));
    }

    // =====================================================
    // GET APPOINTMENT BY ID
    // =====================================================
    @Test
    void getAppointmentById_shouldReturnOk() throws Exception {
        mockAuthenticatedUser(patient);

        AppointmentResponse response = validResponse("appt-1");

        when(appointmentService.getAppointmentById("appt-1")).thenReturn(response);

        mockMvc.perform(get("/appointment/{id}", "appt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("appt-1"))
                .andExpect(jsonPath("$.patientId").value("patient-id"));
    }

    // =====================================================
    // CANCEL APPOINTMENT
    // =====================================================
    @Test
    void cancelAppointment_shouldReturnOk() throws Exception {
        mockAuthenticatedUser(patient);

        AppointmentResponse response = validResponse("appt-1");

        when(appointmentService.cancelAppointment("appt-1")).thenReturn(response);

        mockMvc.perform(patch("/appointment/cancel/{id}/", "appt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("appt-1"));
    }

    @Test
    void cancelAppointment_shouldReturnUnauthorized_whenPatientNotOwner() throws Exception {
        mockAuthenticatedUser(provider);

        when(appointmentService.cancelAppointment("appt-1"))
                .thenThrow(new UnauthorizedException("User is not the patient or provider for the appointment, nor admin"));

        mockMvc.perform(patch("/appointment/cancel/{id}/", "appt-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("User is not the patient or provider for the appointment, nor admin"));
    }

    // =====================================================
    // GET ALL APPOINTMENTS (ADMIN ONLY)
    // =====================================================
    @Test
    void getAllAppointments_shouldReturnOk_forAdmin() throws Exception {
        mockAuthenticatedUser(admin);

        AppointmentResponse response = validResponse("appt-1");

        when(appointmentService.getAllAppointments()).thenReturn(List.of(response));

        mockMvc.perform(get("/appointment/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("appt-1"));
    }

    @Test
    void getAllAppointments_shouldReturnUnauthorized_forNonAdmin() throws Exception {
        mockAuthenticatedUser(patient);

        when(appointmentService.getAllAppointments())
                .thenThrow(new UnauthorizedException("User must be admin to see all appointments"));

        mockMvc.perform(get("/appointment/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("User must be admin to see all appointments"));
    }
}
