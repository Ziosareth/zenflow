-- Create admin user only for the default tenant
-- The ${defaultTenant} placeholder will be replaced with the actual default tenant name
DO $$
BEGIN
    -- Check if we're in the default tenant's database by checking if the database name ends with the default tenant name
    IF '${defaultTenant}' = SUBSTRING(current_database(), (LENGTH(current_database()) - LENGTH('${defaultTenant}') + 1)) THEN
        -- Insert admin user if it doesn't exist
        INSERT INTO users (username, email, password, enabled, password_change_required, tenant)
        SELECT 'admin', 'admin@${defaultTenant}.com', 
               -- Pre-encoded BCrypt password ('changeme')
               '$2a$10$nUI/4Ph4YEhMn8aVnQjAJ.FPyDmMsESbymnOTZxlxX//G32uHgjvC',
               true, true, '${defaultTenant}'
        WHERE NOT EXISTS (
            SELECT 1 FROM users WHERE username = 'admin' AND tenant = '${defaultTenant}'
        );

        -- Get the admin user ID
        PERFORM setval('users_id_seq', (SELECT MAX(id) FROM users), true);

        -- Associate admin user with ADMIN role
        INSERT INTO user_roles (user_id, role_id)
        SELECT u.id, r.id
        FROM users u, roles r
        WHERE u.username = 'admin' 
          AND u.tenant = '${defaultTenant}'
          AND r.name = 'ADMIN'
          AND NOT EXISTS (
              SELECT 1 FROM user_roles ur 
              WHERE ur.user_id = u.id AND ur.role_id = r.id
          );
    END IF;
END $$;
