package it.zenflow.controller;

import it.zenflow.config.multitenant.TenantContext;
import it.zenflow.model.project.Epic;
import it.zenflow.model.project.EpicRepository;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.ProjectRepository;
import it.zenflow.model.project.enums.EpicStatus;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.*;
import it.zenflow.service.ProjectService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class EpicControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private User ownerUser;
    private User teamMemberUser;
    private User outsiderUser;
    private User adminUser;
    private Role ownerRole;
    private Role teamRole;
    private Role adminRole;
    private Project project;

    @BeforeEach
    void setup() {
        TenantContext.setCurrentTenant("test");
        epicRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // permissions
        Permission readProject = permission("READ_PROJECT", "PROJECT");
        Permission createEpic = permission("CREATE_EPIC", "EPIC_MANAGEMENT");

        // roles
        ownerRole = new Role();
        ownerRole.setName("OWNER");
        ownerRole.setPermissions(new HashSet<>(List.of(readProject, createEpic)));
        ownerRole = roleRepository.save(ownerRole);

        teamRole = new Role();
        teamRole.setName("TEAM");
        teamRole.setPermissions(new HashSet<>(List.of(readProject, createEpic)));
        teamRole = roleRepository.save(teamRole);

        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setPermissions(new HashSet<>(List.of(readProject, createEpic)));
        adminRole = roleRepository.save(adminRole);

        // users
        ownerUser = user("owner", ownerRole);
        teamMemberUser = user("teammate", teamRole);
        outsiderUser = user("outsider", teamRole); // has permission but not in project
        adminUser = user("admin", adminRole);

        // project
        project = new Project();
        project.setName("Proj");
        project.setDescription("desc");
        project.setType(ProjectType.SCRUM);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setOwner(ownerUser);
        project.setTeamMembers(new HashSet<>());
        project = projectService.save(project);

        // add team member
        project.getTeamMembers().add(teamMemberUser);
        project = projectService.save(project);
    }

    private Permission permission(String name, String category) {
        Permission p = new Permission();
        p.setName(name);
        p.setCategory(category);
        p.setDescription(name);
        return permissionRepository.save(p);
    }

    private User user(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("pwd");
        u.setEnabled(true);
        u.setRoles(new HashSet<>(List.of(role)));
        return userRepository.save(u);
    }

    @Test
    @WithMockUser(username = "owner", authorities = {"CREATE_EPIC", "READ_PROJECT"})
    void newForm_visible_to_owner() throws Exception {
        mockMvc.perform(get("/projects/" + project.getId() + "/epics/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/epics/form"))
                .andExpect(model().attributeExists("project", "epicDTO", "statuses", "isNew"));
    }

    @Test
    @WithMockUser(username = "teammate", authorities = {"CREATE_EPIC", "READ_PROJECT"})
    void newForm_visible_to_team_member() throws Exception {
        mockMvc.perform(get("/projects/" + project.getId() + "/epics/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/epics/form"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"CREATE_EPIC", "READ_PROJECT", "ADMIN"})
    void newForm_visible_to_admin() throws Exception {
        mockMvc.perform(get("/projects/" + project.getId() + "/epics/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/epics/form"));
    }

    @Test
    @WithMockUser(username = "outsider", authorities = {"CREATE_EPIC", "READ_PROJECT"})
    void newForm_redirects_if_not_member_or_owner_and_not_admin() throws Exception {
        mockMvc.perform(get("/projects/" + project.getId() + "/epics/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + project.getId()));
    }

    @Test
    @WithMockUser(username = "owner", authorities = {"CREATE_EPIC", "READ_PROJECT"})
    void createEpic_validation_errors_return_form() throws Exception {
        mockMvc.perform(post("/projects/" + project.getId() + "/epics/new")
                        .param("projectId", String.valueOf(project.getId()))
                        // missing title
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/epics/form"))
                .andExpect(model().attributeHasFieldErrors("epicDTO", "title"));
    }

    @Test
    @WithMockUser(username = "owner", authorities = {"CREATE_EPIC", "READ_PROJECT"})
    void createEpic_success_persists_and_redirects() throws Exception {
        mockMvc.perform(post("/projects/" + project.getId() + "/epics/new")
                        .param("projectId", String.valueOf(project.getId()))
                        .param("title", "EP One")
                        .param("description", "d")
                        .param("startDate", LocalDate.now().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/" + project.getId()))
                .andExpect(flash().attributeExists("message"));

        List<Epic> epics = epicRepository.findByProject(project);
        assertEquals(1, epics.size());
        Epic e = epics.get(0);
        assertEquals("EP One", e.getTitle());
        assertEquals(project.getId(), e.getProject().getId());
        assertEquals(EpicStatus.PLANNED, e.getStatus());
        assertNotNull(e.getId());
    }
}
