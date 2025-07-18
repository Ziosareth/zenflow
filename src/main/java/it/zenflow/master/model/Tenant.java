package it.zenflow.master.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
