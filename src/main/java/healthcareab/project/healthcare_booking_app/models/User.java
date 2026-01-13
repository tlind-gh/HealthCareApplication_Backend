package healthcareab.project.healthcare_booking_app.models;

import healthcareab.project.healthcare_booking_app.models.supportClasses.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "Username cannot be empty")
    private String username;

    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()\\-_=+{};:,<.>])(?=.{8,})" + ".*$",
            message = "Password must be at least 8 characters long and contain at least one uppercase letter, one number, and one special character")
    private String password;

    private Set<Role> roles;

    @Indexed(unique = true)
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email does not have a valid format")
    private String email;

    private String firstName;
    private String lastName;
    private String profession;

    public User() {
    }

    public User(String username, String password, String email, String firstName, String lastName, String profession) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profession = profession;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    
    public @NotBlank(message = "Username cannot be empty") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "Username cannot be empty") String username) {
        this.username = username;
    }

    public @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()\\-_=+{};:,<.>])(?=.{8,})" + ".*$",
            message = "Password must be at least 8 characters long and contain at least one uppercase letter, one number, and one special character") String getPassword() {
        return password;
    }

    public void setPassword(@Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()\\-_=+{};:,<.>])(?=.{8,})" + ".*$",
            message = "Password must be at least 8 characters long and contain at least one uppercase letter, one number, and one special character") String password) {
        this.password = password;
    }

    public @NotEmpty(message = "User must have at least one role") Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(@NotEmpty(message = "User must have at least one role") Set<Role> roles) {
        this.roles = roles;
    }

    public @NotBlank(message = "Email cannot be empty") @Email(message = "Email does not have a valid format") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email cannot be empty") @Email(message = "Email does not have a valid format") String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }
}
