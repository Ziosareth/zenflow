package it.zenflow.controller;

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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class UserStoryControllerI18nIntegrationTest {

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

    @Autowired
    private MessageSource messageSource;

    private User regularUser;
    private User teamMemberUser;
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
        Permission readProjectPermission = new Permission();
        readProjectPermission.setName("READ_PROJECT");
        readProjectPermission.setDescription("Permission to view projects");
        readProjectPermission.setCategory("PROJECT");
        readProjectPermission = permissionRepository.save(readProjectPermission);

        Permission readUserStoryPermission = new Permission();
        readUserStoryPermission.setName("READ_USER_STORY");
        readUserStoryPermission.setDescription("Permission to view user stories");
        readUserStoryPermission.setCategory("USER_STORY");
        readUserStoryPermission = permissionRepository.save(readUserStoryPermission);

        Permission createUserStoryPermission = new Permission();
        createUserStoryPermission.setName("CREATE_USER_STORY");
        createUserStoryPermission.setDescription("Permission to create user stories");
        createUserStoryPermission.setCategory("USER_STORY");
        createUserStoryPermission = permissionRepository.save(createUserStoryPermission);

        // Create role
        Role userRole = new Role();
        userRole.setName("USER");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(readProjectPermission);
        userPermissions.add(readUserStoryPermission);
        userPermissions.add(createUserStoryPermission);
        userRole.setPermissions(userPermissions);
        userRole = roleRepository.save(userRole);

        // Create users
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
    public void testUserStoryListPageInEnglish() throws Exception {
        // Get localized messages for English
        String userStoriesTitle = messageSource.getMessage("userstory.list", null, Locale.ENGLISH);

        mockMvc.perform(get("/projects/{projectId}/user-stories", testProject.getId())
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(userStoriesTitle)))
                .andExpect(content().string(containsString("Test User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER_STORY", "TENANT_test"})
    public void testUserStoryListPageInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        // Get localized messages for Italian
        String userStoriesTitle = messageSource.getMessage("userstory.list", null, Locale.ITALIAN);

        mockMvc.perform(get("/projects/{projectId}/user-stories", testProject.getId())
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(userStoriesTitle)))
                .andExpect(content().string(containsString("Test User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER_STORY", "TENANT_test"})
    public void testUserStoryDetailPageInEnglish() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories/{id}", testProject.getId(), testUserStory.getId())
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test User Story")))
                .andExpect(content().string(containsString("This is a test user story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER_STORY", "TENANT_test"})
    public void testUserStoryDetailPageInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        mockMvc.perform(get("/projects/{projectId}/user-stories/{id}", testProject.getId(), testUserStory.getId())
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test User Story")))
                .andExpect(content().string(containsString("This is a test user story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_USER_STORY", "TENANT_test"})
    public void testUserStoryFormPageInEnglish() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories/new", testProject.getId())
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_USER_STORY", "TENANT_test"})
    public void testUserStoryFormPageInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        mockMvc.perform(get("/projects/{projectId}/user-stories/new", testProject.getId())
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"UPDATE_USER_STORY", "TENANT_test"})
    public void testUserStoryEditFormPageInEnglish() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/user-stories/{id}/edit", testProject.getId(), testUserStory.getId())
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit User Story")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"UPDATE_USER_STORY", "TENANT_test"})
    public void testUserStoryEditFormPageInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        mockMvc.perform(get("/projects/{projectId}/user-stories/{id}/edit", testProject.getId(), testUserStory.getId())
                        .locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit User Story")));
    }
}
