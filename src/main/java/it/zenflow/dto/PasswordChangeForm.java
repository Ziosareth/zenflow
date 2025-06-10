package it.zenflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeForm {
    @NotEmpty(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String newPassword;
    
    @NotEmpty(message = "Confirm password is required")
    private String confirmPassword;
}