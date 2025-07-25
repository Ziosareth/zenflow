package it.zenflow.mapper;

import it.zenflow.dto.SprintDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SprintStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the SprintMapper implementation.
 * Tests basic mapping functionality without relationship setting.
 */
@ExtendWith(MockitoExtension.class)
public class SprintMapperTest {

    @Spy
    @InjectMocks
    private SprintMapperImpl sprintMapper;

    private SprintDTO sprintDTO;
    private Sprint sprint;
    private Project project;
    private List<UserStory> userStories;

    @BeforeEach
    public void setup() {
        // Create test project
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        // Create test user stories
        userStories = new ArrayList<>();
        UserStory userStory = new UserStory();
        userStory.setId(1L);
        userStory.setTitle("Test User Story");
        userStories.add(userStory);

        // Create test DTO
        sprintDTO = new SprintDTO();
        sprintDTO.setId(1L);
        sprintDTO.setName("Sprint 1");
        sprintDTO.setGoal("Complete feature X");
        sprintDTO.setStartDate(LocalDate.now());
        sprintDTO.setEndDate(LocalDate.now().plusDays(14));
        sprintDTO.setStatus(SprintStatus.PLANNED);
        sprintDTO.setProjectId(project.getId());
        sprintDTO.setPlannedStoryPoints(10);
        sprintDTO.setCompletedStoryPoints(0);
        sprintDTO.setSprintVelocity(0.0);

        // Create test entity
        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setGoal("Complete feature X");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(14));
        sprint.setStatus(SprintStatus.PLANNED);
        sprint.setProject(project);
        sprint.setStories(userStories);
        sprint.setPlannedStoryPoints(10);
        sprint.setCompletedStoryPoints(0);
        sprint.setSprintVelocity(0.0);
    }

    @Test
    public void testToEntity() {
        // Act
        Sprint result = sprintMapper.toEntity(sprintDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(sprintDTO.getId());
        assertThat(result.getName()).isEqualTo(sprintDTO.getName());
        assertThat(result.getGoal()).isEqualTo(sprintDTO.getGoal());
        assertThat(result.getStartDate()).isEqualTo(sprintDTO.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(sprintDTO.getEndDate());
        assertThat(result.getStatus()).isEqualTo(sprintDTO.getStatus());
        assertThat(result.getPlannedStoryPoints()).isEqualTo(sprintDTO.getPlannedStoryPoints());
        assertThat(result.getCompletedStoryPoints()).isEqualTo(sprintDTO.getCompletedStoryPoints());
        assertThat(result.getSprintVelocity()).isEqualTo(sprintDTO.getSprintVelocity());
        
        // Verify collections are initialized but empty
        assertThat(result.getStories()).isNotNull().isEmpty();
        
        // Relationships should be null as they are ignored in the mapping
        // and are expected to be loaded via fetch joins in repository queries
        assertThat(result.getProject()).isNull();
    }

    @Test
    public void testToDto() {
        // Act
        SprintDTO result = sprintMapper.toDto(sprint);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(sprint.getId());
        assertThat(result.getName()).isEqualTo(sprint.getName());
        assertThat(result.getGoal()).isEqualTo(sprint.getGoal());
        assertThat(result.getStartDate()).isEqualTo(sprint.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(sprint.getEndDate());
        assertThat(result.getStatus()).isEqualTo(sprint.getStatus());
        assertThat(result.getPlannedStoryPoints()).isEqualTo(sprint.getPlannedStoryPoints());
        assertThat(result.getCompletedStoryPoints()).isEqualTo(sprint.getCompletedStoryPoints());
        assertThat(result.getSprintVelocity()).isEqualTo(sprint.getSprintVelocity());
        
        // Verify relationship IDs are correctly mapped
        assertThat(result.getProjectId()).isEqualTo(project.getId());
    }

    @Test
    public void testToDto_WithNullRelationships() {
        // Arrange
        sprint.setProject(null);

        // Act
        SprintDTO result = sprintMapper.toDto(sprint);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isNull();
    }

    @Test
    public void testUpdateEntityFromDto() {
        // Arrange
        Sprint existingSprint = new Sprint();
        existingSprint.setId(1L);
        existingSprint.setName("Old Name");
        existingSprint.setGoal("Old Goal");
        existingSprint.setStartDate(LocalDate.now().minusDays(7));
        existingSprint.setEndDate(LocalDate.now().plusDays(7));
        existingSprint.setStatus(SprintStatus.PLANNED);
        existingSprint.setProject(project);
        existingSprint.setPlannedStoryPoints(5);
        existingSprint.setCompletedStoryPoints(0);
        existingSprint.setSprintVelocity(0.0);

        SprintDTO updateDTO = new SprintDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setGoal("Updated Goal");
        updateDTO.setStartDate(LocalDate.now());
        updateDTO.setEndDate(LocalDate.now().plusDays(14));
        updateDTO.setStatus(SprintStatus.ACTIVE); // Should be ignored
        updateDTO.setProjectId(2L); // Should be ignored
        updateDTO.setPlannedStoryPoints(10);
        updateDTO.setCompletedStoryPoints(5);
        updateDTO.setSprintVelocity(2.5);

        // Act
        sprintMapper.updateEntityFromDto(updateDTO, existingSprint);

        // Assert
        assertThat(existingSprint.getId()).isEqualTo(1L); // ID should not change
        assertThat(existingSprint.getName()).isEqualTo("Updated Name");
        assertThat(existingSprint.getGoal()).isEqualTo("Updated Goal");
        assertThat(existingSprint.getStartDate()).isEqualTo(updateDTO.getStartDate());
        assertThat(existingSprint.getEndDate()).isEqualTo(updateDTO.getEndDate());
        assertThat(existingSprint.getStatus()).isEqualTo(SprintStatus.PLANNED); // Status should not change
        assertThat(existingSprint.getPlannedStoryPoints()).isEqualTo(10);
        assertThat(existingSprint.getCompletedStoryPoints()).isEqualTo(5);
        assertThat(existingSprint.getSprintVelocity()).isEqualTo(2.5);
        
        // Relationships should remain unchanged as they are ignored in the mapping
        assertThat(existingSprint.getProject()).isEqualTo(project); // Unchanged
    }

    @Test
    public void testUpdateEntityFromDto_WithNullValues() {
        // Arrange
        Sprint existingSprint = new Sprint();
        existingSprint.setId(1L);
        existingSprint.setName("Old Name");
        existingSprint.setGoal("Old Goal");
        existingSprint.setStartDate(LocalDate.now().minusDays(7));
        existingSprint.setEndDate(LocalDate.now().plusDays(7));
        existingSprint.setStatus(SprintStatus.PLANNED);
        existingSprint.setProject(project);
        existingSprint.setPlannedStoryPoints(5);
        existingSprint.setCompletedStoryPoints(2);
        existingSprint.setSprintVelocity(1.5);

        SprintDTO updateDTO = new SprintDTO();
        updateDTO.setName("Updated Name");
        // Note: SprintDTO initializes default values for these fields, so they're not null
        // and will be applied during the update
        updateDTO.setPlannedStoryPoints(null); // Explicitly set to null to prevent default value

        // Act
        sprintMapper.updateEntityFromDto(updateDTO, existingSprint);

        // Assert
        assertThat(existingSprint.getId()).isEqualTo(1L);
        assertThat(existingSprint.getName()).isEqualTo("Updated Name");
        assertThat(existingSprint.getGoal()).isEqualTo("Old Goal"); // Should not change
        assertThat(existingSprint.getStartDate()).isEqualTo(LocalDate.now().minusDays(7)); // Should not change
        assertThat(existingSprint.getEndDate()).isEqualTo(LocalDate.now().plusDays(7)); // Should not change
        assertThat(existingSprint.getStatus()).isEqualTo(SprintStatus.PLANNED); // Should not change
        assertThat(existingSprint.getPlannedStoryPoints()).isEqualTo(5); // Should not change when explicitly set to null
        assertThat(existingSprint.getCompletedStoryPoints()).isEqualTo(0); // Default value from DTO is applied
        assertThat(existingSprint.getSprintVelocity()).isEqualTo(0.0); // Default value from DTO is applied
    }
}