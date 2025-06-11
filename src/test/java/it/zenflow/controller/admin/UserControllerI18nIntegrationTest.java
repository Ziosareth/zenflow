package it.zenflow.controller.admin;

import it.zenflow.model.rbac.*;
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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class UserControllerI18nIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    private User testUser;
    private Role adminRole;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

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

        // Create admin role
        adminRole = new Role();
        adminRole.setName("ADMIN");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(readUserPermission);
        adminPermissions.add(updateUserPermission);
        adminRole.setPermissions(adminPermissions);
        adminRole = roleRepository.save(adminRole);

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        testUser.setRoles(roles);

        // Save user
        testUser = userService.save(testUser);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testUserListPageInEnglish() throws Exception {
        // Set locale to English using the lang parameter
        mockMvc.perform(get("/admin/users")
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("User Management")))
                .andExpect(content().string(containsString("Users")))
                .andExpect(content().string(containsString("Active")))
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(containsString("entries")))
                .andExpect(content().string(containsString("Page Size")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testUserListPageInItalian() throws Exception {
        // Set locale to Italian
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("Gestione Utenti")))
                .andExpect(content().string(containsString("Utenti")))
                .andExpect(content().string(containsString("Attivo")))
                .andExpect(content().string(containsString("Visualizzazione di")))
                .andExpect(content().string(containsString("elementi")))
                .andExpect(content().string(containsString("Elementi per pagina")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testUserDetailPageInEnglish() throws Exception {
        mockMvc.perform(get("/admin/users/{id}", testUser.getId())
                .locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-detail"))
                .andExpect(content().string(containsString("Dettagli Utente")))
                .andExpect(content().string(containsString("Torna agli Utenti")))
                .andExpect(content().string(containsString("Disabilita Utente")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testUserDetailPageInItalian() throws Exception {
        // Set locale to Italian
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        mockMvc.perform(get("/admin/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-detail"))
                .andExpect(content().string(containsString("Dettagli Utente")))
                .andExpect(content().string(containsString("Torna agli Utenti")))
                .andExpect(content().string(containsString("Disabilita Utente")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testFlashMessagesInEnglish() throws Exception {
        // Toggle user status
        mockMvc.perform(post("/admin/users/{id}/toggle-status", testUser.getId())
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Utente disabilitato con successo"));

        // Toggle back
        mockMvc.perform(post("/admin/users/{id}/toggle-status", testUser.getId())
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Utente abilitato con successo"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "UPDATE_USER"})
    public void testFlashMessagesInItalian() throws Exception {
        // Set locale to Italian
        LocaleContextHolder.setLocale(Locale.ITALIAN);

        // Toggle user status
        mockMvc.perform(post("/admin/users/{id}/toggle-status", testUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Utente disabilitato con successo"));

        // Toggle back
        mockMvc.perform(post("/admin/users/{id}/toggle-status", testUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Utente abilitato con successo"));
    }
}
