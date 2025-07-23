package it.zenflow.facade;

import it.zenflow.dto.ProjectDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Facade for Project-related business operations.
 * This class encapsulates the business logic for project management.
 */
@Component
@RequiredArgsConstructor
public class ProjectFacade {
    private final ProjectService projectService;
    private final UserService userService;

    /**
     * Retrieves all projects with pagination
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Page<Project> getAllProjects(Pageable pageable) {
        return projectService.findAllWithOwners(pageable);
    }

    /**
     * Retrieves a specific project by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Project> getProjectById(Long id) {
        return projectService.findById(id);
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
     * Creates a new project
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Project createProject(ProjectDTO projectDTO, User currentUser) {
        Project project = mapToEntity(projectDTO);
        project.setOwner(currentUser);
        
        // Add team members
        Set<User> teamMembers = getTeamMembersFromIds(projectDTO.getTeamMemberIds());
        project.setTeamMembers(teamMembers);
        
        return projectService.save(project);
    }

    /**
     * Updates an existing project
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Project updateProject(Long id, ProjectDTO projectDTO, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!hasAdminRights(project, currentUser, userDetails)) {
            throw new AccessDeniedException("Not authorized to modify this project");
        }
        
        // Update project fields
        updateEntityFromDTO(project, projectDTO);
        
        // Update team members
        Set<User> teamMembers = getTeamMembersFromIds(projectDTO.getTeamMemberIds());
        project.setTeamMembers(teamMembers);
        
        return projectService.save(project);
    }

    /**
     * Deletes a project
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteProject(Long id, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check permissions
        if (!hasAdminRights(project, currentUser, userDetails)) {
            throw new AccessDeniedException("Not authorized to delete this project");
        }
        
        projectService.deleteById(id);
    }

    /**
     * Retrieves the current user's projects
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> getUserProjects(User currentUser) {
        return projectService.findByOwner(currentUser);
    }
    
    /**
     * Retrieves a user by username
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public User getUserByUsername(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Gets the current authenticated user
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public User getCurrentUser(UserDetails userDetails) {
        return getUserByUsername(userDetails.getUsername());
    }

    /**
     * Retrieves all users
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    /**
     * Converts a DTO to a Project entity
     */
    private Project mapToEntity(ProjectDTO dto) {
        Project project = new Project();
        updateEntityFromDTO(project, dto);
        return project;
    }

    /**
     * Updates a Project entity with data from a DTO
     */
    private void updateEntityFromDTO(Project project, ProjectDTO dto) {
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStatus(dto.getStatus());
        project.setType(dto.getType());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
    }

    /**
     * Converts a Project entity to a DTO
     */
    public ProjectDTO mapToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStatus(project.getStatus());
        dto.setType(project.getType());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        
        // Set team member IDs
        if (project.getTeamMembers() != null) {
            Set<Long> teamMemberIds = project.getTeamMembers().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
            dto.setTeamMemberIds(teamMemberIds);
        }
        
        return dto;
    }

    /**
     * Retrieves User entities from their IDs
     */
    private Set<User> getTeamMembersFromIds(Set<Long> userIds) {
        Set<User> teamMembers = new HashSet<>();
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                userService.findById(userId).ifPresent(teamMembers::add);
            }
        }
        return teamMembers;
    }
}