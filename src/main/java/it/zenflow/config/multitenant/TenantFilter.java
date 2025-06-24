package it.zenflow.config.multitenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-TenantID";
    private static final String TENANT_QUERY_PARAM = "tenant";
    private static final Pattern PATH_TENANT_PATTERN = Pattern.compile("^/api/v\\d+/([^/]+)/.*");

    public TenantFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        String tenantName = extractTenantName(req);
        TenantContext.setCurrentTenant(tenantName);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.setCurrentTenant("");
        }
    }

    private String extractTenantName(HttpServletRequest request) {
        // Priority 1: Header X-TenantID
        String tenantName = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(tenantName)) {
            return tenantName;
        }

        // Priority 2: Query parameter 'tenant'
        tenantName = request.getParameter(TENANT_QUERY_PARAM);
        if (StringUtils.hasText(tenantName)) {
            return tenantName;
        }

        // Priority 3: Path segment (e.g., /api/v1/{tenant}/...)
        tenantName = extractTenantFromPath(request.getRequestURI());
        if (StringUtils.hasText(tenantName)) {
            return tenantName;
        }

        // Priority 4: Domain/subdomain (e.g., tenant1.example.com)
        tenantName = extractTenantFromDomain(request.getServerName());
        if (StringUtils.hasText(tenantName)) {
            return tenantName;
        }

        // Return null if no tenant found (will use default tenant)
        return null;
    }

    private String extractTenantFromPath(String requestURI) {
        if (requestURI == null) {
            return null;
        }

        Matcher matcher = PATH_TENANT_PATTERN.matcher(requestURI);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractTenantFromDomain(String serverName) {
        if (serverName == null) {
            return null;
        }

        // Extract subdomain as tenant (e.g., tenant1.example.com -> tenant1)
        String[] parts = serverName.split("\\.");
        if (parts.length > 2) {
            String subdomain = parts[0];
            return subdomain;
        }

        return null;
    }
}
