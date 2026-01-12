package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

@Service
public class UserService {

    private UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("User is not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (!(principal instanceof CustomUserDetailsService userDetails)) {
            throw new IllegalStateException("Unexpected principal type");
        }
        
        return findById(userDetails.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}