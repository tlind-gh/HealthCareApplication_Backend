package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataMongoTest
class UserAuthRepositoryIntegrationTest {

    @Autowired
    private UserAuthRepository userAuthRepository;

    private User user;

    @BeforeEach
    void setUp() {
        userAuthRepository.deleteAll();

        user = new User();
        user.setUsername("testuser");
        user.setPassword("Password123@");
        user.setEmail("test@example.com");
        user.setRoles(Set.of(Role.PATIENT));

        userAuthRepository.save(user);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticateAndExtractUser_shouldReturnUser_whenAuthenticated() {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "testuser",
                        "Password123@",
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User result = userAuthRepository.authenticateAndExtractUser();

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void authenticateAndExtractUser_shouldThrowUnauthorized_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userAuthRepository.authenticateAndExtractUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User is not logged in");
    }

    @Test
    void authenticateAndExtractUser_shouldThrowException_whenUserNotFound() {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "missingUser",
                        "Password123@",
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> userAuthRepository.authenticateAndExtractUser())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}

