-- Inserimento permessi base
INSERT INTO zenflow.permissions (name, description) VALUES
                                                        ('CREATE_USER', 'Creare nuovi utenti'),
                                                        ('READ_USER', 'Visualizzare utenti'),
                                                        ('UPDATE_USER', 'Modificare utenti'),
                                                        ('DELETE_USER', 'Eliminare utenti'),
                                                        ('CREATE_ROLE', 'Creare nuovi ruoli'),
                                                        ('READ_ROLE', 'Visualizzare ruoli'),
                                                        ('UPDATE_ROLE', 'Modificare ruoli'),
                                                        ('DELETE_ROLE', 'Eliminare ruoli'),
                                                        ('ADMIN_ACCESS', 'Accesso amministrativo completo');

-- Inserimento ruoli
INSERT INTO zenflow.roles (name) VALUES
                                     ('ADMIN'),
                                     ('USER');

-- Associazione ruolo ADMIN con tutti i permessi
INSERT INTO zenflow.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM zenflow.roles r
         CROSS JOIN zenflow.permissions p
WHERE r.name = 'ADMIN';

-- Associazione ruolo USER con permessi limitati
INSERT INTO zenflow.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM zenflow.roles r, zenflow.permissions p
WHERE r.name = 'USER' AND p.name = 'READ_USER';

-- Inserimento utente admin (password temporanea che verrà aggiornata da Java)
INSERT INTO zenflow.users (username, email, password, enabled) VALUES
    ('admin', '', '$2a$10$ZqSPx8eu.yE04hJ1mayubOw3re41cJ.Lg9nAJHXlhaUK8fsHTx71K', true);

-- Associazione utente admin con ruolo ADMIN
INSERT INTO zenflow.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM zenflow.users u, zenflow.roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';