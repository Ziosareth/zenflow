package it.zenflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PasswordResetRequestForm {
    @NotEmpty(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}