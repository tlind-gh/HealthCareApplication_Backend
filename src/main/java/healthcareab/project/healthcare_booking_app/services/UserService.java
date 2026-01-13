package healthcareab.project.healthcare_booking_app.services;

import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.User;
import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;

import healthcareab.project.healthcare_booking_app.repositories.UserAuthRepository;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    private final UserAuthRepository userAuthRepository;
    
    public UserService(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }
    
    public User getCurrentUser() {
        return userAuthRepository.authenticateAndExtractUser();
    }
    
    public String getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    public boolean isCurrentUserAuthenticated() {
        return getCurrentUser().getRoles().contains(Role.PERSONNEL);
    }
    
    public void assertCurrentUserAuthenticated() {
        if (!isCurrentUserAuthenticated()) {
            throw new UnauthorizedException("You are not authenticated");
        }
    }
}