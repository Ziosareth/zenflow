package it.zenflow.config.multitenant;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("!test")
public class MasterFlywayConfiguration {


    @Bean(name = "masterFlyway")
    @Primary
    public Flyway masterFlyway(@Qualifier("masterDataSource") DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/master")  // Separate folder for master migrations
                .baselineOnMigrate(true)
                .schemas("zenflow")
                .defaultSchema("zenflow")
                .load();

        // Run migrations immediately
        flyway.migrate();

        return flyway;
    }
}
