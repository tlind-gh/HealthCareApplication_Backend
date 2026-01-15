package healthcareab.project.healthcare_booking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthcareab.project.healthcare_booking_app.dto.AuthRequest;
import healthcareab.project.healthcare_booking_app.dto.RegisterRequest;
import healthcareab.project.healthcare_booking_app.exceptions.GlobalExceptionHandler;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.services.AuthService;
import healthcareab.project.healthcare_booking_app.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.clearContext();
    }

    // =====================================================
    // REGISTER
    // =====================================================

    @Test
    void register_shouldReturnCreated_whenValidRequest() throws Exception {
        RegisterRequest request = validRegisterRequest();
        User savedUser = validUser();

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("TestUsername"))
                .andExpect(jsonPath("$.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @Test
    void login_shouldReturnOkAndSetJwtCookie_whenValidCredentials() throws Exception {
        AuthRequest request = new AuthRequest("TestUsername", "TestPassword1234@");

        // Mock UserDetails returned by authentication
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("TestUsername")
                .password("encoded")
                .authorities("ROLE_PATIENT")
                .build();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // Mock AuthenticationManager
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Mock JWT generation
        when(jwtUtil.generateToken(userDetails)).thenReturn("fake-jwt-token");

        // Mock service to return User info
        when(authService.findByUsername("TestUsername")).thenReturn(validUser());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=fake-jwt-token")))
                .andExpect(jsonPath("$.jwtToken").value("Login successful"))
                .andExpect(jsonPath("$.username").value("TestUsername"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenInvalidCredentials() throws Exception {
        AuthRequest request = new AuthRequest("TestUsername", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Incorrect username or password"));
    }

    // =====================================================
    // CHECK AUTHENTICATION
    // =====================================================

    @Test
    void checkAuthentication_shouldReturnOk_whenAuthenticated() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("TestUsername")
                .password("encoded")
                .authorities("ROLE_PATIENT")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        when(authService.findByUsername("TestUsername")).thenReturn(validUser());

        mockMvc.perform(get("/auth/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUsername"))
                .andExpect(jsonPath("$.roles[0]").value("PATIENT"));
    }

    @Test
    void checkAuthentication_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/auth/check"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated!"));
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("TestUsername");
        request.setEmail("test@example.com");
        request.setPassword("TestPassword1234@");
        request.setRoles(Set.of(Role.PATIENT));
        request.setFirstName("TestFirstName");
        request.setLastName("TestLastName");
        return request;
    }

    private User validUser() {
        User user = new User(
                "TestUsername",
                "encodedPassword",
                "test@example.com",
                "TestFirstName",
                "TestLastName",
                null
        );
        user.setRoles(Set.of(Role.PATIENT));
        return user;
    }
}
