package it.zenflow.config.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
@Profile("!test")
@Slf4j
public class DatabaseInitializer {

    @Value("${master.datasource.url}")
    private String masterUrl;

    @Value("${master.datasource.username}")
    private String masterUsername;

    @Value("${master.datasource.password}")
    private String masterPassword;

    @Bean(name = "databaseInitialization")
    public boolean initializeDatabase() {
        // Extract the database name from the URL
        String dbName = masterUrl.substring(masterUrl.lastIndexOf("/") + 1);

        // Create a connection to the default PostgreSQL database
        String baseUrl = masterUrl.substring(0, masterUrl.lastIndexOf("/")) + "/postgres";

        try (Connection conn = DriverManager.getConnection(baseUrl, masterUsername, masterPassword);
             Statement stmt = conn.createStatement()) {

            // Check if database exists
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
            if (!rs.next()) {
                // Create database if it doesn't exist
                stmt.execute("CREATE DATABASE " + dbName);
                log.info("Database {} created successfully", dbName);
            } else {
                log.info("Database {} already exists", dbName);
            }

            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}