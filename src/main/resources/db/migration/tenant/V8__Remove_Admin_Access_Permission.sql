-- Remove ADMIN_ACCESS permission
-- First remove from role_permissions to avoid foreign key constraint violations
DELETE FROM role_permissions 
WHERE permission_id IN (SELECT id FROM permissions WHERE name = 'ADMIN_ACCESS');

-- Then remove the permission itself
DELETE FROM permissions 
WHERE name = 'ADMIN_ACCESS';