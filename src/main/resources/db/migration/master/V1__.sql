-- First create the schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS zenflow;

-- Then create the table with schema qualification
CREATE TABLE zenflow.tenants (
                                 name VARCHAR(255) NOT NULL,
                                 url VARCHAR(255),
                                 username VARCHAR(255),
                                 password VARCHAR(255),
                                 driver VARCHAR(255) NOT NULL DEFAULT 'org.postgresql.Driver',
                                 enabled BOOLEAN NOT NULL DEFAULT false,
                                 CONSTRAINT pk_tenants PRIMARY KEY (name),
                                 CONSTRAINT uk_tenants_name UNIQUE (name)
);

-- Insert with schema qualification
INSERT INTO zenflow.tenants (name, url, username, password, driver, enabled) VALUES
    ('tenant1', 'jdbc:postgresql://localhost:5432/tenant1', 'postgres', 'postgres', 'org.postgresql.Driver', true);