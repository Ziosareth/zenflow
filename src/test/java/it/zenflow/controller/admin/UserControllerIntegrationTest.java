package it.zenflow.controller.admin;

import it.zenflow.model.rbac.Permission;
import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.User;
import it.zenflow.model.rbac.UserRepository;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private User adminUser;
    private User regularUser;
    private User techleadUser;
    private Role adminRole;
    private Role userRole;
    private Role techleadRole;

    @Autowired
    private it.zenflow.model.rbac.RoleRepository roleRepository;

    @Autowired
    private it.zenflow.model.rbac.PermissionRepository permissionRepository;

    @BeforeEach
    public void setup() {
        // Clear existing data
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create permissions
        Permission readUserPermission = new Permission();
        readUserPermission.setName("READ_USER");
        readUserPermission.setDescription("Permission to view user details");
        readUserPermission.setCategory("USER_MANAGEMENT");
        readUserPermission = permissionRepository.save(readUserPermission);

        Permission updateUserPermission = new Permission();
        updateUserPermission.setName("UPDATE_USER");
        updateUserPermission.setDescription("Permission to edit user details");
        updateUserPermission.setCategory("USER_MANAGEMENT");
        updateUserPermission = permissionRepository.save(updateUserPermission);

        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(readUserPermission);
        adminPermissions.add(updateUserPermission);
        adminRole.setPermissions(adminPermissions);
        adminRole = roleRepository.save(adminRole);

        userRole = new Role();
        userRole.setName("USER");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(readUserPermission);
        userRole.setPermissions(userPermissions);
        userRole = roleRepository.save(userRole);

        techleadRole = new Role();
        techleadRole.setName("TECHLEAD");
        Set<Permission> techleadPermissions = new HashSet<>();
        techleadPermissions.add(readUserPermission);
        techleadPermissions.add(updateUserPermission);
        techleadRole.setPermissions(techleadPermissions);
        techleadRole = roleRepository.save(techleadRole);

        // Create users
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password");
        adminUser.setEnabled(true);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminUser.setRoles(adminRoles);

        techleadUser = new User();
        techleadUser.setUsername("techlead");
        techleadUser.setEmail("techlead@example.com");
        techleadUser.setPassword("password");
        techleadUser.setEnabled(true);
        Set<Role> techleadRoles = new HashSet<>();
        techleadRoles.add(techleadRole);
        techleadUser.setRoles(techleadRoles);

        regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setPassword("password");
        regularUser.setEnabled(true);
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        regularUser.setRoles(userRoles);

        // Save users
        adminUser = userService.save(adminUser);
        techleadUser = userService.save(techleadUser);
        regularUser = userService.save(regularUser);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testListUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalItems"))
                .andExpect(model().attributeExists("pageSize"))
                .andExpect(model().attributeExists("sortField"))
                .andExpect(content().string(containsString("admin@example.com")))
                .andExpect(content().string(containsString("techlead@example.com")))
                .andExpect(content().string(containsString("user@example.com")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER"})
    public void testViewUser() throws Exception {
        mockMvc.perform(get("/admin/users/{id}", regularUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-detail"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", hasProperty("username", is("user"))))
                .andExpect(model().attribute("user", hasProperty("email", is("user@example.com"))))
                .andExpect(content().string(containsString("user@example.com")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER"})
    public void testViewNonExistentUser() throws Exception {
        mockMvc.perform(get("/admin/users/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER"})
    public void testEditUserForm() throws Exception {
        mockMvc.perform(get("/admin/users/{id}/edit", regularUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("allRoles"))
                .andExpect(model().attribute("user", hasProperty("username", is("user"))))
                .andExpect(model().attribute("user", hasProperty("email", is("user@example.com"))))
                .andExpect(content().string(containsString("user@example.com")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testUpdateUser() throws Exception {
        mockMvc.perform(post("/admin/users/{id}", regularUser.getId())
                        .param("username", "updateduser")
                        .param("email", "updated@example.com")
                        .param("enabled", "true")
                        .param("roles", userRole.getId().toString())
                        .param("roles", techleadRole.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + regularUser.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the user was updated
        User updatedUser = userService.findById(regularUser.getId()).orElseThrow();
        assert updatedUser.getUsername().equals("updateduser");
        assert updatedUser.getEmail().equals("updated@example.com");
        assert updatedUser.isEnabled();
        assert updatedUser.getRoles().size() == 2;
        assert updatedUser.getRoles().contains(userRole);
        assert updatedUser.getRoles().contains(techleadRole);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testToggleUserStatus() throws Exception {
        // Initially the user is enabled
        assert regularUser.isEnabled();

        mockMvc.perform(post("/admin/users/{id}/toggle-status", regularUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + regularUser.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the user status was toggled
        User updatedUser = userService.findById(regularUser.getId()).orElseThrow();
        assert !updatedUser.isEnabled();

        // Toggle again to enable
        mockMvc.perform(post("/admin/users/{id}/toggle-status", regularUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + regularUser.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the user status was toggled back
        updatedUser = userService.findById(regularUser.getId()).orElseThrow();
        assert updatedUser.isEnabled();
    }

    @Test
    @WithMockUser(username = "user", authorities = {})
    public void testAccessDeniedForNonAdminUser() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "techlead", authorities = {"READ_USER"})
    public void testAccessAllowedForTechleadUser() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalItems"))
                .andExpect(model().attributeExists("pageSize"))
                .andExpect(model().attributeExists("sortField"));
    }

    @Test
    @WithMockUser(username = "techlead", authorities = {"READ_USER", "UPDATE_USER"})
    public void testTechleadCanEditUser() throws Exception {
        mockMvc.perform(post("/admin/users/{id}", regularUser.getId())
                        .param("username", "updatedByTechlead")
                        .param("email", "techlead-updated@example.com")
                        .param("enabled", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + regularUser.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the user was updated
        User updatedUser = userService.findById(regularUser.getId()).orElseThrow();
        assert updatedUser.getUsername().equals("updatedByTechlead");
        assert updatedUser.getEmail().equals("techlead-updated@example.com");
        assert updatedUser.isEnabled();
    }

    @Test
    @WithMockUser(username = "techlead", authorities = {"READ_USER", "UPDATE_USER"})
    public void testTechleadCanToggleUserStatus() throws Exception {
        // Initially the user is enabled
        assert regularUser.isEnabled();

        mockMvc.perform(post("/admin/users/{id}/toggle-status", regularUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + regularUser.getId()))
                .andExpect(flash().attributeExists("message"));

        // Verify the user status was toggled
        User updatedUser = userService.findById(regularUser.getId()).orElseThrow();
        assert !updatedUser.isEnabled();
    }

    @Test
    public void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
