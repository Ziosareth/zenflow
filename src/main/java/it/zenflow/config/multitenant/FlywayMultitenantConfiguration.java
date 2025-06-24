package it.zenflow.config.multitenant;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

@Configuration
public class FlywayMultitenantConfiguration {

    @Value("${flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Value("${flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${flyway.default-schema:zenflow}")
    private String defaultSchema;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateTenants() {
        File[] files = Paths.get("allTenants").toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("Directory allTenants not found or empty");
        }

        for (File propertyFile : files) {
            if (propertyFile.getName().endsWith(".properties")) {
                migrateTenant(propertyFile);
            }
        }
    }

    private void migrateTenant(File propertyFile) {
        Properties tenantProperties = new Properties();

        try {
            tenantProperties.load(new FileInputStream(propertyFile));
            String tenantId = tenantProperties.getProperty("name");
            String url = tenantProperties.getProperty("datasource.url");
            String username = tenantProperties.getProperty("datasource.username");
            String password = tenantProperties.getProperty("datasource.password");

            System.out.println("Executing Flyway migration for tenant: " + tenantId);

            Flyway flyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .locations(flywayLocations)
                    .baselineOnMigrate(baselineOnMigrate)
                    .schemas(defaultSchema)
                    .defaultSchema(defaultSchema)
                    .load();

            flyway.migrate();

            System.out.println("Migration completed successfully for tenant: " + tenantId);

        } catch (IOException e) {
            throw new RuntimeException("Error reading tenant properties file: " + propertyFile.getName(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing Flyway migration for tenant from file: " + propertyFile.getName(), e);
        }
    }
}