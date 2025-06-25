package it.zenflow.model.master;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {
    @Id
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    private String url;
    private String username;
    private String password;
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'org.postgresql.Driver'")
    private String driver;
    private boolean enabled;
}
