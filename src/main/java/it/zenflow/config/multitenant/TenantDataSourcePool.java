package it.zenflow.config.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.zenflow.master.model.Tenant;
import it.zenflow.master.model.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantDataSourcePool implements DisposableBean {

    private final TenantRepository repo;
    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public DataSource getOrCreate(String tenantName) {
        return cache.computeIfAbsent(tenantName, name -> repo.findById(name)
            .map(this::build)
            .orElseThrow(() -> new IllegalArgumentException("Tenant " + name + " not found")));
    }

    /**
     * Close and remove a specific tenant's datasource from the pool
     * This is useful for releasing resources for a specific tenant
     * @param tenantName the name of the tenant whose datasource should be closed
     */
    public void closeTenantDataSource(String tenantName) {
        DataSource dataSource = cache.remove(tenantName);
        if (dataSource instanceof HikariDataSource) {
            log.info("Closing datasource for tenant: {}", tenantName);
            try {
                ((HikariDataSource) dataSource).close();
            } catch (Exception e) {
                log.error("Error closing datasource for tenant: {}", tenantName, e);
            }
        }
    }

    /**
     * Refresh a tenant's datasource by closing the existing one and creating a new one
     * This is useful when a tenant's configuration changes or when we need to force a reconnection
     * @param tenantName the name of the tenant whose datasource should be refreshed
     * @return the new DataSource
     */
    public DataSource refreshDataSource(String tenantName) {
        log.info("Refreshing datasource for tenant: {}", tenantName);
        closeTenantDataSource(tenantName);
        return getOrCreate(tenantName);
    }

    private DataSource build(Tenant t) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(t.getUrl());
        cfg.setUsername(t.getUsername());
        cfg.setPassword(t.getPassword());
        cfg.setDriverClassName(
             Optional.ofNullable(t.getDriver()).orElse("org.postgresql.Driver"));
        cfg.setPoolName("ds-" + t.getName());

        // Configure connection pool settings to prevent leaks
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setIdleTimeout(30000); // 30 seconds
        cfg.setMaxLifetime(60000); // 60 seconds
        cfg.setConnectionTimeout(5000); // 5 seconds
        cfg.setLeakDetectionThreshold(60000); // 60 seconds

        // Enable auto-commit to ensure connections are returned to the pool
        cfg.setAutoCommit(true);

        log.info("Creating new datasource for tenant: {}", t.getName());
        return new HikariDataSource(cfg);
    }

    /** preload all enabled tenants at startup (opzionale) */
    @EventListener(ContextRefreshedEvent.class)
    public void warmUp() {
        repo.findByEnabledTrue().forEach(tenant -> getOrCreate(tenant.getName()));
    }

    /**
     * Close all datasources when the application is shutting down
     * This prevents connection leaks during application restarts
     */
    @Override
    public void destroy() throws Exception {
        log.info("Closing all tenant datasources...");
        for (Map.Entry<String, DataSource> entry : cache.entrySet()) {
            try {
                if (entry.getValue() instanceof HikariDataSource) {
                    log.info("Closing datasource for tenant:  {}", entry.getKey());
                    ((HikariDataSource) entry.getValue()).close();

                }
            } catch (Exception e) {
                log.error("Error closing datasource for tenant: {}", entry.getKey(), e);
            }
        }
        cache.clear();
        log.info("All tenant datasources closed");

    }
}
