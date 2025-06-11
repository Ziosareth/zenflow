package it.zenflow.model.rbac;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // es. "CREATE_USER", "READ_USER", ecc.
    private String description;
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'UNCATEGORIZED'")
    private String category;    // es. "ADMINISTRATION", "PROJECT", ecc.

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

}
