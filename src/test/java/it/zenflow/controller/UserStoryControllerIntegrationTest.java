package it.zenflow.controller;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.ProjectRepository;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.UserStoryRepository;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.*;
import it.zenflow.service.ProjectService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class UserStoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserStoryService userStoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private User adminUser;
    private User regularUser;
    private User teamMemberUser;
    private Role adminRole;
    private Role userRole;
    private Project testProject;
    private UserStory testUserStory;

    @BeforeEach
    public void setup() {
        // Clear existing data
        userStoryRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create permissions
        Permission createProjectPermission = new Permission();
        createProjectPermission.setName("CREATE_PROJECT");
        createProjectPermission.setDescription("Permission to create projects");
        createProjectPermission.setCategory("PROJECT");
        createProjectPermission = permissionRepository.save(createProjectPermission);

        Permission readProjectPermission = new Permission();
        readProjectPermission.setName("READ_PROJECT");
        readProjectPermission.setDescription("Permission to view projects");
        readProjectPermission.setCategory("PROJECT");
        readProjectPermission = permissionRepository.save(readProjectPermission);

        Permission createUserStoryPermission = new Permission();
        createUserStoryPermission.setName("CREATE_USER_STORY");
        createUserStoryPermission.setDescription("Permission to create user stories");
        createUserStoryPermission.setCategory("USER_STORY");
        createUserStoryPermission = permissionRepository.save(createUserStoryPermission);

        Permission readUserStoryPermission = new Permission();
        readUserStoryPermission.setName("READ_USER_STORY");
        readUserStoryPermission.setDescription("Permission to view user stories");
        readUserStoryPermission.setCategory("USER_STORY");
        readUserStoryPermission = permissionRepository.save(readUserStoryPermission);

        Permission updateUserStoryPermission = new Permission();
        updateUserStoryPermission.setName("UPDATE_USER_STORY");
        updateUserStoryPermission.setDescription("Permission to edit user stories");
        updateUserStoryPermission.setCategory("USER_STORY");
        updateUserStoryPermission = permissionRepository.save(updateUserStoryPermission);

        Permission deleteUserStoryPermission = new Permission();
        deleteUserStoryPermission.setName("DELETE_USER_STORY");
        deleteUserStoryPermission.setDescription("Permission to delete user stories");
        deleteUserStoryPermission.setCategory("USER_STORY");
        deleteUserStoryPermission = permissionRepository.save(deleteUserStoryPermission);

        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(createProjectPermission);
        adminPermissions.add(readProjectPermission);
        adminPermissions.add(createUserStoryPermission);
        adminPermissions.add(readUserStoryPermission);
        adminPermissions.add(updateUserStoryPermission);
        adminPermissions.add(deleteUserStoryPermission);
        adminRole.setPermissions(adminPermissions);
        adminRole = roleRepository.save(adminRole);

        userRole = new Role();
        userRole.setName("USER");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(readProjectPermission);
        userPermissions.add(createUserStoryPermission);
        userPermissions.add(readUserStoryPermission);
        userPermissions.add(updateUserStoryPermission);
        userRole.setPermissions(userPermissions);
        userRole = roleRepository.save(userRole);

        // Create users
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password");
        adminUser.setEnabled(true);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminUser.setRoles(adminRoles);
        adminUser = userService.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setPassword("password");
        regularUser.setEnabled(true);
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        regularUser.setRoles(userRoles);
        regularUser = userService.save(regularUser);

        teamMemberUser = new User();
        teamMemberUser.setUsername("teammember");
        teamMemberUser.setEmail("teammember@example.com");
        teamMemberUser.setPassword("password");
        teamMemberUser.setEnabled(true);
        teamMemberUser.setRoles(userRoles);
        teamMemberUser = userService.save(teamMemberUser);

        // Create a test project
        testProject = new Project();
        testProject.setName("Test Project");
        testProject.setDescription("This is a test project");
        testProject.setStatus(ProjectStatus.ACTIVE);
        testProject.setType(ProjectType.SCRUM);
        testProject.setStartDate(LocalDate.now());
        testProject.setEndDate(LocalDate.now().plusMonths(3));
        testProject.setOwner(regularUser);

        Set<User> teamMembers = new HashSet<>();
        teamMembers.add(teamMemberUser);
        testProject.setTeamMembers(teamMembers);

        testProject = projectService.save(testProject);

        // Create a test user story
        testUserStory = new UserStory();
        testUserStory.setTitle("Test User Story");
        testUserStory.setDescription("This is a test user story");
        testUserStory.setAcceptanceCriteria("The user story should be testable");
        testUserStory.setStatus(StoryStatus.BACKLOG);
        testUserStory.setPriority(Priority.MEDIUM);
        testUserStory.setStoryPoints(5);
        testUserStory.setBusinessValue(8);
        testUserStory.setProject(testProject);
        testUserStory.setAssignedTo(teamMemberUser);
        testUserStory.setOptimisticEstimate(3.0);
        testUserStory.setMostLikelyEstimate(5.0);
        testUserStory.setPessimisticEstimate(8.0);
        testUserStory = userStoryService.save(testUserStory);
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER_STORY", "TENANT_test"})
    public void testListUserStories() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/user-stories/list"))
                .andExpect(model().attributeExists("project"))
                .andExpect(model().attributeExists("userStories"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("isOwner"))
                .andExpect(model().attributeExists("isTeamMember"))
                .andExpect(content().string(containsString("Test User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER_STORY", "TENANT_test"})
    public void testViewUserStory() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories/{id}", testProject.getId(), testUserStory.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/user-stories/detail"))
                .andExpect(model().attributeExists("project"))
                .andExpect(model().attributeExists("userStory"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("isOwner"))
                .andExpect(model().attributeExists("isTeamMember"))
                .andExpect(model().attribute("userStory", hasProperty("title", is("Test User Story"))))
                .andExpect(model().attribute("userStory", hasProperty("description", is("This is a test user story"))))
                .andExpect(content().string(containsString("Test User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_USER_STORY", "TENANT_test"})
    public void testNewUserStoryForm() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories/new", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/user-stories/form"))
                .andExpect(model().attributeExists("project"))
                .andExpect(model().attributeExists("userStoryDTO"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("priorities"))
                .andExpect(model().attributeExists("teamMembers"))
                .andExpect(model().attribute("isNew", is(true)));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_USER_STORY", "TENANT_test"})
    public void testCreateUserStory() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/user-stories/new", testProject.getId())
                        .param("title", "New Test User Story")
                        .param("description", "This is a new test user story")
                        .param("acceptanceCriteria", "The new user story should be testable")
                        .param("status", "BACKLOG")
                        .param("priority", "HIGH")
                        .param("storyPoints", "8")
                        .param("businessValue", "13")
                        .param("assignedToId", teamMemberUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId() + "/user-stories"))
                .andExpect(flash().attributeExists("message"));

        // Verify the user story was created
        UserStory newUserStory = userStoryRepository.findAll().stream()
                .filter(us -> us.getTitle().equals("New Test User Story"))
                .findFirst()
                .orElse(null);
        assert newUserStory != null;
        assert newUserStory.getTitle().equals("New Test User Story");
        assert newUserStory.getDescription().equals("This is a new test user story");
        assert newUserStory.getAcceptanceCriteria().equals("The new user story should be testable");
        assert newUserStory.getStatus() == StoryStatus.BACKLOG;
        assert newUserStory.getPriority() == Priority.HIGH;
        assert newUserStory.getStoryPoints() == 8;
        assert newUserStory.getBusinessValue() == 13;
        assert newUserStory.getProject().getId().equals(testProject.getId());
        assert newUserStory.getAssignedTo().getId().equals(teamMemberUser.getId());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"UPDATE_USER_STORY", "TENANT_test"})
    public void testEditUserStoryForm() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories/{id}/edit", testProject.getId(), testUserStory.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/user-stories/form"))
                .andExpect(model().attributeExists("project"))
                .andExpect(model().attributeExists("userStoryDTO"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("priorities"))
                .andExpect(model().attributeExists("teamMembers"))
                .andExpect(model().attribute("isNew", is(false)))
                .andExpect(model().attribute("userStoryDTO", hasProperty("title", is("Test User Story"))))
                .andExpect(model().attribute("userStoryDTO", hasProperty("description", is("This is a test user story"))));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"UPDATE_USER_STORY", "TENANT_test"})
    public void testUpdateUserStory() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/user-stories/{id}/edit", testProject.getId(), testUserStory.getId())
                        .param("title", "Updated Test User Story")
                        .param("description", "This is an updated test user story")
                        .param("acceptanceCriteria", "The updated user story should be testable")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH")
                        .param("storyPoints", "8")
                        .param("businessValue", "13")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId() + "/user-stories/" + testUserStory.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the user story was updated
        UserStory updatedUserStory = userStoryService.findById(testUserStory.getId()).orElseThrow();
        assert updatedUserStory.getTitle().equals("Updated Test User Story");
        assert updatedUserStory.getDescription().equals("This is an updated test user story");
        assert updatedUserStory.getAcceptanceCriteria().equals("The updated user story should be testable");
        assert updatedUserStory.getStatus() == StoryStatus.IN_PROGRESS;
        assert updatedUserStory.getPriority() == Priority.HIGH;
        assert updatedUserStory.getStoryPoints() == 8;
        assert updatedUserStory.getBusinessValue() == 13;
    }

    @Test
    @WithMockUser(username = "user", authorities = {"DELETE_USER_STORY", "TENANT_test"})
    public void testDeleteUserStory() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/user-stories/{id}/delete", testProject.getId(), testUserStory.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId() + "/user-stories"))
                .andExpect(flash().attributeExists("message"));

        // Verify the user story was deleted
        assert userStoryService.findById(testUserStory.getId()).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"DELETE_USER_STORY", "ADMIN", "TENANT_test"})
    public void testAdminCanDeleteAnyUserStory() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/user-stories/{id}/delete", testProject.getId(), testUserStory.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId() + "/user-stories"))
                .andExpect(flash().attributeExists("message"));

        // Verify the user story was deleted
        assert userStoryService.findById(testUserStory.getId()).isEmpty();
    }

    @Test
    @WithMockUser(username = "teammember", authorities = {"DELETE_USER_STORY", "TENANT_test"})
    public void testTeamMemberCannotDeleteUserStory() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/user-stories/{id}/delete", testProject.getId(), testUserStory.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId() + "/user-stories"));

        // Verify the user story was not deleted
        assert userStoryService.findById(testUserStory.getId()).isPresent();
    }

    @Test
    public void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories", testProject.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"TENANT_test"})
    public void testAccessWithoutRequiredAuthority() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories", testProject.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_USER_STORY", "TENANT_test"})
    public void testCreateUserStoryWithPERTEstimation() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/user-stories/new", testProject.getId())
                        .param("title", "PERT Test User Story")
                        .param("description", "This is a PERT test user story")
                        .param("acceptanceCriteria", "The PERT user story should be testable")
                        .param("status", "BACKLOG")
                        .param("priority", "MEDIUM")
                        .param("estimationType", "PERT")
                        .param("optimisticEstimate", "3.0")
                        .param("mostLikelyEstimate", "5.0")
                        .param("pessimisticEstimate", "9.0")
                        .param("businessValue", "8")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId() + "/user-stories"))
                .andExpect(flash().attributeExists("message"));

        // Verify the user story was created with correct PERT values and variance
        UserStory newUserStory = userStoryRepository.findAll().stream()
                .filter(us -> us.getTitle().equals("PERT Test User Story"))
                .findFirst()
                .orElse(null);
        assert newUserStory != null;
        assert newUserStory.getTitle().equals("PERT Test User Story");
        assert newUserStory.getEstimationType() == it.zenflow.model.project.enums.EstimationType.PERT;
        assert newUserStory.getOptimisticEstimate() == 3.0;
        assert newUserStory.getMostLikelyEstimate() == 5.0;
        assert newUserStory.getPessimisticEstimate() == 9.0;

        // Verify PERT estimate: (3 + 4*5 + 9) / 6 = 5.33
        double expectedPertEstimate = (3.0 + 4*5.0 + 9.0) / 6.0;
        assert Math.abs(newUserStory.getPertEstimate() - expectedPertEstimate) < 0.01;

        // Verify variance: ((9 - 3) / 6)² = 1.0
        double expectedVariance = Math.pow((9.0 - 3.0) / 6.0, 2);
        assert Math.abs(newUserStory.getVariance() - expectedVariance) < 0.01;

        // Verify story points (rounded PERT estimate): round(5.33) = 5
        assert newUserStory.getStoryPoints() == 5;
    }

    @Test
    public void testUserStoryVarianceCalculation() {
        // Create a user story with PERT estimation
        UserStory pertUserStory = new UserStory();
        pertUserStory.setTitle("Variance Test User Story");
        pertUserStory.setDescription("This is a variance test user story");
        pertUserStory.setAcceptanceCriteria("The variance user story should be testable");
        pertUserStory.setStatus(StoryStatus.BACKLOG);
        pertUserStory.setPriority(Priority.MEDIUM);
        pertUserStory.setProject(testProject);
        pertUserStory.setEstimationType(it.zenflow.model.project.enums.EstimationType.PERT);
        pertUserStory.setOptimisticEstimate(2.0);
        pertUserStory.setMostLikelyEstimate(4.0);
        pertUserStory.setPessimisticEstimate(10.0);
        pertUserStory = userStoryService.save(pertUserStory);

        // Expected values
        double expectedPertEstimate = (2.0 + 4*4.0 + 10.0) / 6.0; // 4.67
        double expectedVariance = Math.pow((10.0 - 2.0) / 6.0, 2); // 1.78

        // Verify the user story has the correct PERT values and variance
        UserStory savedUserStory = userStoryService.findById(pertUserStory.getId()).orElseThrow();
        assert savedUserStory != null;
        assert savedUserStory.getTitle().equals("Variance Test User Story");
        assert savedUserStory.getEstimationType() == it.zenflow.model.project.enums.EstimationType.PERT;
        assert savedUserStory.getOptimisticEstimate() == 2.0;
        assert savedUserStory.getMostLikelyEstimate() == 4.0;
        assert savedUserStory.getPessimisticEstimate() == 10.0;
        assert Math.abs(savedUserStory.getPertEstimate() - expectedPertEstimate) < 0.01;
        assert Math.abs(savedUserStory.getVariance() - expectedVariance) < 0.01;

        // Verify story points (rounded PERT estimate): round(4.67) = 5
        assert savedUserStory.getStoryPoints() == 5;
    }
}
