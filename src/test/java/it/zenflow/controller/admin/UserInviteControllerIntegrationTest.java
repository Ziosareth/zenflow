package it.zenflow.controller.admin;

import it.zenflow.config.ZenFlowAuthenticationHandler;
import it.zenflow.model.rbac.*;
import it.zenflow.service.EmailService;
import it.zenflow.service.rbac.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class UserInviteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUser;
    private Role adminRole;
    private Role userRole;
    private Permission readUserPermission;
    private Permission createUserPermission;
    private Permission updateUserPermission;


    @BeforeEach
    public void setup() {
        // Clear existing data
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create permissions
        readUserPermission = new Permission();
        readUserPermission.setName("READ_USER");
        readUserPermission.setDescription("Permission to view user details");
        readUserPermission.setCategory("USER_MANAGEMENT");
        readUserPermission = permissionRepository.save(readUserPermission);

        createUserPermission = new Permission();
        createUserPermission.setName("CREATE_USER");
        createUserPermission.setDescription("Permission to create users");
        createUserPermission.setCategory("USER_MANAGEMENT");
        createUserPermission = permissionRepository.save(createUserPermission);

        updateUserPermission = new Permission();
        updateUserPermission.setName("UPDATE_USER");
        updateUserPermission.setDescription("Permission to edit user details");
        updateUserPermission.setCategory("USER_MANAGEMENT");
        updateUserPermission = permissionRepository.save(updateUserPermission);

        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(readUserPermission);
        adminPermissions.add(createUserPermission);
        adminPermissions.add(updateUserPermission);
        adminRole.setPermissions(adminPermissions);
        adminRole = roleRepository.save(adminRole);

        userRole = new Role();
        userRole.setName("USER");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(readUserPermission);
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
        regularUser = userService.save(regularUser);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "CREATE_USER"})
    public void testInviteUserForm() throws Exception {
        mockMvc.perform(get("/admin/users/invite"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-invite"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("allRoles"));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER"})
    public void testInviteUserFormAccessDenied() throws Exception {
        mockMvc.perform(get("/admin/users/invite"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "CREATE_USER"})
    public void testInviteUser() throws Exception {
        String newUsername = "newuser";
        String newEmail = "newuser@example.com";

        mockMvc.perform(post("/admin/users/invite")
                        .param("username", newUsername)
                        .param("email", newEmail)
                        .param("roles", adminRole.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("message"));

        // Verify user was created
        Optional<User> createdUser = userService.findByUsername(newUsername);
        assertTrue(createdUser.isPresent());
        assertEquals(newEmail, createdUser.get().getEmail());
        assertTrue(createdUser.get().isEnabled());
        assertTrue(createdUser.get().isPasswordChangeRequired());
        assertEquals(1, createdUser.get().getRoles().size());
        assertTrue(createdUser.get().getRoles().contains(adminRole));

        // Verify email was sent
        verify(emailService, times(1)).sendInvitationEmail(
                eq(newEmail),
                eq(newUsername),
                any(String.class),
                any(Locale.class)
        );
    }

    @Test
    @WithMockUser(username = "user", authorities = {"READ_USER"})
    public void testInviteUserAccessDenied() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("roles", adminRole.getId().toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Verify user was not created
        assertFalse(userService.findByUsername("newuser").isPresent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"READ_USER", "CREATE_USER"})
    public void testInviteUserWithMultipleRoles() throws Exception {
        String newUsername = "multiuser";
        String newEmail = "multiuser@example.com";

        mockMvc.perform(post("/admin/users/invite")
                        .param("username", newUsername)
                        .param("email", newEmail)
                        .param("roles", adminRole.getId().toString())
                        .param("roles", userRole.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("message"));

        // Verify user was created with multiple roles
        Optional<User> createdUser = userService.findByUsername(newUsername);
        assertTrue(createdUser.isPresent());
        assertEquals(2, createdUser.get().getRoles().size());
        assertTrue(createdUser.get().getRoles().contains(adminRole));
        assertTrue(createdUser.get().getRoles().contains(userRole));
    }

    @Test
    public void testPasswordChangeRequiredFlag() throws Exception {
        // Create an invited user with passwordChangeRequired=true
        String invitedUsername = "invited";
        String invitedEmail = "invited@example.com";

        User invitedUser = new User();
        invitedUser.setUsername(invitedUsername);
        invitedUser.setEmail(invitedEmail);
        invitedUser.setPassword(passwordEncoder.encode("temporaryPassword"));
        invitedUser.setEnabled(true);
        invitedUser.setPasswordChangeRequired(true);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        invitedUser.setRoles(roles);
        userRepository.save(invitedUser);

        // Verify the user has passwordChangeRequired=true
        User savedUser = userService.findByUsername(invitedUsername).orElseThrow();
        assertTrue(savedUser.isPasswordChangeRequired());

        // Create a mock HttpServletRequest and HttpServletResponse to test the handler directly
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);

        // Set up the authentication to return our test username
        when(authentication.getName()).thenReturn(invitedUsername);

        // Inject and test the ZenFlowAuthenticationHandler directly
        ZenFlowAuthenticationHandler authHandler = new ZenFlowAuthenticationHandler(userService);
        authHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify that sendRedirect was called with "/password/change"
        verify(response).sendRedirect("/password/change");
    }

    @Test
    @WithMockUser(username = "passwordchange", authorities = {"READ_USER"})
    public void testPasswordChangeProcess() throws Exception {
        // Create a user that needs to change password
        String username = "passwordchange";
        String email = "passwordchange@example.com";

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("oldpassword"));
        user.setEnabled(true);
        user.setPasswordChangeRequired(true);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);

        // Submit password change
        String newPassword = "newSecurePassword123";
        mockMvc.perform(post("/password/change")
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("message"));

        // Verify password was changed and flag was updated
        User updatedUser = userService.findByUsername(username).orElseThrow();
        assertFalse(updatedUser.isPasswordChangeRequired());
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
    }
}
