package it.zenflow.service;

import it.zenflow.model.project.*;
import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserStoryServiceTest {

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private PlanningPokerSessionRepository planningPokerSessionRepository;

    @InjectMocks
    private UserStoryService userStoryService;

    private Project project;
    private User user;
    private UserStory userStory1;
    private UserStory userStory2;

    @BeforeEach
    public void setup() {
        // Create test data
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        userStory1 = new UserStory();
        userStory1.setId(1L);
        userStory1.setTitle("User Story 1");
        userStory1.setDescription("This is user story 1");
        userStory1.setStatus(StoryStatus.BACKLOG);
        userStory1.setPriority(Priority.HIGH);
        userStory1.setEstimationType(EstimationType.STORY_POINTS);
        userStory1.setProject(project);
        userStory1.setAssignedTo(user);

        userStory2 = new UserStory();
        userStory2.setId(2L);
        userStory2.setTitle("User Story 2");
        userStory2.setDescription("This is user story 2");
        userStory2.setStatus(StoryStatus.IN_PROGRESS);
        userStory2.setPriority(Priority.MEDIUM);
        userStory2.setEstimationType(EstimationType.STORY_POINTS);
        userStory2.setProject(project);
    }

    @Test
    public void testFindAll() {
        // Arrange
        when(userStoryRepository.findAll()).thenReturn(Arrays.asList(userStory1, userStory2));

        // Act
        List<UserStory> result = userStoryService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(userStory1, userStory2);
        verify(userStoryRepository, times(1)).findAll();
    }

    @Test
    public void testFindById() {
        // Arrange
        when(userStoryRepository.findById(1L)).thenReturn(Optional.of(userStory1));

        // Act
        Optional<UserStory> result = userStoryService.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(userStory1);
        verify(userStoryRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindByProject() {
        // Arrange
        when(userStoryRepository.findByProject(project)).thenReturn(Arrays.asList(userStory1, userStory2));

        // Act
        List<UserStory> result = userStoryService.findByProject(project);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(userStory1, userStory2);
        verify(userStoryRepository, times(1)).findByProject(project);
    }

    @Test
    public void testFindByProjectId() {
        // Arrange
        when(userStoryRepository.findByProjectId(1L)).thenReturn(Arrays.asList(userStory1, userStory2));

        // Act
        List<UserStory> result = userStoryService.findByProjectId(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(userStory1, userStory2);
        verify(userStoryRepository, times(1)).findByProjectId(1L);
    }

    @Test
    public void testFindByProjectAndStatus() {
        // Arrange
        when(userStoryRepository.findByProjectAndStatus(project, StoryStatus.BACKLOG)).thenReturn(Arrays.asList(userStory1));

        // Act
        List<UserStory> result = userStoryService.findByProjectAndStatus(project, StoryStatus.BACKLOG);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(userStory1);
        verify(userStoryRepository, times(1)).findByProjectAndStatus(project, StoryStatus.BACKLOG);
    }

    @Test
    public void testFindByProjectIdAndStatus() {
        // Arrange
        when(userStoryRepository.findByProjectIdAndStatus(1L, StoryStatus.IN_PROGRESS)).thenReturn(Arrays.asList(userStory2));

        // Act
        List<UserStory> result = userStoryService.findByProjectIdAndStatus(1L, StoryStatus.IN_PROGRESS);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(userStory2);
        verify(userStoryRepository, times(1)).findByProjectIdAndStatus(1L, StoryStatus.IN_PROGRESS);
    }

    @Test
    public void testFindByAssignedTo() {
        // Arrange
        when(userStoryRepository.findByAssignedTo(user)).thenReturn(Arrays.asList(userStory1));

        // Act
        List<UserStory> result = userStoryService.findByAssignedTo(user);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(userStory1);
        verify(userStoryRepository, times(1)).findByAssignedTo(user);
    }

    @Test
    public void testSave() {
        // Arrange
        userStory1.setStoryPoints(5);
        userStory1.setEstimationType(EstimationType.STORY_POINTS);
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(userStory1);

        // Mock the findByProject to return a list of user stories
        List<UserStory> projectUserStories = Arrays.asList(userStory1, userStory2);
        when(userStoryRepository.findByProject(project)).thenReturn(projectUserStories);

        // Set story points for the second user story
        userStory2.setStoryPoints(3);

        // Expected total story points: 5 + 3 = 8
        when(projectService.save(project)).thenReturn(project);

        // Act
        UserStory result = userStoryService.save(userStory1);

        // Assert
        assertThat(result).isEqualTo(userStory1);
        verify(userStoryRepository, times(1)).save(userStory1);
        verify(userStoryRepository, times(1)).findByProject(project);
        verify(projectService, times(1)).save(project);

        // Verify that the project's total story points were updated
        assertThat(project.getTotalStoryPoints()).isEqualTo(8);
    }

    @Test
    public void testSaveWithPERTEstimation() {
        // Arrange
        UserStory userStoryWithEstimates = new UserStory();
        userStoryWithEstimates.setTitle("User Story with PERT");
        userStoryWithEstimates.setEstimationType(EstimationType.PERT);
        userStoryWithEstimates.setOptimisticEstimate(2.0);
        userStoryWithEstimates.setMostLikelyEstimate(4.0);
        userStoryWithEstimates.setPessimisticEstimate(6.0);
        // Story points should be calculated from PERT estimate, not set manually
        userStoryWithEstimates.setProject(project);

        // Expected PERT calculation: (2 + 4*4 + 6) / 6 = 4.0
        double expectedPertEstimate = 4.0;

        // Expected story points: round(4.0) = 4
        int expectedStoryPoints = 4;

        UserStory savedUserStory = new UserStory();
        savedUserStory.setTitle("User Story with PERT");
        savedUserStory.setEstimationType(EstimationType.PERT);
        savedUserStory.setOptimisticEstimate(2.0);
        savedUserStory.setMostLikelyEstimate(4.0);
        savedUserStory.setPessimisticEstimate(6.0);
        savedUserStory.setPertEstimate(expectedPertEstimate);
        savedUserStory.setStoryPoints(expectedStoryPoints);
        savedUserStory.setProject(project);

        when(userStoryRepository.save(any(UserStory.class))).thenReturn(savedUserStory);

        // Mock the findByProject to return a list of user stories
        List<UserStory> projectUserStories = Arrays.asList(savedUserStory);
        when(userStoryRepository.findByProject(project)).thenReturn(projectUserStories);

        // Mock projectService.save
        when(projectService.save(project)).thenReturn(project);

        // Act
        UserStory result = userStoryService.save(userStoryWithEstimates);

        // Assert
        assertThat(result.getPertEstimate()).isEqualTo(expectedPertEstimate);
        assertThat(result.getStoryPoints()).isEqualTo(expectedStoryPoints);
        verify(userStoryRepository, times(1)).save(any(UserStory.class));
        verify(userStoryRepository, times(1)).findByProject(project);
        verify(projectService, times(1)).save(project);

        // Verify that the project's total story points were updated
        assertThat(project.getTotalStoryPoints()).isEqualTo(expectedStoryPoints);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        userStory1.setStoryPoints(5);
        userStory2.setStoryPoints(3);
        project.setTotalStoryPoints(8); // Initial total

        // Mock findById to return the user story
        when(userStoryRepository.findById(1L)).thenReturn(Optional.of(userStory1));

        // Mock deleteById
        doNothing().when(userStoryRepository).deleteById(1L);

        // Mock findByProject to return only the remaining user story after deletion
        when(userStoryRepository.findByProject(project)).thenReturn(Arrays.asList(userStory2));

        // Mock projectService.save
        when(projectService.save(project)).thenReturn(project);

        // Act
        userStoryService.deleteById(1L);

        // Assert
        verify(userStoryRepository, times(1)).findById(1L);
        verify(userStoryRepository, times(1)).deleteById(1L);
        verify(userStoryRepository, times(1)).findByProject(project);
        verify(projectService, times(1)).save(project);

        // Verify that the project's total story points were updated
        // Expected: 8 - 5 = 3 (only userStory2 remains with 3 points)
        assertThat(project.getTotalStoryPoints()).isEqualTo(3);
    }

    @Test
    public void testDeleteByIdWithCascadeDeletion() {
        // Arrange
        userStory1.setStoryPoints(5);
        project.setTotalStoryPoints(5); // Initial total

        // Create a planning poker session associated with the user story
        PlanningPokerSession session = new PlanningPokerSession();
        session.setId(1L);
        session.setName("Test Session");
        session.setStatus(SessionStatus.CREATED);
        session.setProject(project);
        session.setFacilitator(user);
        session.setUserStory(userStory1);

        // Add the session to the user story's planning sessions
        List<PlanningPokerSession> planningSessions = new ArrayList<>();
        planningSessions.add(session);
        userStory1.setPlanningSessions(planningSessions);

        // Mock findById to return the user story
        when(userStoryRepository.findById(1L)).thenReturn(Optional.of(userStory1));

        // Mock deleteById
        doNothing().when(userStoryRepository).deleteById(1L);

        // Mock findByProject to return an empty list after deletion
        when(userStoryRepository.findByProject(project)).thenReturn(Arrays.asList());

        // Mock projectService.save
        when(projectService.save(project)).thenReturn(project);

        // Act
        userStoryService.deleteById(1L);

        // Assert
        verify(userStoryRepository, times(1)).findById(1L);
        verify(userStoryRepository, times(1)).deleteById(1L);
        verify(userStoryRepository, times(1)).findByProject(project);
        verify(projectService, times(1)).save(project);

        // Verify that the project's total story points were updated
        // Expected: 5 - 5 = 0 (no user stories remain)
        assertThat(project.getTotalStoryPoints()).isEqualTo(0);

        // The cascade delete should happen automatically through JPA, so we don't need to verify
        // any explicit deletion of planning poker sessions in the service layer
    }
}
