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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintFacadeTest {

    @Mock
    private SprintService sprintService;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserStoryService userStoryService;

    @Mock
    private UserService userService;

    @Mock
    private SprintMetricsService sprintMetricsService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SprintMapper sprintMapper;

    @InjectMocks
    private SprintFacade sprintFacade;

    // Test data
    private Project project;
    private User owner;
    private User teamMember;
    private User nonTeamMember;
    private Sprint sprint;
    private SprintDTO sprintDTO;
    private UserDetails adminUserDetails;
    private UserDetails regularUserDetails;
    private List<UserStory> userStories;

    @BeforeEach
    void setUp() {
        // Set up project
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");

        teamMember = new User();
        teamMember.setId(2L);
        teamMember.setUsername("team_member");

        nonTeamMember = new User();
        nonTeamMember.setId(3L);
        nonTeamMember.setUsername("non_team_member");

        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setType(ProjectType.SCRUM);
        project.setOwner(owner);
        Set<User> teamMembers = new HashSet<>();
        teamMembers.add(teamMember);
        project.setTeamMembers(teamMembers);

        // Set up sprint
        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setGoal("Complete feature X");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(14));
        sprint.setStatus(SprintStatus.PLANNED);
        sprint.setProject(project);
        sprint.setPlannedStoryPoints(10);
        sprint.setCompletedStoryPoints(0);
        sprint.setSprintVelocity(0.0);

        // Set up sprint DTO
        sprintDTO = new SprintDTO();
        sprintDTO.setId(1L);
        sprintDTO.setName("Sprint 1");
        sprintDTO.setGoal("Complete feature X");
        sprintDTO.setStartDate(LocalDate.now());
        sprintDTO.setEndDate(LocalDate.now().plusDays(14));
        sprintDTO.setStatus(SprintStatus.PLANNED);
        sprintDTO.setProjectId(1L);
        sprintDTO.setPlannedStoryPoints(10);
        sprintDTO.setCompletedStoryPoints(0);
        sprintDTO.setSprintVelocity(0.0);

        // Set up user stories
        userStories = new ArrayList<>();
        UserStory userStory = new UserStory();
        userStory.setId(1L);
        userStory.setTitle("Test User Story");
        userStory.setProject(project);
        userStories.add(userStory);

        // Set up user details
        adminUserDetails = createUserDetails("admin", Collections.singletonList("ADMIN"));
        regularUserDetails = createUserDetails("regular", Collections.singletonList("USER"));
    }

    private UserDetails createUserDetails(String username, List<String> roles) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
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

    @Nested
    @DisplayName("Query Operations Tests")
    class QueryOperationsTests {

        @Test
        @DisplayName("findByProject should return sprints for a project")
        void findByProject_ShouldReturnSprintsForProject() {
            // Arrange
            List<Sprint> sprints = Collections.singletonList(sprint);
            when(sprintService.findByProject(project)).thenReturn(sprints);

            // Act
            List<Sprint> result = sprintFacade.findByProject(project);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(sprint, result.get(0));
            verify(sprintService).findByProject(project);
        }

        @Test
        @DisplayName("findById should return sprint when it exists")
        void findById_ShouldReturnSprint_WhenItExists() {
            // Arrange
            when(sprintService.findById(1L)).thenReturn(Optional.of(sprint));

            // Act
            Optional<Sprint> result = sprintFacade.findById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(sprint, result.get());
            verify(sprintService).findById(1L);
        }

        @Test
        @DisplayName("findById should return empty when sprint doesn't exist")
        void findById_ShouldReturnEmpty_WhenSprintDoesNotExist() {
            // Arrange
            when(sprintService.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<Sprint> result = sprintFacade.findById(99L);

            // Assert
            assertFalse(result.isPresent());
            verify(sprintService).findById(99L);
        }

        @Test
        @DisplayName("findByIdWithStories should return sprint with stories when it exists")
        void findByIdWithStories_ShouldReturnSprintWithStories_WhenItExists() {
            // Arrange
            when(sprintService.findByIdWithStories(1L)).thenReturn(Optional.of(sprint));

            // Act
            Optional<Sprint> result = sprintFacade.findByIdWithStories(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(sprint, result.get());
            verify(sprintService).findByIdWithStories(1L);
        }

        @Test
        @DisplayName("getProjectById should return project when it exists")
        void getProjectById_ShouldReturnProject_WhenItExists() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            Optional<Project> result = sprintFacade.getProjectById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(project, result.get());
            verify(projectService).findById(1L);
        }

        @Test
        @DisplayName("getUserByUsername should return user when it exists")
        void getUserByUsername_ShouldReturnUser_WhenItExists() {
            // Arrange
            when(userService.findByUsername("owner")).thenReturn(Optional.of(owner));

            // Act
            Optional<User> result = sprintFacade.getUserByUsername("owner");

            // Assert
            assertTrue(result.isPresent());
            assertEquals(owner, result.get());
            verify(userService).findByUsername("owner");
        }

        @Test
        @DisplayName("findUnassignedUserStories should return unassigned user stories")
        void findUnassignedUserStories_ShouldReturnUnassignedUserStories() {
            // Arrange
            when(userStoryService.findUnassignedUserStories(project)).thenReturn(userStories);

            // Act
            List<UserStory> result = sprintFacade.findUnassignedUserStories(project);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(userStories.get(0), result.get(0));
            verify(userStoryService).findUnassignedUserStories(project);
        }
    }

    @Nested
    @DisplayName("Permission Checks Tests")
    class PermissionChecksTests {

        @Test
        @DisplayName("isProjectOwner should return true when user is the project owner")
        void isProjectOwner_ShouldReturnTrue_WhenUserIsProjectOwner() {
            // Act
            boolean result = sprintFacade.isProjectOwner(project, owner);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("isProjectOwner should return false when user is not the project owner")
        void isProjectOwner_ShouldReturnFalse_WhenUserIsNotProjectOwner() {
            // Act
            boolean result = sprintFacade.isProjectOwner(project, teamMember);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("isProjectOwner should return false when project has no owner")
        void isProjectOwner_ShouldReturnFalse_WhenProjectHasNoOwner() {
            // Arrange
            Project projectWithoutOwner = new Project();
            projectWithoutOwner.setId(2L);

            // Act
            boolean result = sprintFacade.isProjectOwner(projectWithoutOwner, owner);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("isTeamMember should return true when user is a team member")
        void isTeamMember_ShouldReturnTrue_WhenUserIsTeamMember() {
            // Act
            boolean result = sprintFacade.isTeamMember(project, teamMember);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("isTeamMember should return false when user is not a team member")
        void isTeamMember_ShouldReturnFalse_WhenUserIsNotTeamMember() {
            // Act
            boolean result = sprintFacade.isTeamMember(project, nonTeamMember);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("isTeamMember should return false when project has no team members")
        void isTeamMember_ShouldReturnFalse_WhenProjectHasNoTeamMembers() {
            // Arrange
            Project projectWithoutTeam = new Project();
            projectWithoutTeam.setId(2L);

            // Act
            boolean result = sprintFacade.isTeamMember(projectWithoutTeam, teamMember);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("hasAdminRights should return true when user is project owner")
        void hasAdminRights_ShouldReturnTrue_WhenUserIsProjectOwner() {
            // Act
            boolean result = sprintFacade.hasAdminRights(project, owner, regularUserDetails);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("hasAdminRights should return true when user has ADMIN authority")
        void hasAdminRights_ShouldReturnTrue_WhenUserHasAdminAuthority() {
            // Act
            boolean result = sprintFacade.hasAdminRights(project, nonTeamMember, adminUserDetails);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("hasAdminRights should return false when user is not owner and has no ADMIN authority")
        void hasAdminRights_ShouldReturnFalse_WhenUserIsNotOwnerAndHasNoAdminAuthority() {
            // Act
            boolean result = sprintFacade.hasAdminRights(project, nonTeamMember, regularUserDetails);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("isProjectScrum should return true when project is of type SCRUM")
        void isProjectScrum_ShouldReturnTrue_WhenProjectIsScrum() {
            // Act
            boolean result = sprintFacade.isProjectScrum(project);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("isProjectScrum should return false when project is not of type SCRUM")
        void isProjectScrum_ShouldReturnFalse_WhenProjectIsNotScrum() {
            // Arrange
            project.setType(ProjectType.KANBAN);

            // Act
            boolean result = sprintFacade.isProjectScrum(project);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("isProjectScrum should return false when project has no type")
        void isProjectScrum_ShouldReturnFalse_WhenProjectHasNoType() {
            // Arrange
            project.setType(null);

            // Act
            boolean result = sprintFacade.isProjectScrum(project);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("hasSprintsWithStatus should return true when sprints contain the status")
        void hasSprintsWithStatus_ShouldReturnTrue_WhenSprintsContainStatus() {
            // Arrange
            List<Sprint> sprints = Collections.singletonList(sprint);

            // Act
            boolean result = sprintFacade.hasSprintsWithStatus(sprints, SprintStatus.PLANNED);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("hasSprintsWithStatus should return false when sprints don't contain the status")
        void hasSprintsWithStatus_ShouldReturnFalse_WhenSprintsDontContainStatus() {
            // Arrange
            List<Sprint> sprints = Collections.singletonList(sprint);

            // Act
            boolean result = sprintFacade.hasSprintsWithStatus(sprints, SprintStatus.ACTIVE);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("createSprint should create and return sprint when user has permission")
        void createSprint_ShouldCreateAndReturnSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(sprintMapper.toEntity(sprintDTO)).thenReturn(sprint);
            when(sprintService.save(any(Sprint.class))).thenReturn(sprint);

            // Act
            Sprint result = sprintFacade.createSprint(sprintDTO, teamMember, regularUserDetails);

            // Assert
            assertNotNull(result);
            assertEquals(sprint, result);
            verify(projectService).findById(1L);
            verify(sprintMapper).toEntity(sprintDTO);
            verify(sprintService).save(any(Sprint.class));
        }

        @Test
        @DisplayName("createSprint should throw IllegalArgumentException when project is not SCRUM")
        void createSprint_ShouldThrowIllegalArgumentException_WhenProjectIsNotScrum() {
            // Arrange
            project.setType(ProjectType.KANBAN);
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(messageSource.getMessage(eq("sprint.only_for_scrum"), isNull(), any(Locale.class)))
                    .thenReturn("Sprints are only available for SCRUM projects");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                sprintFacade.createSprint(sprintDTO, teamMember, regularUserDetails));
            
            assertEquals("Sprints are only available for SCRUM projects", exception.getMessage());
            verify(projectService).findById(1L);
            verify(sprintService, never()).save(any(Sprint.class));
        }

        @Test
        @DisplayName("createSprint should throw AccessDeniedException when user has no permission")
        void createSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.createSprint(sprintDTO, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService, never()).save(any(Sprint.class));
        }

        @Test
        @DisplayName("updateSprint should update and return sprint when user has permission")
        void updateSprint_ShouldUpdateAndReturnSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(sprintService.findById(1L)).thenReturn(Optional.of(sprint));
            when(sprintService.save(any(Sprint.class))).thenReturn(sprint);

            // Act
            Sprint result = sprintFacade.updateSprint(1L, sprintDTO, teamMember, regularUserDetails);

            // Assert
            assertNotNull(result);
            assertEquals(sprint, result);
            verify(projectService).findById(1L);
            verify(sprintService).findById(1L);
            verify(sprintMapper).updateEntityFromDto(sprintDTO, sprint);
            verify(sprintService).save(any(Sprint.class));
        }

        @Test
        @DisplayName("updateSprint should throw AccessDeniedException when user has no permission")
        void updateSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(sprintService.findById(1L)).thenReturn(Optional.of(sprint));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.updateSprint(1L, sprintDTO, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService).findById(1L);
            verify(sprintService, never()).save(any(Sprint.class));
        }

        @Test
        @DisplayName("updateSprint should throw RuntimeException when sprint does not belong to project")
        void updateSprint_ShouldThrowRuntimeException_WhenSprintDoesNotBelongToProject() {
            // Arrange
            Project differentProject = new Project();
            differentProject.setId(2L);
            
            Sprint sprintWithDifferentProject = new Sprint();
            sprintWithDifferentProject.setId(1L);
            sprintWithDifferentProject.setProject(differentProject);
            
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(sprintService.findById(1L)).thenReturn(Optional.of(sprintWithDifferentProject));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                sprintFacade.updateSprint(1L, sprintDTO, teamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService).findById(1L);
            verify(sprintService, never()).save(any(Sprint.class));
        }
    }

    @Nested
    @DisplayName("Sprint Operations Tests")
    class SprintOperationsTests {

        @Test
        @DisplayName("startSprint should call sprintService.startSprint when user has permission")
        void startSprint_ShouldCallSprintServiceStartSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            sprintFacade.startSprint(1L, 1L, teamMember, regularUserDetails);

            // Assert
            verify(projectService).findById(1L);
            verify(sprintService).startSprint(1L);
        }

        @Test
        @DisplayName("startSprint should throw AccessDeniedException when user has no permission")
        void startSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.startSprint(1L, 1L, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService, never()).startSprint(anyLong());
        }

        @Test
        @DisplayName("completeSprint should call sprintService.completeSprint when user has permission")
        void completeSprint_ShouldCallSprintServiceCompleteSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            sprintFacade.completeSprint(1L, 1L, teamMember, regularUserDetails);

            // Assert
            verify(projectService).findById(1L);
            verify(sprintService).completeSprint(1L);
        }

        @Test
        @DisplayName("completeSprint should throw AccessDeniedException when user has no permission")
        void completeSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.completeSprint(1L, 1L, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService, never()).completeSprint(anyLong());
        }

        @Test
        @DisplayName("cancelSprint should call sprintService.cancelSprint when user has permission")
        void cancelSprint_ShouldCallSprintServiceCancelSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            sprintFacade.cancelSprint(1L, 1L, teamMember, regularUserDetails);

            // Assert
            verify(projectService).findById(1L);
            verify(sprintService).cancelSprint(1L);
        }

        @Test
        @DisplayName("cancelSprint should throw AccessDeniedException when user has no permission")
        void cancelSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.cancelSprint(1L, 1L, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService, never()).cancelSprint(anyLong());
        }

        @Test
        @DisplayName("addUserStoryToSprint should call sprintService.addUserStoryToSprint when user has permission")
        void addUserStoryToSprint_ShouldCallSprintServiceAddUserStoryToSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            sprintFacade.addUserStoryToSprint(1L, 1L, 1L, teamMember, regularUserDetails);

            // Assert
            verify(projectService).findById(1L);
            verify(sprintService).addUserStoryToSprint(1L, 1L);
        }

        @Test
        @DisplayName("addUserStoryToSprint should throw AccessDeniedException when user has no permission")
        void addUserStoryToSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.addUserStoryToSprint(1L, 1L, 1L, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService, never()).addUserStoryToSprint(anyLong(), anyLong());
        }

        @Test
        @DisplayName("removeUserStoryFromSprint should call sprintService.removeUserStoryFromSprint when user has permission")
        void removeUserStoryFromSprint_ShouldCallSprintServiceRemoveUserStoryFromSprint_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            sprintFacade.removeUserStoryFromSprint(1L, 1L, 1L, teamMember, regularUserDetails);

            // Assert
            verify(projectService).findById(1L);
            verify(sprintService).removeUserStoryFromSprint(1L, 1L);
        }

        @Test
        @DisplayName("removeUserStoryFromSprint should throw AccessDeniedException when user has no permission")
        void removeUserStoryFromSprint_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                sprintFacade.removeUserStoryFromSprint(1L, 1L, 1L, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(sprintService, never()).removeUserStoryFromSprint(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("mapToDTO should convert sprint to DTO")
        void mapToDTO_ShouldConvertSprintToDTO() {
            // Arrange
            when(sprintMapper.toDto(sprint)).thenReturn(sprintDTO);

            // Act
            SprintDTO result = sprintFacade.mapToDTO(sprint);

            // Assert
            assertNotNull(result);
            assertEquals(sprintDTO, result);
            verify(sprintMapper).toDto(sprint);
        }

        @Test
        @DisplayName("getLocalizedMessage should return message from message source")
        void getLocalizedMessage_ShouldReturnMessageFromMessageSource() {
            // Arrange
            String code = "sprint.created";
            String expectedMessage = "Sprint created successfully";
            when(messageSource.getMessage(eq(code), isNull(), any(Locale.class))).thenReturn(expectedMessage);

            // Act
            String result = sprintFacade.getLocalizedMessage(code);

            // Assert
            assertEquals(expectedMessage, result);
            verify(messageSource).getMessage(eq(code), isNull(), any(Locale.class));
        }
    }
}