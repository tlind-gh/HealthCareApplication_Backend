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

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("Password1!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRoles(Set.of(Role.PATIENT));

        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password1!")).thenReturn("encodedPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        // Act
        User savedUser = authService.registerUser(request);

        // Assert
        assertThat(savedUser.getUsername()).isEqualTo("john");
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRoles()).containsExactly(Role.PATIENT);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        // Arrange
        User user = new User("jane", "encodedPwd", "jane@example.com", "Jane", "Doe", null);
        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(user));

        // Act
        User result = authService.findByUsername("jane");

        // Assert
        assertThat(result).isEqualTo(user);
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenUserExists() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));
        assertThat(authService.existsByUsername("john")).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new User()));
        assertThat(authService.existsByEmail("john@example.com")).isTrue();
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
        request.setUsername("user");
        request.setEmail("user@example.com");
        request.setRoles(Set.of(Role.PATIENT));
        request.setProfession("Doctor");

        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

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
