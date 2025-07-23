package it.zenflow.controller;

import it.zenflow.config.multitenant.TenantContext;
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
public class ProjectControllerIntegrationTest {

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

    private User adminUser;
    private User regularUser;
    private User teamMemberUser;
    private Role adminRole;
    private Role userRole;
    private Project testProject;

    @BeforeEach
    public void setup() {
        TenantContext.setCurrentTenant("test");
        // Clear existing data
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

        Permission updateProjectPermission = new Permission();
        updateProjectPermission.setName("UPDATE_PROJECT");
        updateProjectPermission.setDescription("Permission to edit projects");
        updateProjectPermission.setCategory("PROJECT");
        updateProjectPermission = permissionRepository.save(updateProjectPermission);

        Permission deleteProjectPermission = new Permission();
        deleteProjectPermission.setName("DELETE_PROJECT");
        deleteProjectPermission.setDescription("Permission to delete projects");
        deleteProjectPermission.setCategory("PROJECT");
        deleteProjectPermission = permissionRepository.save(deleteProjectPermission);

        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(createProjectPermission);
        adminPermissions.add(readProjectPermission);
        adminPermissions.add(updateProjectPermission);
        adminPermissions.add(deleteProjectPermission);
        adminRole.setPermissions(adminPermissions);
        adminRole = roleRepository.save(adminRole);

        userRole = new Role();
        userRole.setName("USER");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(readProjectPermission);
        userPermissions.add(createProjectPermission);
        userPermissions.add(updateProjectPermission);
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
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "TENANT_test"})
    public void testListProjects() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/list"))
                .andExpect(model().attributeExists("projects"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalItems"))
                .andExpect(model().attributeExists("pageSize"))
                .andExpect(model().attributeExists("sortField"))
                .andExpect(content().string(containsString("Test Project")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "TENANT_test"})
    public void testViewProject() throws Exception {
        mockMvc.perform(get("/projects/{id}", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/detail"))
                .andExpect(model().attributeExists("project"))
                .andExpect(model().attribute("project", hasProperty("name", is("Test Project"))))
                .andExpect(model().attribute("project", hasProperty("description", is("This is a test project"))))
                .andExpect(model().attribute("isOwner", is(true)))
                .andExpect(content().string(containsString("Test Project")));
    }

    @Test
    @WithMockUser(username = "teammember", authorities = {"READ_PROJECT", "TENANT_test"})
    public void testViewProjectAsTeamMember() throws Exception {
        mockMvc.perform(get("/projects/{id}", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/detail"))
                .andExpect(model().attributeExists("project"))
                .andExpect(model().attribute("project", hasProperty("name", is("Test Project"))))
                .andExpect(model().attribute("isOwner", is(false)))
                .andExpect(model().attribute("isTeamMember", is(true)))
                .andExpect(content().string(containsString("Test Project")));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_PROJECT", "TENANT_test"})
    public void testNewProjectForm() throws Exception {
        mockMvc.perform(get("/projects/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/form"))
                .andExpect(model().attributeExists("projectDTO"))
                .andExpect(model().attributeExists("allUsers"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attribute("isNew", is(true)));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CREATE_PROJECT", "TENANT_test"})
    public void testCreateProject() throws Exception {
        mockMvc.perform(post("/projects/new")
                        .param("name", "New Test Project")
                        .param("description", "This is a new test project")
                        .param("status", "ACTIVE")
                        .param("type", "KANBAN")
                        .param("teamMemberIds", teamMemberUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"))
                .andExpect(flash().attributeExists("message"));

        // Verify the project was created
        // Find the project by listing all projects and finding the one with the expected name
        Project newProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("New Test Project"))
                .findFirst()
                .orElse(null);
        assert newProject != null;
        assert newProject.getName().equals("New Test Project");
        assert newProject.getDescription().equals("This is a new test project");
        assert newProject.getStatus() == ProjectStatus.ACTIVE;
        assert newProject.getType() == ProjectType.KANBAN;
        assert newProject.getOwner().getId().equals(regularUser.getId());
        assert newProject.getTeamMembers().size() == 1;
        assert newProject.getTeamMembers().iterator().next().getId().equals(teamMemberUser.getId());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"UPDATE_PROJECT", "TENANT_test"})
    public void testEditProjectForm() throws Exception {
        mockMvc.perform(get("/projects/{id}/edit", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/form"))
                .andExpect(model().attributeExists("projectDTO"))
                .andExpect(model().attributeExists("allUsers"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attribute("isNew", is(false)))
                .andExpect(model().attribute("projectDTO", hasProperty("name", is("Test Project"))))
                .andExpect(model().attribute("projectDTO", hasProperty("description", is("This is a test project"))));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"UPDATE_PROJECT", "TENANT_test"})
    public void testUpdateProject() throws Exception {
        mockMvc.perform(post("/projects/{id}/edit", testProject.getId())
                        .param("name", "Updated Test Project")
                        .param("description", "This is an updated test project")
                        .param("status", "ON_HOLD")
                        .param("type", "KANBAN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + testProject.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the project was updated
        Project updatedProject = projectService.findById(testProject.getId()).orElseThrow();
        assert updatedProject.getName().equals("Updated Test Project");
        assert updatedProject.getDescription().equals("This is an updated test project");
        assert updatedProject.getStatus() == ProjectStatus.ON_HOLD;
        assert updatedProject.getType() == ProjectType.KANBAN;
    }

    @Test
    @WithMockUser(username = "user", authorities = {"DELETE_PROJECT", "TENANT_test"})
    public void testDeleteProject() throws Exception {
        mockMvc.perform(post("/projects/{id}/delete", testProject.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"))
                .andExpect(flash().attributeExists("message"));

        // Verify the project was deleted
        assert projectService.findById(testProject.getId()).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"DELETE_PROJECT", "ADMIN", "TENANT_test"})
    public void testAdminCanDeleteAnyProject() throws Exception {
        mockMvc.perform(post("/projects/{id}/delete", testProject.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"))
                .andExpect(flash().attributeExists("message"));

        // Verify the project was deleted
        assert projectService.findById(testProject.getId()).isEmpty();
    }

    @Test
    @WithMockUser(username = "teammember", authorities = {"DELETE_PROJECT", "TENANT_test"})
    public void testTeamMemberCannotDeleteProject() throws Exception {
        mockMvc.perform(post("/projects/{id}/delete", testProject.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"));

        // Verify the project was not deleted
        assert projectService.findById(testProject.getId()).isPresent();
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_PROJECT", "TENANT_test"})
    public void testMyProjects() throws Exception {
        mockMvc.perform(get("/projects/my"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/list"))
                .andExpect(model().attributeExists("projects"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attribute("isMyProjects", is(true)))
                .andExpect(content().string(containsString("Test Project")));
    }

    @Test
    public void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
