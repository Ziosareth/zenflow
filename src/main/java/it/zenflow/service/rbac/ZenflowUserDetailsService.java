package it.zenflow.service.rbac;

import it.zenflow.config.multitenant.TenantContext;
import it.zenflow.model.rbac.User;
import it.zenflow.model.rbac.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class ZenflowUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Set<SimpleGrantedAuthority> authoritiesSet = user.getRoles().stream()
                .flatMap(role -> {
                    // Aggiungi il ruolo come authority
                    var roleAuthority = new SimpleGrantedAuthority("ROLE_" + role.getName());

                    // Aggiungi i permessi del ruolo come authorities
                    var permissionAuthorities = role.getPermissions().stream()
                            .map(permission -> new SimpleGrantedAuthority(permission.getName()));

                    return Stream.concat(
                            Stream.of(roleAuthority),
                            permissionAuthorities
                    );
                })
                .collect(Collectors.toSet());

        // AGGIUNGI IL TENANT COME AUTHORITY SPECIALE
        authoritiesSet.add(new SimpleGrantedAuthority("TENANT_" + user.getTenant()));

        return authoritiesSet;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Ottieni il tenant corrente dal context
        String currentTenant = TenantContext.getCurrentTenant();

        if (currentTenant == null) {
            throw new UsernameNotFoundException("Tenant context non impostato per l'utente: " + username);
        }

        // Cerca l'utente considerando sia username che tenant
        User user = userRepository.findByUsernameAndTenant(username, currentTenant)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username + " per tenant: " + currentTenant));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(getAuthorities(user))
                .disabled(!user.isEnabled())
                .build();

    }

}
