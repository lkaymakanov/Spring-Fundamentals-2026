package app.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegisterRequest {
    @Size(min = 6, message = "Username must be at least 6 characters")
    private String username;
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @Email(message = "Please provide a valid email address")
    private String email;

}
