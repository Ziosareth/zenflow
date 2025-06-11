package it.zenflow.controller.admin;

import it.zenflow.model.rbac.Permission;
import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.RoleRepository;
import it.zenflow.service.rbac.PermissionService;
import it.zenflow.service.rbac.RoleService;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleRepository roleRepository;

    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    public void setup() {
        // Create a test permission if it doesn't exist
        testPermission = permissionService.findByName("TEST_PERMISSION")
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName("TEST_PERMISSION");
                    permission.setDescription("Test permission for integration tests");
                    permission.setCategory("TEST");
                    return permissionService.save(permission);
                });

        // Create a test role
        testRole = new Role();
        testRole.setName("TEST_ROLE");
        Set<Permission> permissions = new HashSet<>();
        permissions.add(testPermission);
        testRole.setPermissions(permissions);
        testRole = roleService.save(testRole);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testListRoles() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalItems"))
                .andExpect(model().attributeExists("pageSize"))
                .andExpect(model().attributeExists("sortField"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testViewRole() throws Exception {
        mockMvc.perform(get("/admin/roles/{id}", testRole.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/role-detail"))
                .andExpect(model().attributeExists("role"))
                .andExpect(model().attribute("role", hasProperty("id", is(testRole.getId()))))
                .andExpect(model().attribute("role", hasProperty("name", is("TEST_ROLE"))));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testNewRoleForm() throws Exception {
        mockMvc.perform(get("/admin/roles/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/role-edit"))
                .andExpect(model().attributeExists("role"))
                .andExpect(model().attributeExists("allPermissions"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testEditRoleForm() throws Exception {
        mockMvc.perform(get("/admin/roles/{id}/edit", testRole.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/role-edit"))
                .andExpect(model().attributeExists("role"))
                .andExpect(model().attributeExists("allPermissions"))
                .andExpect(model().attribute("role", hasProperty("id", is(testRole.getId()))))
                .andExpect(model().attribute("role", hasProperty("name", is("TEST_ROLE"))));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testCreateRole() throws Exception {
        mockMvc.perform(post("/admin/roles/new")
                        .param("name", "NEW_TEST_ROLE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/roles/*"));

        // Verify the role was created
        Role newRole = roleService.findByName("NEW_TEST_ROLE").orElseThrow();
        assert newRole.getName().equals("NEW_TEST_ROLE");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testUpdateRole() throws Exception {
        mockMvc.perform(post("/admin/roles/{id}", testRole.getId())
                        .param("name", "UPDATED_TEST_ROLE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles/" + testRole.getId()));

        // Verify the role was updated
        Role updatedRole = roleService.findById(testRole.getId()).orElseThrow();
        assert updatedRole.getName().equals("UPDATED_TEST_ROLE");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_ROLE", "UPDATE_ROLE"})
    public void testDeleteRole() throws Exception {
        mockMvc.perform(post("/admin/roles/{id}/delete", testRole.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"));

        // Verify the role was deleted
        assert roleService.findById(testRole.getId()).isEmpty();
    }

    @Test
    @WithMockUser(username = "user", authorities = {})
    public void testAccessDeniedForNonAdminUser() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
