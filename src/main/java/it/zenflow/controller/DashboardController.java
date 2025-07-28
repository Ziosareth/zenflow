package it.zenflow.controller;

import it.zenflow.facade.DashboardFacade;
import it.zenflow.model.rbac.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Controller for the dashboard page.
 * Displays a comprehensive view of a user's work, including assigned user stories,
 * projects they're involved in, and active sprints.
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardFacade dashboardFacade;

    /**
     * Displays the dashboard page with user's projects, assigned user stories, and active sprints.
     *
     * @param model the Spring MVC model
     * @param userDetails the authenticated user details
     * @return the dashboard view name
     */
    @GetMapping("")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        // Get the current user
        User currentUser = dashboardFacade.getCurrentUser(userDetails);
        
        // Get all dashboard data
        Map<String, Object> dashboardData = dashboardFacade.getDashboardData(currentUser);
        
        // Add all data to the model
        model.addAllAttributes(dashboardData);
        
        return "dashboard";
    }
}
