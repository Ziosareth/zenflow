package it.zenflow.mapper;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the UserStoryMapper implementation.
 * Tests basic mapping functionality without relationship setting.
 */
@ExtendWith(MockitoExtension.class)
public class UserStoryMapperTest {

    @Spy
    @InjectMocks
    private UserStoryMapperImpl userStoryMapper;

    private UserStoryDTO userStoryDTO;
    private UserStory userStory;
    private Project project;
    private Sprint sprint;
    private User assignedUser;

    @BeforeEach
    public void setup() {
        // Create test project
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        // Create test sprint
        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setProject(project);

        // Create test user
        assignedUser = new User();
        assignedUser.setId(1L);
        assignedUser.setUsername("testuser");
        assignedUser.setEmail("test@example.com");

        // Create test DTO
        userStoryDTO = new UserStoryDTO();
        userStoryDTO.setId(1L);
        userStoryDTO.setTitle("Test User Story");
        userStoryDTO.setDescription("This is a test user story");
        userStoryDTO.setAcceptanceCriteria("The user story should pass all tests");
        userStoryDTO.setStatus(StoryStatus.BACKLOG);
        userStoryDTO.setPriority(Priority.MEDIUM);
        userStoryDTO.setStoryPoints(5);
        userStoryDTO.setBusinessValue(8);
        userStoryDTO.setEstimationType(EstimationType.STORY_POINTS);
        userStoryDTO.setProjectId(project.getId());
        userStoryDTO.setSprintId(sprint.getId());
        userStoryDTO.setAssignedToId(assignedUser.getId());
        userStoryDTO.setOptimisticEstimate(3.0);
        userStoryDTO.setPessimisticEstimate(8.0);
        userStoryDTO.setMostLikelyEstimate(5.0);
        userStoryDTO.setPertEstimate(5.2);
        userStoryDTO.setVariance(0.7);

        // Create test entity
        userStory = new UserStory();
        userStory.setId(1L);
        userStory.setTitle("Test User Story");
        userStory.setDescription("This is a test user story");
        userStory.setAcceptanceCriteria("The user story should pass all tests");
        userStory.setStatus(StoryStatus.BACKLOG);
        userStory.setPriority(Priority.MEDIUM);
        userStory.setStoryPoints(5);
        userStory.setBusinessValue(8);
        userStory.setEstimationType(EstimationType.STORY_POINTS);
        userStory.setProject(project);
        userStory.setSprint(sprint);
        userStory.setAssignedTo(assignedUser);
        userStory.setOptimisticEstimate(3.0);
        userStory.setPessimisticEstimate(8.0);
        userStory.setMostLikelyEstimate(5.0);
        userStory.setPertEstimate(5.2);
        userStory.setVariance(0.7);
        userStory.setTasks(new ArrayList<>());
        userStory.setPlanningSessions(new ArrayList<>());
    }

    @Test
    public void testToEntity() {
        // Act
        UserStory result = userStoryMapper.toEntity(userStoryDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userStoryDTO.getId());
        assertThat(result.getTitle()).isEqualTo(userStoryDTO.getTitle());
        assertThat(result.getDescription()).isEqualTo(userStoryDTO.getDescription());
        assertThat(result.getAcceptanceCriteria()).isEqualTo(userStoryDTO.getAcceptanceCriteria());
        assertThat(result.getStatus()).isEqualTo(userStoryDTO.getStatus());
        assertThat(result.getPriority()).isEqualTo(userStoryDTO.getPriority());
        assertThat(result.getStoryPoints()).isEqualTo(userStoryDTO.getStoryPoints());
        assertThat(result.getBusinessValue()).isEqualTo(userStoryDTO.getBusinessValue());
        assertThat(result.getEstimationType()).isEqualTo(userStoryDTO.getEstimationType());
        assertThat(result.getOptimisticEstimate()).isEqualTo(userStoryDTO.getOptimisticEstimate());
        assertThat(result.getPessimisticEstimate()).isEqualTo(userStoryDTO.getPessimisticEstimate());
        assertThat(result.getMostLikelyEstimate()).isEqualTo(userStoryDTO.getMostLikelyEstimate());
        assertThat(result.getPertEstimate()).isEqualTo(userStoryDTO.getPertEstimate());
        assertThat(result.getVariance()).isEqualTo(userStoryDTO.getVariance());
        
        // Verify collections are initialized but empty
        assertThat(result.getTasks()).isEmpty();
        assertThat(result.getPlanningSessions()).isEmpty();
        
        // Relationships should be null as they are ignored in the mapping
        // and are expected to be loaded via fetch joins in repository queries
        assertThat(result.getProject()).isNull();
        assertThat(result.getSprint()).isNull();
        assertThat(result.getAssignedTo()).isNull();
    }

    @Test
    public void testToDto() {
        // Act
        UserStoryDTO result = userStoryMapper.toDto(userStory);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userStory.getId());
        assertThat(result.getTitle()).isEqualTo(userStory.getTitle());
        assertThat(result.getDescription()).isEqualTo(userStory.getDescription());
        assertThat(result.getAcceptanceCriteria()).isEqualTo(userStory.getAcceptanceCriteria());
        assertThat(result.getStatus()).isEqualTo(userStory.getStatus());
        assertThat(result.getPriority()).isEqualTo(userStory.getPriority());
        assertThat(result.getStoryPoints()).isEqualTo(userStory.getStoryPoints());
        assertThat(result.getBusinessValue()).isEqualTo(userStory.getBusinessValue());
        assertThat(result.getEstimationType()).isEqualTo(userStory.getEstimationType());
        assertThat(result.getOptimisticEstimate()).isEqualTo(userStory.getOptimisticEstimate());
        assertThat(result.getPessimisticEstimate()).isEqualTo(userStory.getPessimisticEstimate());
        assertThat(result.getMostLikelyEstimate()).isEqualTo(userStory.getMostLikelyEstimate());
        assertThat(result.getPertEstimate()).isEqualTo(userStory.getPertEstimate());
        assertThat(result.getVariance()).isEqualTo(userStory.getVariance());
        
        // Verify relationship IDs are correctly mapped
        assertThat(result.getProjectId()).isEqualTo(project.getId());
        assertThat(result.getSprintId()).isEqualTo(sprint.getId());
        assertThat(result.getAssignedToId()).isEqualTo(assignedUser.getId());
    }

    @Test
    public void testToDto_WithNullRelationships() {
        // Arrange
        userStory.setProject(null);
        userStory.setSprint(null);
        userStory.setAssignedTo(null);

        // Act
        UserStoryDTO result = userStoryMapper.toDto(userStory);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isNull();
        assertThat(result.getSprintId()).isNull();
        assertThat(result.getAssignedToId()).isNull();
    }

    @Test
    public void testUpdateEntityFromDto() {
        // Arrange
        UserStory existingUserStory = new UserStory();
        existingUserStory.setId(1L);
        existingUserStory.setTitle("Old Title");
        existingUserStory.setDescription("Old Description");
        existingUserStory.setAcceptanceCriteria("Old Acceptance Criteria");
        existingUserStory.setStatus(StoryStatus.BACKLOG);
        existingUserStory.setPriority(Priority.LOW);
        existingUserStory.setStoryPoints(3);
        existingUserStory.setBusinessValue(5);
        existingUserStory.setEstimationType(EstimationType.STORY_POINTS);
        existingUserStory.setProject(project);
        existingUserStory.setSprint(null);
        existingUserStory.setAssignedTo(null);

        UserStoryDTO updateDTO = new UserStoryDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Description");
        updateDTO.setPriority(Priority.HIGH);
        updateDTO.setStoryPoints(8);
        updateDTO.setSprintId(sprint.getId());
        updateDTO.setAssignedToId(assignedUser.getId());

        // Act
        userStoryMapper.updateEntityFromDto(updateDTO, existingUserStory);

        // Assert
        assertThat(existingUserStory.getId()).isEqualTo(1L); // ID should not change
        assertThat(existingUserStory.getTitle()).isEqualTo("Updated Title");
        assertThat(existingUserStory.getDescription()).isEqualTo("Updated Description");
        assertThat(existingUserStory.getAcceptanceCriteria()).isEqualTo("Old Acceptance Criteria"); // Not updated
        assertThat(existingUserStory.getStatus()).isEqualTo(StoryStatus.BACKLOG); // Not updated
        assertThat(existingUserStory.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(existingUserStory.getStoryPoints()).isEqualTo(8);
        assertThat(existingUserStory.getBusinessValue()).isEqualTo(5); // Not updated
        assertThat(existingUserStory.getEstimationType()).isEqualTo(EstimationType.STORY_POINTS); // Not updated
        
        // Relationships should remain unchanged as they are ignored in the mapping
        // and are expected to be loaded via fetch joins in repository queries
        assertThat(existingUserStory.getProject()).isEqualTo(project); // Unchanged
        assertThat(existingUserStory.getSprint()).isNull(); // Unchanged
        assertThat(existingUserStory.getAssignedTo()).isNull(); // Unchanged
    }
}