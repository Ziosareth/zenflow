package it.zenflow.config.multitenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class TenantAuthorizationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestResolvedTenant = TenantContext.getCurrentTenant();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails user = authentication == null ? null : (UserDetails) authentication.getPrincipal();
        String userTenantId = user == null ? null : extractTenantFromAuthorities(user);

        if (user == null) {
            // Utente non autenticato, lascia passare per permettere il login
            chain.doFilter(request, response);
            return;
        }

        // Utente autenticato: imponi sempre il tenant del principal, ignorando header/query/path
        if (userTenantId != null) {
            if (requestResolvedTenant != null && !Objects.equals(requestResolvedTenant, userTenantId)) {
                // Logghiamo il tentativo di "forzare" un tenant diverso, ma non blocchiamo perché imponiamo il tenant corretto
                log.warn("Authenticated user {} attempted to set tenant {} via request while actual tenant is {}. Overriding to user tenant.",
                        user.getUsername(), requestResolvedTenant, userTenantId);
            }
            TenantContext.setCurrentTenant(userTenantId);
        }

        // A questo punto il TenantContext corrisponde al tenant dell'utente, prosegui
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/webjars/")
                || request.getRequestURI().startsWith("/css/")
                || request.getRequestURI().startsWith("/js/")
                || request.getRequestURI().endsWith(".ico")
                || request.getRequestURI().startsWith("/master/")
                || request.getRequestURI().startsWith("/master/**")
                || request.getRequestURI().startsWith("/master")  // Add this line
                ;
    }

    private String extractTenantFromAuthorities(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("TENANT_"))
                .findFirst()
                .map(authority -> authority.getAuthority().substring(7))
                .orElse(null);
    }

}
