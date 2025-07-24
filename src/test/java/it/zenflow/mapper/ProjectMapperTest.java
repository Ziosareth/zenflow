package it.zenflow.mapper;

import it.zenflow.dto.ProjectDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the ProjectMapper implementation.
 */
@SpringBootTest
public class ProjectMapperTest {

    @Autowired
    private ProjectMapper projectMapper;

    private ProjectDTO projectDTO;
    private Project project;
    private User owner;
    private User teamMember1;
    private User teamMember2;
    private Set<User> teamMembers;

    @BeforeEach
    public void setup() {
        // Create test users
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setEmail("owner@example.com");

        teamMember1 = new User();
        teamMember1.setId(2L);
        teamMember1.setUsername("member1");
        teamMember1.setEmail("member1@example.com");

        teamMember2 = new User();
        teamMember2.setId(3L);
        teamMember2.setUsername("member2");
        teamMember2.setEmail("member2@example.com");

        // Create team members set
        teamMembers = new HashSet<>();
        teamMembers.add(teamMember1);
        teamMembers.add(teamMember2);

        // Create test DTO
        projectDTO = new ProjectDTO();
        projectDTO.setId(1L);
        projectDTO.setName("Test Project");
        projectDTO.setDescription("This is a test project");
        projectDTO.setStatus(ProjectStatus.ACTIVE);
        projectDTO.setType(ProjectType.SCRUM);
        projectDTO.setStartDate(LocalDate.of(2025, 1, 1));
        projectDTO.setEndDate(LocalDate.of(2025, 12, 31));
        Set<Long> teamMemberIds = new HashSet<>();
        teamMemberIds.add(teamMember1.getId());
        teamMemberIds.add(teamMember2.getId());
        projectDTO.setTeamMemberIds(teamMemberIds);

        // Create test entity
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("This is a test project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setType(ProjectType.SCRUM);
        project.setStartDate(LocalDate.of(2025, 1, 1));
        project.setEndDate(LocalDate.of(2025, 12, 31));
        project.setOwner(owner);
        project.setTeamMembers(teamMembers);
        project.setTotalStoryPoints(10);
        project.setCompletedStoryPoints(5);
        project.setTeamVelocity(2.5);
    }

    @Test
    public void testToEntity() {
        // Act
        Project result = projectMapper.toEntity(projectDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(projectDTO.getId());
        assertThat(result.getName()).isEqualTo(projectDTO.getName());
        assertThat(result.getDescription()).isEqualTo(projectDTO.getDescription());
        assertThat(result.getStatus()).isEqualTo(projectDTO.getStatus());
        assertThat(result.getType()).isEqualTo(projectDTO.getType());
        assertThat(result.getStartDate()).isEqualTo(projectDTO.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(projectDTO.getEndDate());
        
        // Verify ignored fields are not set
        assertThat(result.getOwner()).isNull();
        assertThat(result.getTeamMembers()).isEmpty();
        assertThat(result.getSprints()).isEmpty();
        assertThat(result.getBacklog()).isEmpty();
        assertThat(result.getTotalStoryPoints()).isEqualTo(0);
        assertThat(result.getCompletedStoryPoints()).isEqualTo(0);
        assertThat(result.getTeamVelocity()).isEqualTo(0.0);
    }

    @Test
    public void testToDto() {
        // Act
        ProjectDTO result = projectMapper.toDto(project);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(project.getId());
        assertThat(result.getName()).isEqualTo(project.getName());
        assertThat(result.getDescription()).isEqualTo(project.getDescription());
        assertThat(result.getStatus()).isEqualTo(project.getStatus());
        assertThat(result.getType()).isEqualTo(project.getType());
        assertThat(result.getStartDate()).isEqualTo(project.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(project.getEndDate());
        
        // Verify team member IDs are correctly mapped
        assertThat(result.getTeamMemberIds()).hasSize(2);
        assertThat(result.getTeamMemberIds()).contains(teamMember1.getId(), teamMember2.getId());
    }

    @Test
    public void testToDto_WithNullTeamMembers() {
        // Arrange
        project.setTeamMembers(null);

        // Act
        ProjectDTO result = projectMapper.toDto(project);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTeamMemberIds()).isEmpty();
    }

    @Test
    public void testUpdateEntityFromDto() {
        // Arrange
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setName("Old Name");
        existingProject.setDescription("Old Description");
        existingProject.setStatus(ProjectStatus.ON_HOLD);
        existingProject.setType(ProjectType.KANBAN);
        existingProject.setStartDate(LocalDate.of(2024, 1, 1));
        existingProject.setEndDate(LocalDate.of(2024, 12, 31));
        existingProject.setOwner(owner);
        existingProject.setTeamMembers(teamMembers);
        existingProject.setTotalStoryPoints(20);
        existingProject.setCompletedStoryPoints(10);
        existingProject.setTeamVelocity(5.0);

        ProjectDTO updateDTO = new ProjectDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setDescription("Updated Description");
        updateDTO.setStatus(ProjectStatus.COMPLETED);
        updateDTO.setType(ProjectType.SCRUM);
        updateDTO.setStartDate(LocalDate.of(2025, 6, 1));
        updateDTO.setEndDate(LocalDate.of(2026, 5, 31));

        // Act
        projectMapper.updateEntityFromDto(updateDTO, existingProject);

        // Assert
        assertThat(existingProject.getId()).isEqualTo(1L); // ID should not change
        assertThat(existingProject.getName()).isEqualTo("Updated Name");
        assertThat(existingProject.getDescription()).isEqualTo("Updated Description");
        assertThat(existingProject.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(existingProject.getType()).isEqualTo(ProjectType.SCRUM);
        assertThat(existingProject.getStartDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(existingProject.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        
        // Verify ignored fields are not changed
        assertThat(existingProject.getOwner()).isEqualTo(owner);
        assertThat(existingProject.getTeamMembers()).isEqualTo(teamMembers);
        assertThat(existingProject.getTotalStoryPoints()).isEqualTo(20);
        assertThat(existingProject.getCompletedStoryPoints()).isEqualTo(10);
        assertThat(existingProject.getTeamVelocity()).isEqualTo(5.0);
    }

    @Test
    public void testUpdateEntityFromDto_WithNullValues() {
        // Arrange
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setName("Old Name");
        existingProject.setDescription("Old Description");
        existingProject.setStatus(ProjectStatus.ON_HOLD);
        existingProject.setType(ProjectType.KANBAN);
        existingProject.setStartDate(LocalDate.of(2024, 1, 1));
        existingProject.setEndDate(LocalDate.of(2024, 12, 31));

        ProjectDTO updateDTO = new ProjectDTO();
        updateDTO.setName("Updated Name");
        // Other fields are not explicitly set, but have default values due to initialization in ProjectDTO

        // Act
        projectMapper.updateEntityFromDto(updateDTO, existingProject);

        // Assert
        assertThat(existingProject.getId()).isEqualTo(1L);
        assertThat(existingProject.getName()).isEqualTo("Updated Name");
        // Description should remain unchanged as it has no default value in ProjectDTO
        assertThat(existingProject.getDescription()).isEqualTo("Old Description");
        // Status and Type will be updated to the default values from ProjectDTO
        assertThat(existingProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE); // Default in ProjectDTO
        assertThat(existingProject.getType()).isEqualTo(ProjectType.SCRUM); // Default in ProjectDTO
        // Dates should remain unchanged as they have no default values in ProjectDTO
        assertThat(existingProject.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(existingProject.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }
}