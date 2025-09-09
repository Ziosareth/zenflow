package it.zenflow.facade;

import it.zenflow.dto.UserStoryViewDTO;
import it.zenflow.mapper.UserStoryViewMapper;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.SprintService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Facade for dashboard-related operations.
 * Aggregates data from different services to provide a comprehensive view of a user's work.
 */
@Component
@RequiredArgsConstructor
public class DashboardFacade {

    private final ProjectService projectService;
    private final UserStoryService userStoryService;
    private final SprintService sprintService;
    private final UserService userService;
    private final UserStoryViewMapper userStoryViewMapper;

    /**
     * Gets the current user from UserDetails.
     *
     * @param userDetails the authenticated user details
     * @return the User entity
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public User getCurrentUser(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    /**
     * Gets all projects where the user is involved (as owner or team member).
     *
     * @param user the user
     * @return list of projects
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> getUserProjects(User user) {
        return projectService.findUserProjects(user, ProjectStatus.ACTIVE);
    }

    /**
     * Gets all user stories assigned to the user.
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStoryViewDTO> getAssignedUserStories(User user) {
        return userStoryService.findByAssignedTo(user).stream()
                .map(userStoryViewMapper::toViewDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets user stories assigned to the user, grouped by status (String key).
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Map<String, List<UserStoryViewDTO>> getAssignedUserStoriesByStatus(User user) {
        List<UserStoryViewDTO> userStories = getAssignedUserStories(user);
        return userStories.stream()
                .collect(Collectors.groupingBy(UserStoryViewDTO::getStatusName));
    }

    /**
     * Gets active sprints for the user's projects.
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Sprint> getActiveSprintsInUserProjects(User user) {
        List<Project> userProjects = getUserProjects(user);
        List<Sprint> activeSprints = new ArrayList<>();

        for (Project project : userProjects) {
            Optional<Sprint> activeSprint = sprintService.findActiveSprintByProject(project.getId());
            activeSprint.ifPresent(activeSprints::add);
        }

        return activeSprints;
    }

    /**
     * Gets all dashboard data for a user.
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Map<String, Object> getDashboardData(User user) {
        Map<String, Object> dashboardData = new HashMap<>();

        // Get user projects
        List<Project> userProjects = getUserProjects(user);
        dashboardData.put("userProjects", userProjects);

        // Get assigned user stories grouped by status (String keys)
        Map<String, List<UserStoryViewDTO>> storiesByStatus = getAssignedUserStoriesByStatus(user);
        dashboardData.put("storiesByStatus", storiesByStatus);
        
        // Calculate total number of assigned stories
        int totalAssignedStories = storiesByStatus.values().stream()
                .mapToInt(List::size)
                .sum();
        dashboardData.put("totalAssignedStories", totalAssignedStories);

        // Get active sprints in user projects
        List<Sprint> activeSprints = getActiveSprintsInUserProjects(user);
        dashboardData.put("activeSprints", activeSprints);

        return dashboardData;
    }
}