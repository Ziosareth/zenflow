package it.zenflow.facade;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.StoryStatus;
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
     *
     * @param user the user
     * @return list of user stories
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> getAssignedUserStories(User user) {
        return userStoryService.findByAssignedTo(user);
    }

    /**
     * Gets user stories assigned to the user, grouped by status.
     *
     * @param user the user
     * @return map of user stories grouped by status
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Map<StoryStatus, List<UserStory>> getAssignedUserStoriesByStatus(User user) {
        List<UserStory> userStories = userStoryService.findByAssignedTo(user);
        return userStories.stream()
                .collect(Collectors.groupingBy(UserStory::getStatus));
    }

    /**
     * Gets active sprints for the user's projects.
     *
     * @param user the user
     * @return list of active sprints
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
     *
     * @param user the user
     * @return map containing all dashboard data
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Map<String, Object> getDashboardData(User user) {
        Map<String, Object> dashboardData = new HashMap<>();

        // Get user projects
        List<Project> userProjects = getUserProjects(user);
        dashboardData.put("userProjects", userProjects);

        // Compute presentation attributes for projects (move logic out of the view)
        Map<Long, String> projectStatusClassById = userProjects.stream()
                .collect(Collectors.toMap(
                        Project::getId,
                        p -> p.getStatus() == ProjectStatus.ACTIVE ? "bg-success" : "bg-warning"
                ));
        dashboardData.put("projectStatusClassById", projectStatusClassById);

        // Get assigned user stories grouped by status
        Map<StoryStatus, List<UserStory>> storiesByStatus = getAssignedUserStoriesByStatus(user);
        dashboardData.put("storiesByStatus", storiesByStatus);

        // Also provide pre-split lists to avoid enum usage in the view
        List<UserStory> backlogStories = Optional.ofNullable(storiesByStatus.get(StoryStatus.BACKLOG)).orElseGet(List::of);
        List<UserStory> inProgressStories = Optional.ofNullable(storiesByStatus.get(StoryStatus.IN_PROGRESS)).orElseGet(List::of);
        List<UserStory> doneStories = Optional.ofNullable(storiesByStatus.get(StoryStatus.DONE)).orElseGet(List::of);
        dashboardData.put("backlogStories", backlogStories);
        dashboardData.put("inProgressStories", inProgressStories);
        dashboardData.put("doneStories", doneStories);
        
        // Calculate total number of assigned stories
        int totalAssignedStories = backlogStories.size() + inProgressStories.size() + doneStories.size();
        dashboardData.put("totalAssignedStories", totalAssignedStories);

        // Get active sprints in user projects
        List<Sprint> activeSprints = getActiveSprintsInUserProjects(user);
        dashboardData.put("activeSprints", activeSprints);

        return dashboardData;
    }
}