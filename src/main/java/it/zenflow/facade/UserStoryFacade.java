package it.zenflow.facade;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.mapper.UserStoryMapper;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.SprintMetricsService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Facade for the operations related to User Stories.
 * This class encapsulates the business logic for user story management.
 */
@Component
@RequiredArgsConstructor
public class UserStoryFacade {
    private final UserStoryService userStoryService;
    private final ProjectService projectService;
    private final UserService userService;
    private final SprintMetricsService sprintMetricsService;
    private final MessageSource messageSource;
    private final UserStoryMapper userStoryMapper;
    private final it.zenflow.mapper.UserStoryViewMapper userStoryViewMapper;

    /**
     * Retrieves all user stories for a project with pagination
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Page<UserStory> findByProjectPaginated(Project project, Pageable pageable) {
        return userStoryService.findByProjectPaginated(project, pageable);
    }

    /**
     * View-safe: Retrieves user stories as view DTOs with pagination
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Page<it.zenflow.dto.UserStoryViewDTO> findByProjectPaginatedView(Project project, Pageable pageable) {
        return userStoryService.findByProjectPaginated(project, pageable)
                .map(userStoryViewMapper::toViewDto);
    }

    /**
     * Retrieves a specific user story by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<UserStory> findById(Long id) {
        return userStoryService.findById(id);
    }

    /**
     * Retrieves a user story with its tasks
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<UserStory> findByIdWithTasks(Long id) {
        return userStoryService.findByIdWithTasks(id);
    }

    /**
     * View-safe: Retrieves a user story with tasks as a view DTO
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<it.zenflow.dto.UserStoryViewDTO> findByIdWithTasksView(Long id) {
        return userStoryService.findByIdWithTasks(id).map(userStoryViewMapper::toViewDto);
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
     * Retrieves a user by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<User> getUserById(Long id) {
        return userService.findById(id);
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
     * Creates a new user story
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public UserStory createUserStory(UserStoryDTO userStoryDTO, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(userStoryDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
                userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to create user stories for this project");
        }
        
        // Use mapper to convert DTO to entity
        UserStory userStory = userStoryMapper.toEntity(userStoryDTO);
        userStory.setProject(project);

        // Set assigned user if provided
        if (userStoryDTO.getAssignedToId() != null) {
            userService.findById(userStoryDTO.getAssignedToId())
                    .ifPresent(userStory::setAssignedTo);
        }

        return userStoryService.save(userStory);
    }

    /**
     * Updates an existing user story
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public UserStory updateUserStory(Long id, UserStoryDTO userStoryDTO, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(userStoryDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        UserStory userStory = userStoryService.findById(id)
                .orElseThrow(() -> new RuntimeException("User story not found"));
        
        // Check that the user story belongs to the specified project
        if (!userStory.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("User story does not belong to the specified project");
        }
        
        // Check permissions
        if (!isProjectOwner(project, currentUser) && 
            !isTeamMember(project, currentUser) &&
                userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to update user stories for this project");
        }
        
        // Use mapper to update entity from DTO
        userStoryMapper.updateEntityFromDto(userStoryDTO, userStory);

        // Update assigned user if provided
        if (userStoryDTO.getAssignedToId() != null) {
            userService.findById(userStoryDTO.getAssignedToId())
                    .ifPresent(userStory::setAssignedTo);
        } else {
            userStory.setAssignedTo(null);
        }

        UserStory savedUserStory = userStoryService.save(userStory);

        // If the status is DONE and the user story is associated with a sprint,
        // update the sprint's completed points and velocity
        if (userStory.getStatus() == StoryStatus.DONE && userStory.getSprint() != null) {
            sprintMetricsService.updateSprintCompletedPoints(userStory.getSprint().getId());
        }

        return savedUserStory;
    }

    /**
     * Deletes a user story
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteUserStory(Long projectId, Long id, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        UserStory userStory = userStoryService.findById(id)
                .orElseThrow(() -> new RuntimeException("User story not found"));
        
        // Check that the user story belongs to the specified project
        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("User story does not belong to the specified project");
        }
        
        // Check permissions (only owner or admin)
        if (!isProjectOwner(project, currentUser) &&
                userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to delete user stories for this project");
        }
        
        userStoryService.deleteById(id);
    }

    /**
     * Converts a UserStory entity to a DTO
     */
    public UserStoryDTO mapToDTO(UserStory userStory) {
        return userStoryMapper.toDto(userStory);
    }

    /**
     * Gets a localized message
     */
    public String getLocalizedMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}