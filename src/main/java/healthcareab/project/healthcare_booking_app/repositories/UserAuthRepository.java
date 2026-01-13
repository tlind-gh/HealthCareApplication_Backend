package healthcareab.project.healthcare_booking_app.repository;

import healthcareab.project.healthcare_booking_app.exceptions.UnauthorizedException;
import healthcareab.project.healthcare_booking_app.models.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.file.AccessDeniedException;

public interface UserAuthRepository extends UserRepository{
    //authenticate and extract current logged-in user, cast error if no user is logged-in or cannot be found in database
    default User authenticateAndExtractUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("User is not logged in.");
        }
        
        //get user id from token via userDetails
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}
