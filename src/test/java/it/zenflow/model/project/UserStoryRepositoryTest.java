package it.zenflow.model.project;

import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.RoleRepository;
import it.zenflow.model.rbac.User;
import it.zenflow.model.rbac.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserStoryRepositoryTest {

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Project project1;
    private Project project2;
    private User user1;
    private User user2;
    private UserStory userStory1;
    private UserStory userStory2;
    private UserStory userStory3;

    @BeforeEach
    public void setup() {
        // Clear existing data
        userStoryRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role role = new Role();
        role.setName("USER");
        role = roleRepository.save(role);

        // Create users
        user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user1.setRoles(roles);
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setEnabled(true);
        user2.setRoles(roles);
        user2 = userRepository.save(user2);

        // Create projects
        project1 = new Project();
        project1.setName("Project 1");
        project1.setDescription("This is project 1");
        project1.setStatus(ProjectStatus.ACTIVE);
        project1.setType(ProjectType.SCRUM);
        project1.setStartDate(LocalDate.now());
        project1.setEndDate(LocalDate.now().plusMonths(3));
        project1.setOwner(user1);
        Set<User> teamMembers = new HashSet<>();
        teamMembers.add(user2);
        project1.setTeamMembers(teamMembers);
        project1 = projectRepository.save(project1);

        project2 = new Project();
        project2.setName("Project 2");
        project2.setDescription("This is project 2");
        project2.setStatus(ProjectStatus.ACTIVE);
        project2.setType(ProjectType.KANBAN);
        project2.setStartDate(LocalDate.now());
        project2.setEndDate(LocalDate.now().plusMonths(6));
        project2.setOwner(user2);
        project2 = projectRepository.save(project2);

        // Create user stories
        userStory1 = new UserStory();
        userStory1.setTitle("User Story 1");
        userStory1.setDescription("This is user story 1");
        userStory1.setAcceptanceCriteria("User story 1 should be testable");
        userStory1.setStatus(StoryStatus.BACKLOG);
        userStory1.setPriority(Priority.HIGH);
        userStory1.setStoryPoints(5);
        userStory1.setBusinessValue(8);
        userStory1.setProject(project1);
        userStory1.setAssignedTo(user2);
        userStory1 = userStoryRepository.save(userStory1);

        userStory2 = new UserStory();
        userStory2.setTitle("User Story 2");
        userStory2.setDescription("This is user story 2");
        userStory2.setAcceptanceCriteria("User story 2 should be testable");
        userStory2.setStatus(StoryStatus.IN_PROGRESS);
        userStory2.setPriority(Priority.MEDIUM);
        userStory2.setStoryPoints(3);
        userStory2.setBusinessValue(5);
        userStory2.setProject(project1);
        userStory2 = userStoryRepository.save(userStory2);

        userStory3 = new UserStory();
        userStory3.setTitle("User Story 3");
        userStory3.setDescription("This is user story 3");
        userStory3.setAcceptanceCriteria("User story 3 should be testable");
        userStory3.setStatus(StoryStatus.BACKLOG);
        userStory3.setPriority(Priority.LOW);
        userStory3.setStoryPoints(2);
        userStory3.setBusinessValue(3);
        userStory3.setProject(project2);
        userStory3.setAssignedTo(user2);
        userStory3 = userStoryRepository.save(userStory3);
    }

    @Test
    public void testFindByProject() {
        List<UserStory> userStories = userStoryRepository.findByProject(project1);
        
        assertThat(userStories).hasSize(2);
        assertThat(userStories).extracting(UserStory::getTitle).containsExactlyInAnyOrder("User Story 1", "User Story 2");
    }

    @Test
    public void testFindByProjectAndStatus() {
        List<UserStory> userStories = userStoryRepository.findByProjectAndStatus(project1, StoryStatus.BACKLOG);
        
        assertThat(userStories).hasSize(1);
        assertThat(userStories.get(0).getTitle()).isEqualTo("User Story 1");
    }

    @Test
    public void testFindByAssignedTo() {
        List<UserStory> userStories = userStoryRepository.findByAssignedTo(user2);
        
        assertThat(userStories).hasSize(2);
        assertThat(userStories).extracting(UserStory::getTitle).containsExactlyInAnyOrder("User Story 1", "User Story 3");
    }

    @Test
    public void testFindByProjectId() {
        List<UserStory> userStories = userStoryRepository.findByProjectId(project1.getId());
        
        assertThat(userStories).hasSize(2);
        assertThat(userStories).extracting(UserStory::getTitle).containsExactlyInAnyOrder("User Story 1", "User Story 2");
    }

    @Test
    public void testFindByProjectIdAndStatus() {
        List<UserStory> userStories = userStoryRepository.findByProjectIdAndStatus(project1.getId(), StoryStatus.IN_PROGRESS);
        
        assertThat(userStories).hasSize(1);
        assertThat(userStories.get(0).getTitle()).isEqualTo("User Story 2");
    }

    @Test
    public void testSaveUserStory() {
        UserStory newUserStory = new UserStory();
        newUserStory.setTitle("New User Story");
        newUserStory.setDescription("This is a new user story");
        newUserStory.setAcceptanceCriteria("New user story should be testable");
        newUserStory.setStatus(StoryStatus.BACKLOG);
        newUserStory.setPriority(Priority.MEDIUM);
        newUserStory.setStoryPoints(3);
        newUserStory.setBusinessValue(5);
        newUserStory.setProject(project2);
        
        UserStory savedUserStory = userStoryRepository.save(newUserStory);
        
        assertThat(savedUserStory.getId()).isNotNull();
        assertThat(savedUserStory.getTitle()).isEqualTo("New User Story");
        
        // Verify it was actually saved to the database
        UserStory retrievedUserStory = userStoryRepository.findById(savedUserStory.getId()).orElse(null);
        assertThat(retrievedUserStory).isNotNull();
        assertThat(retrievedUserStory.getTitle()).isEqualTo("New User Story");
    }

    @Test
    public void testUpdateUserStory() {
        userStory1.setTitle("Updated User Story");
        userStory1.setStatus(StoryStatus.DONE);
        
        UserStory updatedUserStory = userStoryRepository.save(userStory1);
        
        assertThat(updatedUserStory.getTitle()).isEqualTo("Updated User Story");
        assertThat(updatedUserStory.getStatus()).isEqualTo(StoryStatus.DONE);
        
        // Verify it was actually updated in the database
        UserStory retrievedUserStory = userStoryRepository.findById(userStory1.getId()).orElse(null);
        assertThat(retrievedUserStory).isNotNull();
        assertThat(retrievedUserStory.getTitle()).isEqualTo("Updated User Story");
        assertThat(retrievedUserStory.getStatus()).isEqualTo(StoryStatus.DONE);
    }

    @Test
    public void testDeleteUserStory() {
        userStoryRepository.delete(userStory1);
        
        // Verify it was actually deleted from the database
        assertThat(userStoryRepository.findById(userStory1.getId())).isEmpty();
        
        // Verify other user stories still exist
        assertThat(userStoryRepository.findAll()).hasSize(2);
    }
}