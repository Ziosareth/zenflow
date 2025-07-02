package it.zenflow.master.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTenantDTO {
    @NotBlank(message = "Il nome è obbligatorio")
    private String name;

    @NotBlank(message = "L'URL è obbligatorio")
    private String url;

    @NotBlank(message = "Il nome utente è obbligatorio")
    private String username;

    @NotBlank(message = "La password è obbligatoria")
    private String password;

    private String driver = "org.postgresql.Driver";

    private boolean enabled = false;

    @Email(message = "Formato email non valido")
    private String adminEmail; // Email dell'amministratore del tenant
}
