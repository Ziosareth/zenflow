package it.zenflow.master.service;

import it.zenflow.config.multitenant.TenantContext;
import it.zenflow.master.dto.CreateTenantDTO;
import it.zenflow.master.model.Tenant;
import it.zenflow.master.model.TenantRepository;
import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.RoleRepository;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MasterTenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoleRepository roleRepository;

    // Create a spy of MasterTenantService to mock the initializeTenantDatabase method
    private MasterTenantService masterTenantService;

    private CreateTenantDTO createTenantDTO;
    private Tenant tenant;
    private Role adminRole;

    @BeforeEach
    public void setup() {
        // Create test data
        createTenantDTO = new CreateTenantDTO();
        createTenantDTO.setName("test-tenant");
        createTenantDTO.setUrl("jdbc:postgresql://localhost:5432/test-tenant");
        createTenantDTO.setUsername("testuser");
        createTenantDTO.setPassword("testpassword");
        createTenantDTO.setDriver("org.postgresql.Driver");
        createTenantDTO.setEnabled(false);
        createTenantDTO.setAdminEmail("admin@example.com");

        tenant = new Tenant();
        tenant.setName("test-tenant");
        tenant.setUrl("jdbc:postgresql://localhost:5432/test-tenant");
        tenant.setUsername("testuser");
        tenant.setPassword("testpassword");
        tenant.setDriver("org.postgresql.Driver");
        tenant.setEnabled(false);

        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ROLE_ADMIN");

        // Initialize the service with mocks
        masterTenantService = new MasterTenantService(tenantRepository, userService, roleRepository);

        // Common mock setup
        when(tenantRepository.existsById(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // Mock the initializeTenantDatabase method to do nothing
        masterTenantService = spy(masterTenantService);
        doNothing().when(masterTenantService).initializeTenantDatabase(anyString());
    }

    @Test
    public void testCreateTenantWithAdminUser() {
        // Arrange
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        // Act
        Tenant result = masterTenantService.createTenant(createTenantDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-tenant");

        // Verify tenant was saved
        verify(tenantRepository, times(1)).save(any(Tenant.class));

        // Verify database was initialized
        verify(masterTenantService, times(1)).initializeTenantDatabase("test-tenant");

        // Verify admin user was created
        verify(roleRepository, times(1)).findByName("ADMIN");

        // Capture the arguments passed to inviteUser
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Set<Role>> rolesCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Locale> localeCaptor = ArgumentCaptor.forClass(Locale.class);

        verify(userService, times(1)).inviteUser(
            usernameCaptor.capture(),
            emailCaptor.capture(),
            rolesCaptor.capture(),
            localeCaptor.capture()
        );

        assertThat(usernameCaptor.getValue()).isEqualTo("admin");
        assertThat(emailCaptor.getValue()).isEqualTo("admin@example.com");
        assertThat(rolesCaptor.getValue()).contains(adminRole);
        assertThat(localeCaptor.getValue()).isEqualTo(Locale.ITALIAN);
    }

    @Test
    public void testCreateTenantWithoutAdminEmail() {
        // Arrange
        createTenantDTO.setAdminEmail(null);

        // Act
        Tenant result = masterTenantService.createTenant(createTenantDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-tenant");

        // Verify tenant was saved
        verify(tenantRepository, times(1)).save(any(Tenant.class));

        // Verify database was initialized
        verify(masterTenantService, times(1)).initializeTenantDatabase("test-tenant");

        // Verify admin user was NOT created
        verify(userService, never()).inviteUser(anyString(), anyString(), any(), any(Locale.class));
    }

    @Test
    public void testCreateTenantWithAdminUserError() {
        // Arrange
        when(roleRepository.findByName("ADMIN")).thenThrow(new RuntimeException("Role not found"));

        // Act
        Tenant result = masterTenantService.createTenant(createTenantDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-tenant");

        // Verify tenant was saved and database was initialized despite the error
        verify(tenantRepository, times(1)).save(any(Tenant.class));
        verify(masterTenantService, times(1)).initializeTenantDatabase("test-tenant");

        // Verify the error was handled and the tenant creation completed
        verify(roleRepository, times(1)).findByName("ADMIN");
        verify(userService, never()).inviteUser(anyString(), anyString(), any(), any(Locale.class));
    }

    @Test
    public void testTenantContextIsProperlyManaged() {
        // Arrange
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        // Set a previous tenant context
        TenantContext.setCurrentTenant("previous-tenant");

        // Act
        Tenant result = masterTenantService.createTenant(createTenantDTO);

        // Assert
        assertThat(result).isNotNull();

        // Verify the tenant context was restored
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("previous-tenant");

        // Clean up
        TenantContext.clear();
    }
}
