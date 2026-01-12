
package healthcareab.project.healthcare_booking_app.repositories;

import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataMongoTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user = new User();
        user.setUsername("TestUsername");
        user.setPassword("TestPassword1234@");
        user.setEmail("test@example.com");
        user.setFirstName("TestFirstName");
        user.setLastName("TestLastName");
        user.setRoles(Set.of(Role.PATIENT));

        userRepository.save(user);
    }

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        Optional<User> result = userRepository.findByUsername("TestUsername");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("TestUsername");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenUserDoesNotExist() {
        Optional<User> result = userRepository.findByUsername("unknownUser");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        // Act
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("TestUsername");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmail("missing@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldFail_whenUsernameIsDuplicate() {
        User duplicateUser = new User();
        duplicateUser.setUsername("TestUsername");
        duplicateUser.setPassword("TestPassword1234@");
        duplicateUser.setEmail("another@example.com");
        duplicateUser.setRoles(Set.of(Role.PATIENT));

        assertThatThrownBy(() -> userRepository.save(duplicateUser))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void save_shouldFail_whenEmailIsDuplicate() {
        User duplicateUser = new User();
        duplicateUser.setUsername("TestAnotherUsername");
        duplicateUser.setPassword("TestPassword1234@");
        duplicateUser.setEmail("test@example.com");
        duplicateUser.setRoles(Set.of(Role.PATIENT));

        assertThatThrownBy(() -> userRepository.save(duplicateUser))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
