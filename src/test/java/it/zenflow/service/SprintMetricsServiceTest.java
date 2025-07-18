package it.zenflow.service;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.SprintRepository;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.project.enums.StoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SprintMetricsServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private SprintMetricsService sprintMetricsService;

    @Captor
    private ArgumentCaptor<Sprint> sprintCaptor;

    @Captor
    private ArgumentCaptor<Long> projectIdCaptor;

    @Captor
    private ArgumentCaptor<Double> velocityCaptor;

    private Project project;
    private Sprint sprint;
    private UserStory userStory1;
    private UserStory userStory2;
    private UserStory userStory3;

    @BeforeEach
    public void setup() {
        // Create test data
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setTeamVelocity(0.0);

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartDate(LocalDate.now().minusDays(7));
        sprint.setEndDate(LocalDate.now().plusDays(7));
        sprint.setProject(project);
        sprint.setPlannedStoryPoints(10);
        sprint.setCompletedStoryPoints(0);
        sprint.setSprintVelocity(0.0);

        userStory1 = new UserStory();
        userStory1.setId(1L);
        userStory1.setTitle("User Story 1");
        userStory1.setStatus(StoryStatus.DONE);
        userStory1.setStoryPoints(3);
        userStory1.setProject(project);
        userStory1.setSprint(sprint);

        userStory2 = new UserStory();
        userStory2.setId(2L);
        userStory2.setTitle("User Story 2");
        userStory2.setStatus(StoryStatus.IN_PROGRESS);
        userStory2.setStoryPoints(5);
        userStory2.setProject(project);
        userStory2.setSprint(sprint);

        userStory3 = new UserStory();
        userStory3.setId(3L);
        userStory3.setTitle("User Story 3");
        userStory3.setStatus(StoryStatus.DONE);
        userStory3.setStoryPoints(2);
        userStory3.setProject(project);
        userStory3.setSprint(sprint);

        List<UserStory> stories = new ArrayList<>();
        stories.add(userStory1);
        stories.add(userStory2);
        stories.add(userStory3);
        sprint.setStories(stories);
    }

    @Test
    public void testUpdateSprintCompletedPoints() {
        // Arrange
        when(sprintRepository.findByIdWithStories(1L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(sprint);

        // Act
        sprintMetricsService.updateSprintCompletedPoints(1L);

        // Assert
        verify(sprintRepository).save(sprintCaptor.capture());
        Sprint savedSprint = sprintCaptor.getValue();
        
        // Only userStory1 and userStory3 are DONE, so completed points should be 3 + 2 = 5
        assertThat(savedSprint.getCompletedStoryPoints()).isEqualTo(5);
        
        // Sprint duration is 14 days (2 weeks), so velocity should be 5 / 2 = 2.5
        assertThat(savedSprint.getSprintVelocity()).isEqualTo(2.5);
        
        // Verify that findCompletedSprintsByProjectId was called with the project ID
        verify(sprintRepository).findCompletedSprintsByProjectId(project.getId());
    }

    @Test
    public void testUpdateProjectVelocity() {
        // Arrange
        Sprint completedSprint1 = new Sprint();
        completedSprint1.setId(2L);
        completedSprint1.setStatus(SprintStatus.COMPLETED);
        completedSprint1.setSprintVelocity(3.0);
        completedSprint1.setProject(project);

        Sprint completedSprint2 = new Sprint();
        completedSprint2.setId(3L);
        completedSprint2.setStatus(SprintStatus.COMPLETED);
        completedSprint2.setSprintVelocity(5.0);
        completedSprint2.setProject(project);

        List<Sprint> completedSprints = Arrays.asList(completedSprint1, completedSprint2);
        
        when(sprintRepository.findCompletedSprintsByProjectId(1L)).thenReturn(completedSprints);
        doNothing().when(projectService).updateProjectVelocity(anyLong(), anyDouble());

        // Act
        sprintMetricsService.updateProjectVelocity(1L);

        // Assert
        verify(projectService).updateProjectVelocity(projectIdCaptor.capture(), velocityCaptor.capture());
        Long capturedProjectId = projectIdCaptor.getValue();
        Double capturedVelocity = velocityCaptor.getValue();
        
        assertThat(capturedProjectId).isEqualTo(1L);
        // Average velocity should be (3.0 + 5.0) / 2 = 4.0
        assertThat(capturedVelocity).isEqualTo(4.0);
    }

    @Test
    public void testUpdateProjectVelocityWithNoCompletedSprints() {
        // Arrange
        when(sprintRepository.findCompletedSprintsByProjectId(1L)).thenReturn(new ArrayList<>());

        // Act
        sprintMetricsService.updateProjectVelocity(1L);

        // Assert
        // No completed sprints, so updateProjectVelocity should not be called
        verify(projectService, never()).updateProjectVelocity(anyLong(), anyDouble());
    }
}