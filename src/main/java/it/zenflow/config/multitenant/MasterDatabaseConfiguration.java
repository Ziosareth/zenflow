package it.zenflow.config.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Profile("!test")
@Configuration
@EnableJpaRepositories(
    basePackages = "it.zenflow.model.master",
    entityManagerFactoryRef = "masterEntityManagerFactory",
    transactionManagerRef = "masterTransactionManager"
)
public class MasterDatabaseConfiguration {

    @Value("${master.datasource.url:jdbc:postgresql://localhost:5432/zenflow}")
    private String masterUrl;

    @Value("${master.datasource.username:postgres}")
    private String masterUsername;

    @Value("${master.datasource.password:postgres}")
    private String masterPassword;

    @Value("${master.datasource.driver-class-name:org.postgresql.Driver}")
    private String masterDriverClassName;

    @Bean(name = "masterDataSource")
    @Primary
    @DependsOn("databaseInitialization")
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(masterUsername);
        config.setPassword(masterPassword);
        config.setDriverClassName(masterDriverClassName);
        config.setPoolName("master-db-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        return new HikariDataSource(config);
    }

    @Bean(name = "masterEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean masterEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(masterDataSource());
        em.setPackagesToScan("it.zenflow.model.master");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.default_schema", "zenflow"); // Add this line
        em.setJpaProperties(properties);
        em.setJpaProperties(properties);
        
        return em;
    }

    @Bean(name = "masterTransactionManager")
    public PlatformTransactionManager masterTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(masterEntityManagerFactory().getObject());
        return transactionManager;
    }


}