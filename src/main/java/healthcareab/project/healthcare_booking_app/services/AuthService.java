package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.dto.RegisterRequest;
import healthcareab.project.healthcare_booking_app.exceptions.IllegalArgumentException;
import healthcareab.project.healthcare_booking_app.exceptions.NameAlreadyBoundException;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import healthcareab.project.healthcare_booking_app.repositories.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterRequest registerRequest) {
        if (existsByUsername(registerRequest.getUsername())) {
            throw new NameAlreadyBoundException("Username already exists");
        }

        if (existsByEmail(registerRequest.getEmail())) {
            throw new NameAlreadyBoundException("Email already exists");
        }

        if (!registerRequest.getRoles().contains(Role.PROVIDER) && registerRequest.getProfession() != null) {
            throw new IllegalArgumentException("Only personnel can have a profession");
        }

        if (registerRequest.getUsername().isBlank() || registerRequest.getEmail().isBlank()) {
            throw new IllegalArgumentException("Username and email cannot be blank");
        }

        return userRepository.save(mapRequestToUser(registerRequest));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    private User mapRequestToUser(RegisterRequest registerRequest) {
        User user = new User(
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail(),
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getProfession());
        if (registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            user.setRoles(Set.of(Role.PATIENT));
        } else {
            user.setRoles(registerRequest.getRoles());
        }
        return user;
    }
}
