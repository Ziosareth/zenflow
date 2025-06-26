package it.zenflow.master.service;

import it.zenflow.master.dto.CreateTenantDTO;
import it.zenflow.master.dto.UpdateTenantDTO;
import it.zenflow.model.master.Tenant;
import it.zenflow.model.master.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.Optional;

@Service
@Transactional(transactionManager = "masterTransactionManager")
@RequiredArgsConstructor
@Slf4j
public class MasterTenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true, transactionManager = "masterTransactionManager")
    public Page<Tenant> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true, transactionManager = "masterTransactionManager")
    public Optional<Tenant> findByName(String name) {
        return tenantRepository.findById(name);
    }

    public Tenant createTenant(CreateTenantDTO dto) {
        // Check if tenant already exists
        if (tenantRepository.existsById(dto.getName())) {
            throw new IllegalArgumentException("Tenant già esistente: " + dto.getName());
        }

        // Create new tenant
        Tenant tenant = new Tenant();
        tenant.setName(dto.getName());
        tenant.setUrl(dto.getUrl());
        tenant.setUsername(dto.getUsername());
        tenant.setPassword(dto.getPassword());
        tenant.setDriver(dto.getDriver() != null ? dto.getDriver() : "org.postgresql.Driver");
        tenant.setEnabled(dto.isEnabled());

        // Save tenant to master database
        return tenantRepository.save(tenant);
    }

    public Tenant updateTenant(String name, UpdateTenantDTO dto) {
        Tenant tenant = tenantRepository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trovato: " + name));

        // Update tenant fields
        if (dto.getUrl() != null) tenant.setUrl(dto.getUrl());
        if (dto.getUsername() != null) tenant.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) tenant.setPassword(dto.getPassword());
        if (dto.getDriver() != null) tenant.setDriver(dto.getDriver());
        tenant.setEnabled(dto.isEnabled());

        return tenantRepository.save(tenant);
    }

    public void save(Tenant tenant) {
        tenantRepository.save(tenant);
    }

    public void initializeTenantDatabase(String name) {
        Tenant tenant = tenantRepository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trovato: " + name));

        // Extract database name from URL
        String url = tenant.getUrl();
        String dbName = url.substring(url.lastIndexOf("/") + 1);

        // Create database if it doesn't exist
        createDatabaseIfNotExists(dbName, tenant);

        // Run Flyway migrations
        runFlywayMigrations(tenant);

        // Enable tenant
        tenant.setEnabled(true);
        tenantRepository.save(tenant);
    }

    private void createDatabaseIfNotExists(String dbName, Tenant tenant) {
        // Extract base URL for PostgreSQL connection
        String baseUrl = tenant.getUrl().substring(0, tenant.getUrl().lastIndexOf("/")) + "/postgres";

        try (Connection conn = DriverManager.getConnection(baseUrl, tenant.getUsername(), tenant.getPassword());
             Statement stmt = conn.createStatement()) {

            // Check if database exists
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
            if (!rs.next()) {
                // Create database if it doesn't exist
                stmt.execute("CREATE DATABASE " + dbName);
                log.info("Database {} creato con successo", dbName);
            } else {
                log.info("Database {} già esistente", dbName);
            }
        } catch (SQLException e) {
            log.error("Errore nella creazione del database: {}", dbName, e);
            throw new RuntimeException("Errore nella creazione del database: " + dbName, e);
        }
    }

    private void runFlywayMigrations(Tenant tenant) {
        try {
            log.info("Esecuzione della migrazione Flyway per il tenant: {}", tenant.getName());

            Flyway flyway = Flyway.configure()
                    .dataSource(tenant.getUrl(), tenant.getUsername(), tenant.getPassword())
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .schemas("zenflow")
                    .defaultSchema("zenflow")
                    .load();

            flyway.migrate();

            log.info("Migrazione completata con successo per il tenant: {}", tenant.getName());
        } catch (Exception e) {
            log.error("Errore nell'esecuzione della migrazione Flyway per il tenant: {}", tenant.getName(), e);
            throw new RuntimeException("Errore nell'esecuzione della migrazione Flyway per il tenant: " + tenant.getName(), e);
        }
    }
}