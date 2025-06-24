package it.zenflow.config.multitenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
public class TenantAuthorizationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String tenantId = TenantContext.getCurrentTenant();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var user = authentication == null ? null : (CustomUserDetails) authentication.getPrincipal();
        var userTenantId = user == null ? null : user.getTenantId();

        if (user == null || Objects.equals(tenantId, userTenantId)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Attempted cross-tenant access.");
            response.setStatus(FORBIDDEN.value());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/webjars/")
                || request.getRequestURI().startsWith("/css/")
                || request.getRequestURI().startsWith("/js/")
                || request.getRequestURI().endsWith(".ico");
    }
}
