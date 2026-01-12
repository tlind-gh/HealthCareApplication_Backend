package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.dto.RegisterRequest;
import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.NameAlreadyBoundException;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_shouldSaveUser_whenValidRequest() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("TestUsername");
        request.setEmail("test@example.com");
        request.setPassword("TestPassword1234@");
        request.setFirstName("TestFirstName");
        request.setLastName("TestLastName");
        request.setRoles(Set.of(Role.PATIENT));

        when(userRepository.findByUsername("TestUsername")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("TestPassword1234@")).thenReturn("encodedPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        User savedUser = authService.registerUser(request);

        assertThat(savedUser.getUsername()).isEqualTo("TestUsername");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRoles()).containsExactly(Role.PATIENT);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        User user = new User("TestUsername", "encodedPwd", "test@example.com", "Jane", "Doe", null);
        when(userRepository.findByUsername("TestUsername")).thenReturn(Optional.of(user));

        User result = authService.findByUsername("TestUsername");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenUserExists() {
        when(userRepository.findByUsername("TestUsername")).thenReturn(Optional.of(new User()));
        assertThat(authService.existsByUsername("TestUsername")).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));
        assertThat(authService.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void registerUser_shouldThrow_whenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existingUser");
        request.setEmail("new@example.com");
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(NameAlreadyBoundException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void registerUser_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("existing@example.com");
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(NameAlreadyBoundException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void registerUser_shouldThrow_whenNonPersonnelHasProfession() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("Patient");
        request.setEmail("patient@example.com");
        request.setRoles(Set.of(Role.PATIENT));
        request.setProfession("Doctor");

        when(userRepository.findByUsername("Patient")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only personnel can have a profession");
    }

    @Test
    void registerUser_shouldThrow_whenUsernameOrEmailBlank() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("");
        request.setRoles(Set.of(Role.PATIENT));

        when(userRepository.findByUsername("")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username and email cannot be blank");
    }

    @Test
    void findByUsername_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
