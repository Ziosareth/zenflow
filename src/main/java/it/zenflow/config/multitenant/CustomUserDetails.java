package it.zenflow.config.multitenant;

import java.util.Collection;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomUserDetails extends User {
    private final long userId;
    private final String tenantId;

    public CustomUserDetails(String username, String password, long userId, String tenantId,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
        this.tenantId = tenantId;
    }

}
