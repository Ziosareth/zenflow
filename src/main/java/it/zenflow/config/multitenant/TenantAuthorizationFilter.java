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

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@Component
public class TenantAuthorizationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String tenantId = TenantContext.getCurrentTenant();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails user = authentication == null ? null : (UserDetails) authentication.getPrincipal();
        String userTenantId = user == null ? null : extractTenantFromAuthorities(user);
        if (user == null) {
            // Utente non autenticato, lascia passare per permettere il login
            chain.doFilter(request, response);
        } else if (Objects.equals(tenantId, userTenantId)) {
            // Utente autenticato e accede al proprio tenant
            chain.doFilter(request, response);
        } else {
            // Utente autenticato che tenta di accedere a un tenant diverso
            log.warn("Attempted cross-tenant access from user {} with tenant {} to tenant {}",
                    user.getUsername(), userTenantId, tenantId);
            response.setStatus(FORBIDDEN.value());
        }
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
