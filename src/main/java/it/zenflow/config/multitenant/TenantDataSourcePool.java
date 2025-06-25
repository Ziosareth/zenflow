package it.zenflow.config.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.zenflow.model.master.Tenant;
import it.zenflow.model.master.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TenantDataSourcePool {

    private final TenantRepository repo;
    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public DataSource getOrCreate(String tenantName) {
        return cache.computeIfAbsent(tenantName, name -> repo.findById(name)
            .map(this::build)
            .orElseThrow(() -> new IllegalArgumentException("Tenant " + name + " not found")));
    }

    private DataSource build(Tenant t) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(t.getUrl());
        cfg.setUsername(t.getUsername());
        cfg.setPassword(t.getPassword());
        cfg.setDriverClassName(
             Optional.ofNullable(t.getDriver()).orElse("org.postgresql.Driver"));
        cfg.setPoolName("ds-" + t.getName());
        return new HikariDataSource(cfg);
    }

    /** preload all enabled tenants at startup (opzionale) */
    @EventListener(ContextRefreshedEvent.class)
    public void warmUp() {
        repo.findByEnabledTrue().forEach(tenant -> getOrCreate(tenant.getName()));
    }
}
