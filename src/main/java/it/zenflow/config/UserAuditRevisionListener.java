package it.zenflow.config;

import it.zenflow.model.audit.UserRevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class UserAuditRevisionListener implements RevisionListener {
    
    @Override
    public void newRevision(Object revisionEntity) {
        UserRevisionEntity customRevision = (UserRevisionEntity) revisionEntity;
        
        // Ottieni l'username dall'autenticazione Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            customRevision.setUsername(authentication.getName());
        } else {
            customRevision.setUsername("anonymous");
        }
        
        // Ottieni l'indirizzo IP (opzionale)
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            customRevision.setIpAddress(getClientIpAddress(request));
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}