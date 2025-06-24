package it.zenflow.config;

import it.zenflow.config.multitenant.TenantContext;
import it.zenflow.service.rbac.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZenFlowAuthenticationHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        log.debug("User {} successfully authenticated", username);

        userService.findByUsername(username).ifPresent(user -> {
            if (user.isPasswordChangeRequired()) {
                log.debug("User {} needs to change password, redirecting", username);
                try {
                    response.sendRedirect("/password/change");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to redirect to password change page", e);
                }
            } else {
                try {
                    response.sendRedirect("/dashboard");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to redirect to dashboard", e);
                }
            }
        });
    }
}