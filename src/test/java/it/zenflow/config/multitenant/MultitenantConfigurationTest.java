package it.zenflow.config.multitenant;

import it.zenflow.master.model.Tenant;
import it.zenflow.master.model.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MultitenantConfigurationTest {

    @Mock
    private TenantDataSourcePool tenantDataSourcePool;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private DataSource mockDataSource;

    private MultitenantConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new MultitenantConfiguration(tenantDataSourcePool);
    }

    @Test
    void testRoutingDataSourceCreation() {
        // When
        DataSource routingDataSource = configuration.routingDataSource();

        // Then
        assertNotNull(routingDataSource);
        assertInstanceOf(AbstractRoutingDataSource.class, routingDataSource);
    }

    @Test
    void testTenantContextIntegration() {
        // Given
        String tenantId = "test";
        TenantContext.setCurrentTenant(tenantId);

        try {
            // When
            String currentTenant = TenantContext.getCurrentTenant();

            // Then
            assertEquals(tenantId, currentTenant);
        } finally {
            // Cleanup
            TenantContext.clear();
        }
    }

    @Test
    void testTenantContextClear() {
        // Given
        TenantContext.setCurrentTenant("test");

        // When
        TenantContext.clear();

        // Then
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testTenantEntity() {
        // Given
        Tenant tenant = new Tenant();
        tenant.setName("tenant1");
        tenant.setUrl("jdbc:postgresql://localhost:5432/tenant1");
        tenant.setUsername("tenant1_user");
        tenant.setPassword("tenant1_pass");
        tenant.setDriver("org.postgresql.Driver");
        tenant.setEnabled(true);

        // Then
        assertEquals("tenant1", tenant.getName());
        assertEquals("jdbc:postgresql://localhost:5432/tenant1", tenant.getUrl());
        assertEquals("tenant1_user", tenant.getUsername());
        assertEquals("tenant1_pass", tenant.getPassword());
        assertEquals("org.postgresql.Driver", tenant.getDriver());
        assertTrue(tenant.isEnabled());
    }
}
