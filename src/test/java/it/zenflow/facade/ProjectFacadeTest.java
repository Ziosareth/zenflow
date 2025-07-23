package it.zenflow.facade;

import it.zenflow.dto.ProjectDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectFacadeTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectFacade projectFacade;

    private Project project1;
    private Project project2;
    private User ownerUser;
    private User teamMemberUser;
    private User regularUser;
    private UserDetails ownerUserDetails;
    private UserDetails adminUserDetails;
    private ProjectDTO projectDTO;
    private Set<User> teamMembers;

    @BeforeEach
    public void setup() {
        // Create test users
        ownerUser = new User();
        ownerUser.setId(1L);
        ownerUser.setUsername("owner");
        ownerUser.setEmail("owner@example.com");

        teamMemberUser = new User();
        teamMemberUser.setId(2L);
        teamMemberUser.setUsername("teammember");
        teamMemberUser.setEmail("teammember@example.com");

        regularUser = new User();
        regularUser.setId(3L);
        regularUser.setUsername("regular");
        regularUser.setEmail("regular@example.com");

        // Create team members set
        teamMembers = new HashSet<>();
        teamMembers.add(teamMemberUser);

        // Create test projects
        project1 = new Project();
        project1.setId(1L);
        project1.setName("Test Project 1");
        project1.setDescription("This is test project 1");
        project1.setStatus(ProjectStatus.ACTIVE);
        project1.setType(ProjectType.SCRUM);
        project1.setStartDate(LocalDate.now());
        project1.setEndDate(LocalDate.now().plusMonths(3));
        project1.setOwner(ownerUser);
        project1.setTeamMembers(teamMembers);

        project2 = new Project();
        project2.setId(2L);
        project2.setName("Test Project 2");
        project2.setDescription("This is test project 2");
        project2.setStatus(ProjectStatus.ON_HOLD);
        project2.setType(ProjectType.KANBAN);
        project2.setStartDate(LocalDate.now().plusDays(7));
        project2.setEndDate(LocalDate.now().plusMonths(6));
        project2.setOwner(ownerUser);
        project2.setTeamMembers(new HashSet<>());

        // Create test DTO
        projectDTO = new ProjectDTO();
        projectDTO.setName("New Project");
        projectDTO.setDescription("This is a new project");
        projectDTO.setStatus(ProjectStatus.ACTIVE);
        projectDTO.setType(ProjectType.SCRUM);
        projectDTO.setStartDate(LocalDate.now());
        projectDTO.setEndDate(LocalDate.now().plusMonths(3));
        Set<Long> teamMemberIds = new HashSet<>();
        teamMemberIds.add(teamMemberUser.getId());
        projectDTO.setTeamMemberIds(teamMemberIds);

        // Create UserDetails for authentication tests
        ownerUserDetails = createUserDetails(ownerUser.getUsername(), Collections.singletonList("USER"));
        adminUserDetails = createUserDetails("admin", Arrays.asList("USER", "ADMIN"));
    }

    private UserDetails createUserDetails(String username, List<String> roles) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            @Override
            public String getPassword() {
                return "password";
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }

    @Test
    public void testGetAllProjects() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Project> projects = Arrays.asList(project1, project2);
        Page<Project> projectPage = new PageImpl<>(projects, pageable, projects.size());
        when(projectService.findAllWithOwners(pageable)).thenReturn(projectPage);

        // Act
        Page<Project> result = projectFacade.getAllProjects(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).contains(project1, project2);
        verify(projectService, times(1)).findAllWithOwners(pageable);
    }

    @Test
    public void testGetProjectById() {
        // Arrange
        when(projectService.findById(1L)).thenReturn(Optional.of(project1));

        // Act
        Optional<Project> result = projectFacade.getProjectById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(project1);
        verify(projectService, times(1)).findById(1L);
    }

    @Test
    public void testGetUserProjects() {
        // Arrange
        when(projectService.findByOwner(ownerUser)).thenReturn(Arrays.asList(project1, project2));

        // Act
        List<Project> result = projectFacade.getUserProjects(ownerUser);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(project1, project2);
        verify(projectService, times(1)).findByOwner(ownerUser);
    }

    @Test
    public void testIsProjectOwner_WhenUserIsOwner() {
        // Act
        boolean result = projectFacade.isProjectOwner(project1, ownerUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testIsProjectOwner_WhenUserIsNotOwner() {
        // Act
        boolean result = projectFacade.isProjectOwner(project1, regularUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testIsTeamMember_WhenUserIsTeamMember() {
        // Act
        boolean result = projectFacade.isTeamMember(project1, teamMemberUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testIsTeamMember_WhenUserIsNotTeamMember() {
        // Act
        boolean result = projectFacade.isTeamMember(project1, regularUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testHasAdminRights_WhenUserIsOwner() {
        // Act
        boolean result = projectFacade.hasAdminRights(project1, ownerUser, ownerUserDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testHasAdminRights_WhenUserIsAdmin() {
        // Act
        boolean result = projectFacade.hasAdminRights(project1, regularUser, adminUserDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testHasAdminRights_WhenUserIsNotOwnerOrAdmin() {
        // Act
        boolean result = projectFacade.hasAdminRights(project1, regularUser, ownerUserDetails);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testCreateProject() {
        // Arrange
        Project newProject = new Project();
        newProject.setId(3L);
        newProject.setName(projectDTO.getName());
        newProject.setDescription(projectDTO.getDescription());
        newProject.setStatus(projectDTO.getStatus());
        newProject.setType(projectDTO.getType());
        newProject.setStartDate(projectDTO.getStartDate());
        newProject.setEndDate(projectDTO.getEndDate());
        newProject.setOwner(ownerUser);
        newProject.setTeamMembers(teamMembers);

        when(userService.findById(teamMemberUser.getId())).thenReturn(Optional.of(teamMemberUser));
        when(projectService.save(any(Project.class))).thenReturn(newProject);

        // Act
        Project result = projectFacade.createProject(projectDTO, ownerUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(projectDTO.getName());
        assertThat(result.getDescription()).isEqualTo(projectDTO.getDescription());
        assertThat(result.getStatus()).isEqualTo(projectDTO.getStatus());
        assertThat(result.getType()).isEqualTo(projectDTO.getType());
        assertThat(result.getStartDate()).isEqualTo(projectDTO.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(projectDTO.getEndDate());
        assertThat(result.getOwner()).isEqualTo(ownerUser);
        assertThat(result.getTeamMembers()).containsExactly(teamMemberUser);
        
        verify(userService, times(1)).findById(teamMemberUser.getId());
        verify(projectService, times(1)).save(any(Project.class));
    }

    @Test
    public void testUpdateProject_Success() {
        // Arrange
        ProjectDTO updateDTO = new ProjectDTO();
        updateDTO.setName("Updated Project");
        updateDTO.setDescription("This is an updated project");
        updateDTO.setStatus(ProjectStatus.COMPLETED);
        updateDTO.setType(ProjectType.KANBAN);
        updateDTO.setStartDate(LocalDate.now().minusDays(7));
        updateDTO.setEndDate(LocalDate.now().plusDays(7));
        Set<Long> updatedTeamMemberIds = new HashSet<>();
        updatedTeamMemberIds.add(teamMemberUser.getId());
        updateDTO.setTeamMemberIds(updatedTeamMemberIds);

        Project updatedProject = new Project();
        updatedProject.setId(1L);
        updatedProject.setName(updateDTO.getName());
        updatedProject.setDescription(updateDTO.getDescription());
        updatedProject.setStatus(updateDTO.getStatus());
        updatedProject.setType(updateDTO.getType());
        updatedProject.setStartDate(updateDTO.getStartDate());
        updatedProject.setEndDate(updateDTO.getEndDate());
        updatedProject.setOwner(ownerUser);
        updatedProject.setTeamMembers(teamMembers);

        when(projectService.findById(1L)).thenReturn(Optional.of(project1));
        when(userService.findById(teamMemberUser.getId())).thenReturn(Optional.of(teamMemberUser));
        when(projectService.save(any(Project.class))).thenReturn(updatedProject);

        // Act
        Project result = projectFacade.updateProject(1L, updateDTO, ownerUser, ownerUserDetails);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(updateDTO.getName());
        assertThat(result.getDescription()).isEqualTo(updateDTO.getDescription());
        assertThat(result.getStatus()).isEqualTo(updateDTO.getStatus());
        assertThat(result.getType()).isEqualTo(updateDTO.getType());
        assertThat(result.getStartDate()).isEqualTo(updateDTO.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(updateDTO.getEndDate());
        
        verify(projectService, times(1)).findById(1L);
        verify(userService, times(1)).findById(teamMemberUser.getId());
        verify(projectService, times(1)).save(any(Project.class));
    }

    @Test
    public void testUpdateProject_ProjectNotFound() {
        // Arrange
        when(projectService.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectFacade.updateProject(999L, projectDTO, ownerUser, ownerUserDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");
        
        verify(projectService, times(1)).findById(999L);
        verify(projectService, never()).save(any(Project.class));
    }

    @Test
    public void testUpdateProject_NotAuthorized() {
        // Arrange
        when(projectService.findById(1L)).thenReturn(Optional.of(project1));

        // Act & Assert
        assertThatThrownBy(() -> projectFacade.updateProject(1L, projectDTO, regularUser, ownerUserDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized to modify this project");
        
        verify(projectService, times(1)).findById(1L);
        verify(projectService, never()).save(any(Project.class));
    }

    @Test
    public void testDeleteProject_Success() {
        // Arrange
        when(projectService.findById(1L)).thenReturn(Optional.of(project1));
        doNothing().when(projectService).deleteById(1L);

        // Act
        projectFacade.deleteProject(1L, ownerUser, ownerUserDetails);

        // Assert
        verify(projectService, times(1)).findById(1L);
        verify(projectService, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteProject_ProjectNotFound() {
        // Arrange
        when(projectService.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectFacade.deleteProject(999L, ownerUser, ownerUserDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");
        
        verify(projectService, times(1)).findById(999L);
        verify(projectService, never()).deleteById(anyLong());
    }

    @Test
    public void testDeleteProject_NotAuthorized() {
        // Arrange
        when(projectService.findById(1L)).thenReturn(Optional.of(project1));

        // Act & Assert
        assertThatThrownBy(() -> projectFacade.deleteProject(1L, regularUser, ownerUserDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized to delete this project");
        
        verify(projectService, times(1)).findById(1L);
        verify(projectService, never()).deleteById(anyLong());
    }

    @Test
    public void testMapToDTO() {
        // Act
        ProjectDTO result = projectFacade.mapToDTO(project1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(project1.getId());
        assertThat(result.getName()).isEqualTo(project1.getName());
        assertThat(result.getDescription()).isEqualTo(project1.getDescription());
        assertThat(result.getStatus()).isEqualTo(project1.getStatus());
        assertThat(result.getType()).isEqualTo(project1.getType());
        assertThat(result.getStartDate()).isEqualTo(project1.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(project1.getEndDate());
        assertThat(result.getTeamMemberIds()).hasSize(1);
        assertThat(result.getTeamMemberIds()).contains(teamMemberUser.getId());
    }
}