INSERT INTO zenflow.tenants (name, url, username, password, driver, enabled)
SELECT '${defaultTenant}', 
       '${tenant.url}', 
       '${tenant.username}', 
       '${tenant.password}', 
       '${tenant.driver}',
       true
WHERE NOT EXISTS (
    SELECT 1 FROM zenflow.tenants WHERE name = '${defaultTenant}'
);