package it.zenflow.config.multitenant;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@Profile("!test")
public class MasterFlywayConfiguration {


    @Bean(name = "masterFlyway")
    @Primary
    public Flyway masterFlyway(@Qualifier("masterDataSource") DataSource dataSource,
                              @Value("${defaultTenant}") String defaultTenant,
                              @Value("${tenant.url}") String tenantUrl,
                              @Value("${tenant.username}") String tenantUsername,
                              @Value("${tenant.password}") String tenantPassword,
                              @Value("${tenant.driver:org.postgresql.Driver}") String tenantDriver) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/master")  // Separate folder for master migrations
                .baselineOnMigrate(true)
                .schemas("zenflow")
                .defaultSchema("zenflow")
                .placeholders(Map.of(
                    "defaultTenant", defaultTenant,
                    "tenant.url", tenantUrl,
                    "tenant.username", tenantUsername,
                    "tenant.password", tenantPassword,
                    "tenant.driver", tenantDriver
                ))
                .load();

        // Run migrations immediately
        flyway.migrate();

        return flyway;
    }
}
