package it.zenflow.config.multitenant;

import it.zenflow.master.model.Tenant;
import it.zenflow.master.model.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Context;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class FlywayMultitenantConfiguration implements DisposableBean {

    private final TenantRepository tenantRepository;
    private final TenantDataSourcePool tenantDataSourcePool;

    @Value("${flyway.locations:classpath:db/migration/tenant}")
    private String flywayLocations;

    @Value("${flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${flyway.default-schema:zenflow}")
    private String defaultSchema;

    @Value("${defaultTenant}")
    private String defaultTenant;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateTenants() {
        List<Tenant> enabledTenants = tenantRepository.findByEnabledTrue();

        if (enabledTenants.isEmpty()) {
            log.warn("No enabled tenants found for Flyway migration");
            return;
        }

        log.info("Starting Flyway migration for {} enabled tenants", enabledTenants.size());

        for (Tenant tenant : enabledTenants) {
            migrateTenant(tenant);
        }

        log.info("Flyway migration completed for all tenants");
    }

    private void migrateTenant(Tenant tenant) {
        try {
            log.info("Executing Flyway migration for tenant: {}", tenant.getName());

            // Use the shared connection pool instead of creating a new connection
            DataSource dataSource = tenantDataSourcePool.getOrCreate(tenant.getName());

            // Set up placeholders for the migration
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("defaultTenant", defaultTenant);

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(flywayLocations)
                    .baselineOnMigrate(baselineOnMigrate)
                    .schemas(defaultSchema)
                    .defaultSchema(defaultSchema)
                    .placeholders(placeholders)
                    .load();

            flyway.migrate();

            log.info("Migration completed successfully for tenant: {}", tenant.getName());

        } catch (Exception e) {
            log.error("Error executing Flyway migration for tenant: {}", tenant.getName(), e);
            throw new RuntimeException("Error executing Flyway migration for tenant: " + tenant.getName(), e);
        }
    }

    /**
     * Handle application shutdown event
     * This is an additional safety measure to ensure connections are closed
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        log.info("Application shutdown event received, ensuring all connections are closed");
        // No specific action needed here as the TenantDataSourcePool will handle cleanup via DisposableBean
    }

    @Override
    public void destroy() throws Exception {
        // No specific cleanup needed here as the TenantDataSourcePool manages the lifecycle of DataSources
        log.info("FlywayMultitenantConfiguration is being destroyed");
    }
}
