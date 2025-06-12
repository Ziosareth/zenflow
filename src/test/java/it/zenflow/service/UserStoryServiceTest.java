package it.zenflow.service;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.UserStoryRepository;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        userStory1.setProject(project);
        userStory1.setAssignedTo(user);

        userStory2 = new UserStory();
        userStory2.setId(2L);
        userStory2.setTitle("User Story 2");
        userStory2.setDescription("This is user story 2");
        userStory2.setStatus(StoryStatus.IN_PROGRESS);
        userStory2.setPriority(Priority.MEDIUM);
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
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(userStory1);

        // Act
        UserStory result = userStoryService.save(userStory1);

        // Assert
        assertThat(result).isEqualTo(userStory1);
        verify(userStoryRepository, times(1)).save(userStory1);
    }

    @Test
    public void testSaveWithPERTEstimation() {
        // Arrange
        UserStory userStoryWithEstimates = new UserStory();
        userStoryWithEstimates.setTitle("User Story with PERT");
        userStoryWithEstimates.setOptimisticEstimate(2.0);
        userStoryWithEstimates.setMostLikelyEstimate(4.0);
        userStoryWithEstimates.setPessimisticEstimate(6.0);
        
        // Expected PERT calculation: (2 + 4*4 + 6) / 6 = 4.0
        double expectedPertEstimate = 4.0;
        
        UserStory savedUserStory = new UserStory();
        savedUserStory.setTitle("User Story with PERT");
        savedUserStory.setOptimisticEstimate(2.0);
        savedUserStory.setMostLikelyEstimate(4.0);
        savedUserStory.setPessimisticEstimate(6.0);
        savedUserStory.setPertEstimate(expectedPertEstimate);
        
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(savedUserStory);

        // Act
        UserStory result = userStoryService.save(userStoryWithEstimates);

        // Assert
        assertThat(result.getPertEstimate()).isEqualTo(expectedPertEstimate);
        verify(userStoryRepository, times(1)).save(any(UserStory.class));
    }

    @Test
    public void testDeleteById() {
        // Arrange
        doNothing().when(userStoryRepository).deleteById(1L);

        // Act
        userStoryService.deleteById(1L);

        // Assert
        verify(userStoryRepository, times(1)).deleteById(1L);
    }
}