package it.zenflow.facade;

import it.zenflow.dto.SprintDTO;
import it.zenflow.mapper.SprintMapper;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.SprintMetricsService;
import it.zenflow.service.SprintService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Facade for Sprint-related business operations.
 * This class encapsulates the business logic for sprint management.
 */
@Component
@RequiredArgsConstructor
public class SprintFacade {
    private final SprintService sprintService;
    private final ProjectService projectService;
    private final UserStoryService userStoryService;
    private final UserService userService;
    private final SprintMetricsService sprintMetricsService;
    private final MessageSource messageSource;
    private final SprintMapper sprintMapper;

    /**
     * Retrieves all sprints for a project
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Sprint> findByProject(Project project) {
        return sprintService.findByProject(project);
    }

    /**
     * Retrieves a specific sprint by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Sprint> findById(Long id) {
        return sprintService.findById(id);
    }

    /**
     * Retrieves a sprint with its user stories
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Sprint> findByIdWithStories(Long id) {
        return sprintService.findByIdWithStories(id);
    }

    /**
     * Retrieves a project by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Project> getProjectById(Long id) {
        return projectService.findById(id);
    }

    /**
     * Retrieves a user by username
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<User> getUserByUsername(String username) {
        return userService.findByUsername(username);
    }

    /**
     * Checks if the user is the owner of the project
     */
    public boolean isProjectOwner(Project project, User user) {
        return project.getOwner() != null && 
               project.getOwner().getId() != null && 
               project.getOwner().getId().equals(user.getId());
    }

    /**
     * Checks if the user is a team member of the project
     */
    public boolean isTeamMember(Project project, User user) {
        return project.getTeamMembers() != null && 
               project.getTeamMembers().contains(user);
    }

    /**
     * Checks if the user has admin rights for the project
     */
    public boolean hasAdminRights(Project project, User user, UserDetails userDetails) {
        return isProjectOwner(project, user) || 
               userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    /**
     * Checks if the project is of type SCRUM
     */
    public boolean isProjectScrum(Project project) {
        return project.getType() != null && project.getType() == ProjectType.SCRUM;
    }

    /**
     * Retrieves user stories not assigned to any sprint
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findUnassignedUserStories(Project project) {
        return userStoryService.findUnassignedUserStories(project);
    }

    /**
     * Checks if there are sprints with a specific status
     */
    public boolean hasSprintsWithStatus(List<Sprint> sprints, SprintStatus status) {
        return sprints.stream().anyMatch(s -> s.getStatus() == status);
    }

    /**
     * Creates a new sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Sprint createSprint(SprintDTO sprintDTO, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(sprintDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check that the project is of type SCRUM
        if (!isProjectScrum(project)) {
            throw new IllegalArgumentException(getLocalizedMessage("sprint.only_for_scrum"));
        }
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to create sprints for this project");
        }
        
        // Use mapper to convert DTO to entity
        Sprint sprint = sprintMapper.toEntity(sprintDTO);
        sprint.setStatus(SprintStatus.PLANNED);
        sprint.setProject(project);

        return sprintService.save(sprint);
    }

    /**
     * Updates an existing sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Sprint updateSprint(Long id, SprintDTO sprintDTO, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(sprintDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        Sprint sprint = sprintService.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));
        
        // Check that the sprint belongs to the specified project
        if (!sprint.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Sprint does not belong to the specified project");
        }
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to update sprints for this project");
        }
        
        // Use mapper to update entity from DTO
        sprintMapper.updateEntityFromDto(sprintDTO, sprint);

        return sprintService.save(sprint);
    }

    /**
     * Starts a sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void startSprint(Long projectId, Long sprintId, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to start sprints for this project");
        }
        
        sprintService.startSprint(sprintId);
    }

    /**
     * Completes a sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void completeSprint(Long projectId, Long sprintId, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to complete sprints for this project");
        }
        
        sprintService.completeSprint(sprintId);
    }

    /**
     * Cancels a sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void cancelSprint(Long projectId, Long sprintId, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to cancel sprints for this project");
        }
        
        sprintService.cancelSprint(sprintId);
    }

    /**
     * Adds a user story to a sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void addUserStoryToSprint(Long projectId, Long sprintId, Long storyId, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to modify sprints for this project");
        }
        
        sprintService.addUserStoryToSprint(sprintId, storyId);
    }

    /**
     * Removes a user story from a sprint
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void removeUserStoryFromSprint(Long projectId, Long sprintId, Long storyId, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to modify sprints for this project");
        }
        
        sprintService.removeUserStoryFromSprint(sprintId, storyId);
    }

    /**
     * Converts a Sprint entity to a DTO
     */
    public SprintDTO mapToDTO(Sprint sprint) {
        return sprintMapper.toDto(sprint);
    }

    /**
     * Gets a localized message
     */
    public String getLocalizedMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}