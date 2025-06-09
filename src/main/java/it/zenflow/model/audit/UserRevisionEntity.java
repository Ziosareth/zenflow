package it.zenflow.model.audit;

import it.zenflow.config.UserAuditRevisionListener;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

@Entity
@Table(name = "revinfo")
@RevisionEntity(UserAuditRevisionListener.class)
@Getter
@Setter
public class UserRevisionEntity extends DefaultRevisionEntity {
    
    private String username;
    private String ipAddress;

}