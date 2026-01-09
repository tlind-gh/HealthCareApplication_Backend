package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("TestName");
    }

    @Test
    void existsByUsername_shouldReturnTrueWhenUserExists() {
        when(userRepository.findByUsername("TestName")).thenReturn(Optional.of(user));

        boolean exists = authService.existsByUsername("TestName");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalseWhenUserDoesNotExist() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        boolean exists = authService.existsByUsername("unknown");

        assertThat(exists).isFalse();
    }
}
