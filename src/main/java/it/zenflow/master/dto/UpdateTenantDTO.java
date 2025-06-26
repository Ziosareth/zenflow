package it.zenflow.master.dto;

import lombok.Data;

@Data
public class UpdateTenantDTO {
    private String url;
    private String username;
    private String password;
    private String driver;
    private boolean enabled;
}