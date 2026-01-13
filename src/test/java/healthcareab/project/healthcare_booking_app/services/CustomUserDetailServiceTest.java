package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        // Arrange
        User user = new User("TestUsername", "encodedPassword", "test@example.com", "John", "Doe", null);
        user.setRoles(Set.of(Role.PATIENT, Role.PROVIDER));

        when(userRepository.findByUsername("TestUsername")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("TestUsername");

        // Assert
        assertThat(userDetails.getUsername()).isEqualTo("TestUsername");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");

        Set<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_PATIENT", "ROLE_PERSONNEL");

        verify(userRepository, times(1)).findByUsername("TestUsername");
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findByUsername("unknown");
    }
}
