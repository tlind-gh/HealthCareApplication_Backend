package healthcareab.project.healthcare_booking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthcareab.project.healthcare_booking_app.dto.AuthRequest;
import healthcareab.project.healthcare_booking_app.dto.RegisterRequest;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_shouldReturnCreated_whenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("TestUsername");
        request.setEmail("test@example.com");
        request.setPassword("TestPassword1234@");
        request.setRoles(Set.of(Role.PATIENT));
        request.setFirstName("TestFirstName");
        request.setLastName("TestLastName");

        User savedUser = new User("TestUsername", "encodedPassword", "test@example.com", "TestFirstName", "TestLastName", null);
        savedUser.setRoles(Set.of(Role.PATIENT));

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.username").value("TestUsername"))
                .andExpect(jsonPath("$.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    // ---------------------- LOGIN ----------------------

    @Test
    void login_shouldReturnOkAndJwt_whenValidCredentials() throws Exception {

        AuthRequest request = new AuthRequest("TestUsername", "TestPassword1234@");

        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("TestUsername");
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwtToken");

        User user = new User("TestUsername", "encodedPassword", "test@example.com", "TestFirstName", "TestLastName", null);
        user.setRoles(Set.of(Role.PATIENT));
        when(authService.findByUsername("TestUsername")).thenReturn(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("Login successful"))
                .andExpect(jsonPath("$.username").value("TestUsername"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenInvalidCredentials() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest("TestUsername", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Incorrect username or password"));
    }


    @Test
    void logout_shouldReturnOkAndClearJwt() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("jwt=")))
                .andExpect(content().string("Logout successful!"));
    }

    // ---------------------- CHECK AUTHENTICATION ----------------------

    @Test
    void checkAuthentication_shouldReturnUnauthorized_whenNoUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);

        mockMvc.perform(get("/auth/check"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated!"));
    }

    @Test
    void checkAuthentication_shouldReturnOk_whenUserAuthenticated() throws Exception {
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("TestUsername");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        User user = new User("TestUsername", "encodedPassword", "test@example.com", "TestFirstName", "TestLastName", null);
        user.setRoles(Set.of(Role.PATIENT));
        when(authService.findByUsername("TestUsername")).thenReturn(user);

        mockMvc.perform(get("/auth/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("Authenticated"))
                .andExpect(jsonPath("$.username").value("TestUsername"))
                .andExpect(jsonPath("$.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
