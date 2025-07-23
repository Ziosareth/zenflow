package it.zenflow.controller;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.ProjectRepository;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.*;
import it.zenflow.facade.ProjectFacade;
import it.zenflow.service.ProjectService;
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
public class ProjectControllerI18nIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private ProjectFacade projectFacade;

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

    @BeforeEach
    public void setup() {
        // Clear existing data
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

        // Create role
        Role userRole = new Role();
        userRole.setName("USER");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(readProjectPermission);
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
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "TENANT_test"})
    public void testProjectListPageInEnglish() throws Exception {

        String projectsTitle = messageSource.getMessage("project.list.title", null, Locale.ENGLISH);
        String myProjects = messageSource.getMessage("project.my", null, Locale.ENGLISH);
        String newProject = messageSource.getMessage("project.new", null, Locale.ENGLISH);
        String viewAction = messageSource.getMessage("project.view", null, Locale.ENGLISH);
        
        mockMvc.perform(get("/projects")
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(projectsTitle)))
                .andExpect(content().string(containsString(myProjects)))
                .andExpect(content().string(containsString(newProject)))
                .andExpect(content().string(containsString(viewAction)));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "TENANT_test"})
    public void testProjectListPageInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);
        
        String projectsTitle = messageSource.getMessage("project.list.title", null, Locale.ITALIAN);
        String myProjects = messageSource.getMessage("project.my", null, Locale.ITALIAN);
        String newProject = messageSource.getMessage("project.new", null, Locale.ITALIAN);
        String viewAction = messageSource.getMessage("project.view", null, Locale.ITALIAN);
        
        mockMvc.perform(get("/projects").locale(Locale.ITALIAN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(projectsTitle)))
                .andExpect(content().string(containsString(myProjects)))
                .andExpect(content().string(containsString(newProject)))
                .andExpect(content().string(containsString(viewAction)));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "UPDATE_PROJECT", "TENANT_test"})
    public void testProjectDetailPageInEnglish() throws Exception {

        String projectDetails = messageSource.getMessage("project.details", null, Locale.ENGLISH);
        String projectMetrics = messageSource.getMessage("project.metrics", null, Locale.ENGLISH);
        String teamMembers = messageSource.getMessage("project.team.members", null, Locale.ENGLISH);
        String editAction = messageSource.getMessage("project.edit", null, Locale.ENGLISH);
        
        mockMvc.perform(get("/projects/{id}", testProject.getId())
                .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(projectDetails)))
                .andExpect(content().string(containsString(projectMetrics)))
                .andExpect(content().string(containsString(teamMembers)))
                .andExpect(content().string(containsString(editAction)));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "UPDATE_PROJECT", "TENANT_test"})
    public void testProjectDetailPageInItalian() throws Exception {
        LocaleContextHolder.setLocale(Locale.ITALIAN);
        
        String projectDetails = messageSource.getMessage("project.details", null, Locale.ITALIAN);
        String projectMetrics = messageSource.getMessage("project.metrics", null, Locale.ITALIAN);
        String teamMembers = messageSource.getMessage("project.team.members", null, Locale.ITALIAN);
        String editAction = messageSource.getMessage("project.edit", null, Locale.ITALIAN);
        
        mockMvc.perform(get("/projects/{id}", testProject.getId())
                        .param("lang", "it"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(projectDetails)))
                .andExpect(content().string(containsString(projectMetrics)))
                .andExpect(content().string(containsString(teamMembers)))
                .andExpect(content().string(containsString(editAction)));
    }
}