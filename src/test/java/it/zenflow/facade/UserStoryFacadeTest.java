package it.zenflow.facade;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.mapper.UserStoryMapper;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.SprintMetricsService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStoryFacadeTest {

    @Mock
    private UserStoryService userStoryService;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private SprintMetricsService sprintMetricsService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private UserStoryMapper userStoryMapper;

    @InjectMocks
    private UserStoryFacade userStoryFacade;

    // Test data
    private Project project;
    private User owner;
    private User teamMember;
    private User nonTeamMember;
    private UserStory userStory;
    private UserStoryDTO userStoryDTO;
    private UserDetails adminUserDetails;
    private UserDetails regularUserDetails;
    private Sprint sprint;

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
        project.setOwner(owner);
        Set<User> teamMembers = new HashSet<>();
        teamMembers.add(teamMember);
        project.setTeamMembers(teamMembers);

        // Set up sprint
        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setProject(project);

        // Set up user story
        userStory = new UserStory();
        userStory.setId(1L);
        userStory.setTitle("Test User Story");
        userStory.setDescription("Test Description");
        userStory.setAcceptanceCriteria("Test Acceptance Criteria");
        userStory.setStatus(StoryStatus.BACKLOG);
        userStory.setPriority(Priority.MEDIUM);
        userStory.setStoryPoints(5);
        userStory.setBusinessValue(8);
        userStory.setEstimationType(EstimationType.STORY_POINTS);
        userStory.setProject(project);
        userStory.setAssignedTo(teamMember);

        // Set up user story DTO
        userStoryDTO = new UserStoryDTO();
        userStoryDTO.setId(1L);
        userStoryDTO.setTitle("Test User Story");
        userStoryDTO.setDescription("Test Description");
        userStoryDTO.setAcceptanceCriteria("Test Acceptance Criteria");
        userStoryDTO.setStatus(StoryStatus.BACKLOG);
        userStoryDTO.setPriority(Priority.MEDIUM);
        userStoryDTO.setStoryPoints(5);
        userStoryDTO.setBusinessValue(8);
        userStoryDTO.setEstimationType(EstimationType.STORY_POINTS);
        userStoryDTO.setProjectId(1L);
        userStoryDTO.setAssignedToId(2L);

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
        @DisplayName("findByProjectPaginated should return paginated user stories")
        void findByProjectPaginated_ShouldReturnPaginatedUserStories() {
            // Arrange
            Pageable pageable = Pageable.unpaged();
            List<UserStory> userStories = Collections.singletonList(userStory);
            Page<UserStory> page = new PageImpl<>(userStories);
            when(userStoryService.findByProjectPaginated(project, pageable)).thenReturn(page);

            // Act
            Page<UserStory> result = userStoryFacade.findByProjectPaginated(project, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(userStory, result.getContent().get(0));
            verify(userStoryService).findByProjectPaginated(project, pageable);
        }

        @Test
        @DisplayName("findById should return user story when it exists")
        void findById_ShouldReturnUserStory_WhenItExists() {
            // Arrange
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStory));

            // Act
            Optional<UserStory> result = userStoryFacade.findById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(userStory, result.get());
            verify(userStoryService).findById(1L);
        }

        @Test
        @DisplayName("findById should return empty when user story doesn't exist")
        void findById_ShouldReturnEmpty_WhenUserStoryDoesNotExist() {
            // Arrange
            when(userStoryService.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<UserStory> result = userStoryFacade.findById(99L);

            // Assert
            assertFalse(result.isPresent());
            verify(userStoryService).findById(99L);
        }

        @Test
        @DisplayName("findByIdWithTasks should return user story with tasks when it exists")
        void findByIdWithTasks_ShouldReturnUserStoryWithTasks_WhenItExists() {
            // Arrange
            when(userStoryService.findByIdWithTasks(1L)).thenReturn(Optional.of(userStory));

            // Act
            Optional<UserStory> result = userStoryFacade.findByIdWithTasks(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(userStory, result.get());
            verify(userStoryService).findByIdWithTasks(1L);
        }

        @Test
        @DisplayName("getProjectById should return project when it exists")
        void getProjectById_ShouldReturnProject_WhenItExists() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act
            Optional<Project> result = userStoryFacade.getProjectById(1L);

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
            Optional<User> result = userStoryFacade.getUserByUsername("owner");

            // Assert
            assertTrue(result.isPresent());
            assertEquals(owner, result.get());
            verify(userService).findByUsername("owner");
        }

        @Test
        @DisplayName("getUserById should return user when it exists")
        void getUserById_ShouldReturnUser_WhenItExists() {
            // Arrange
            when(userService.findById(1L)).thenReturn(Optional.of(owner));

            // Act
            Optional<User> result = userStoryFacade.getUserById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(owner, result.get());
            verify(userService).findById(1L);
        }
    }

    @Nested
    @DisplayName("Permission Checks Tests")
    class PermissionChecksTests {

        @Test
        @DisplayName("isProjectOwner should return true when user is the project owner")
        void isProjectOwner_ShouldReturnTrue_WhenUserIsProjectOwner() {
            // Act
            boolean result = userStoryFacade.isProjectOwner(project, owner);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("isProjectOwner should return false when user is not the project owner")
        void isProjectOwner_ShouldReturnFalse_WhenUserIsNotProjectOwner() {
            // Act
            boolean result = userStoryFacade.isProjectOwner(project, teamMember);

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
            boolean result = userStoryFacade.isProjectOwner(projectWithoutOwner, owner);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("isTeamMember should return true when user is a team member")
        void isTeamMember_ShouldReturnTrue_WhenUserIsTeamMember() {
            // Act
            boolean result = userStoryFacade.isTeamMember(project, teamMember);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("isTeamMember should return false when user is not a team member")
        void isTeamMember_ShouldReturnFalse_WhenUserIsNotTeamMember() {
            // Act
            boolean result = userStoryFacade.isTeamMember(project, nonTeamMember);

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
            boolean result = userStoryFacade.isTeamMember(projectWithoutTeam, teamMember);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("hasAdminRights should return true when user is project owner")
        void hasAdminRights_ShouldReturnTrue_WhenUserIsProjectOwner() {
            // Act
            boolean result = userStoryFacade.hasAdminRights(project, owner, regularUserDetails);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("hasAdminRights should return true when user has ADMIN authority")
        void hasAdminRights_ShouldReturnTrue_WhenUserHasAdminAuthority() {
            // Act
            boolean result = userStoryFacade.hasAdminRights(project, nonTeamMember, adminUserDetails);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("hasAdminRights should return false when user is not owner and has no ADMIN authority")
        void hasAdminRights_ShouldReturnFalse_WhenUserIsNotOwnerAndHasNoAdminAuthority() {
            // Act
            boolean result = userStoryFacade.hasAdminRights(project, nonTeamMember, regularUserDetails);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("createUserStory should create and return user story when user has permission")
        void createUserStory_ShouldCreateAndReturnUserStory_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userService.findById(2L)).thenReturn(Optional.of(teamMember));
            when(userStoryMapper.toEntity(userStoryDTO)).thenReturn(new UserStory());
            when(userStoryService.save(any(UserStory.class))).thenAnswer(invocation -> {
                UserStory savedStory = invocation.getArgument(0);
                savedStory.setId(1L);
                savedStory.setTitle(userStoryDTO.getTitle());
                return savedStory;
            });

            // Act
            UserStory result = userStoryFacade.createUserStory(userStoryDTO, teamMember, regularUserDetails);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test User Story", result.getTitle());
            assertEquals(project, result.getProject());
            assertEquals(teamMember, result.getAssignedTo());
            verify(projectService).findById(1L);
            verify(userService).findById(2L);
            verify(userStoryService).save(any(UserStory.class));
        }

        @Test
        @DisplayName("createUserStory should throw AccessDeniedException when user has no permission")
        void createUserStory_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                userStoryFacade.createUserStory(userStoryDTO, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(userStoryService, never()).save(any(UserStory.class));
        }

        @Test
        @DisplayName("createUserStory should throw RuntimeException when project not found")
        void createUserStory_ShouldThrowRuntimeException_WhenProjectNotFound() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                userStoryFacade.createUserStory(userStoryDTO, teamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(userStoryService, never()).save(any(UserStory.class));
        }

        @Test
        @DisplayName("updateUserStory should update and return user story when user has permission")
        void updateUserStory_ShouldUpdateAndReturnUserStory_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStory));
            when(userService.findById(2L)).thenReturn(Optional.of(teamMember));
            
            // Update DTO with new values
            userStoryDTO.setTitle("Updated Title");
            userStoryDTO.setStatus(StoryStatus.IN_PROGRESS);
            
            // Mock the updateEntityFromDto method to update the userStory
            doAnswer(invocation -> {
                UserStoryDTO dto = invocation.getArgument(0);
                UserStory story = invocation.getArgument(1);
                story.setTitle(dto.getTitle());
                story.setStatus(dto.getStatus());
                return null;
            }).when(userStoryMapper).updateEntityFromDto(eq(userStoryDTO), any(UserStory.class));
            
            when(userStoryService.save(any(UserStory.class))).thenReturn(userStory);

            // Act
            UserStory result = userStoryFacade.updateUserStory(1L, userStoryDTO, teamMember, regularUserDetails);

            // Assert
            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());
            assertEquals(StoryStatus.IN_PROGRESS, result.getStatus());
            verify(projectService).findById(1L);
            verify(userStoryService).findById(1L);
            verify(userService).findById(2L);
            verify(userStoryService).save(any(UserStory.class));
        }

        @Test
        @DisplayName("updateUserStory should throw AccessDeniedException when user has no permission")
        void updateUserStory_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStory));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                userStoryFacade.updateUserStory(1L, userStoryDTO, nonTeamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(userStoryService).findById(1L);
            verify(userStoryService, never()).save(any(UserStory.class));
        }

        @Test
        @DisplayName("updateUserStory should throw RuntimeException when user story does not belong to project")
        void updateUserStory_ShouldThrowRuntimeException_WhenUserStoryDoesNotBelongToProject() {
            // Arrange
            Project differentProject = new Project();
            differentProject.setId(2L);
            
            UserStory userStoryWithDifferentProject = new UserStory();
            userStoryWithDifferentProject.setId(1L);
            userStoryWithDifferentProject.setProject(differentProject);
            
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStoryWithDifferentProject));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                userStoryFacade.updateUserStory(1L, userStoryDTO, teamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(userStoryService).findById(1L);
            verify(userStoryService, never()).save(any(UserStory.class));
        }

        @Test
        @DisplayName("updateUserStory should update sprint metrics when status changes to DONE")
        void updateUserStory_ShouldUpdateSprintMetrics_WhenStatusChangesToDone() {
            // Arrange
            userStory.setSprint(sprint);
            
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStory));
            when(userService.findById(2L)).thenReturn(Optional.of(teamMember));
            
            // Update DTO with DONE status
            userStoryDTO.setStatus(StoryStatus.DONE);
            
            // Mock the updateEntityFromDto method to update the userStory status to DONE
            doAnswer(invocation -> {
                UserStoryDTO dto = invocation.getArgument(0);
                UserStory story = invocation.getArgument(1);
                story.setStatus(dto.getStatus()); // This sets the status to DONE
                return null;
            }).when(userStoryMapper).updateEntityFromDto(eq(userStoryDTO), any(UserStory.class));
            
            when(userStoryService.save(any(UserStory.class))).thenAnswer(invocation -> {
                UserStory savedStory = invocation.getArgument(0);
                return savedStory;
            });

            // Act
            userStoryFacade.updateUserStory(1L, userStoryDTO, teamMember, regularUserDetails);

            // Assert
            verify(sprintMetricsService).updateSprintCompletedPoints(sprint.getId());
        }

        @Test
        @DisplayName("deleteUserStory should delete user story when user has permission")
        void deleteUserStory_ShouldDeleteUserStory_WhenUserHasPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStory));

            // Act
            userStoryFacade.deleteUserStory(1L, 1L, owner, regularUserDetails);

            // Assert
            verify(projectService).findById(1L);
            verify(userStoryService).findById(1L);
            verify(userStoryService).deleteById(1L);
        }

        @Test
        @DisplayName("deleteUserStory should throw AccessDeniedException when user has no permission")
        void deleteUserStory_ShouldThrowAccessDeniedException_WhenUserHasNoPermission() {
            // Arrange
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStory));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> 
                userStoryFacade.deleteUserStory(1L, 1L, teamMember, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(userStoryService).findById(1L);
            verify(userStoryService, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("deleteUserStory should throw RuntimeException when user story does not belong to project")
        void deleteUserStory_ShouldThrowRuntimeException_WhenUserStoryDoesNotBelongToProject() {
            // Arrange
            Project differentProject = new Project();
            differentProject.setId(2L);
            
            UserStory userStoryWithDifferentProject = new UserStory();
            userStoryWithDifferentProject.setId(1L);
            userStoryWithDifferentProject.setProject(differentProject);
            
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(userStoryService.findById(1L)).thenReturn(Optional.of(userStoryWithDifferentProject));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                userStoryFacade.deleteUserStory(1L, 1L, owner, regularUserDetails));
            
            verify(projectService).findById(1L);
            verify(userStoryService).findById(1L);
            verify(userStoryService, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("mapToDTO should convert user story to DTO")
        void mapToDTO_ShouldConvertUserStoryToDTO() {
            // Arrange
            when(userStoryMapper.toDto(userStory)).thenReturn(userStoryDTO);

            // Act
            UserStoryDTO result = userStoryFacade.mapToDTO(userStory);

            // Assert
            assertNotNull(result);
            assertEquals(userStoryDTO, result);
            verify(userStoryMapper).toDto(userStory);
        }

        @Test
        @DisplayName("getLocalizedMessage should return message from message source")
        void getLocalizedMessage_ShouldReturnMessageFromMessageSource() {
            // Arrange
            String code = "userstory.created";
            String expectedMessage = "User story created successfully";
            when(messageSource.getMessage(eq(code), isNull(), any(Locale.class))).thenReturn(expectedMessage);

            // Act
            String result = userStoryFacade.getLocalizedMessage(code);

            // Assert
            assertEquals(expectedMessage, result);
            verify(messageSource).getMessage(eq(code), isNull(), any(Locale.class));
        }
    }
}