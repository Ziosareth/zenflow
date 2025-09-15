-- Inserimento permessi base
INSERT INTO permissions (name, description) VALUES
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
INSERT INTO roles (name) VALUES
                                     ('ADMIN');

-- Associazione ruolo ADMIN con tutti i permessi
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN';


-- -- Inserimento utente admin (password temporanea che verrà aggiornata da Java)
-- INSERT INTO users (username, email, password, enabled, password_change_required) VALUES
--     ('admin', 'ale.bivi94@icloud.com', '$2a$10$ZqSPx8eu.yE04hJ1mayubOw3re41cJ.Lg9nAJHXlhaUK8fsHTx71K', true, false);
--
-- -- Associazione utente admin con ruolo ADMIN
-- INSERT INTO user_roles (user_id, role_id)
-- SELECT u.id, r.id
-- FROM users u, roles r
-- WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- Add permissions for Project entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_PROJECT', 'Creare nuovi progetti'),
                                                        ('READ_PROJECT', 'Visualizzare progetti'),
                                                        ('UPDATE_PROJECT', 'Modificare progetti'),
                                                        ('DELETE_PROJECT', 'Eliminare progetti');

-- Add permissions for UserStory entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_USER_STORY', 'Creare nuove user story'),
                                                        ('READ_USER_STORY', 'Visualizzare user story'),
                                                        ('UPDATE_USER_STORY', 'Modificare user story'),
                                                        ('DELETE_USER_STORY', 'Eliminare user story');

-- Add permissions for Sprint entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_SPRINT', 'Creare nuovi sprint'),
                                                        ('READ_SPRINT', 'Visualizzare sprint'),
                                                        ('UPDATE_SPRINT', 'Modificare sprint'),
                                                        ('DELETE_SPRINT', 'Eliminare sprint');

-- Add permissions for Task entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_TASK', 'Creare nuovi task'),
                                                        ('READ_TASK', 'Visualizzare task'),
                                                        ('UPDATE_TASK', 'Modificare task'),
                                                        ('DELETE_TASK', 'Eliminare task');

-- Add permissions for PlanningPokerSession entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_PLANNING_POKER_SESSION', 'Creare nuove sessioni di planning poker'),
                                                        ('READ_PLANNING_POKER_SESSION', 'Visualizzare sessioni di planning poker'),
                                                        ('UPDATE_PLANNING_POKER_SESSION', 'Modificare sessioni di planning poker'),
                                                        ('DELETE_PLANNING_POKER_SESSION', 'Eliminare sessioni di planning poker');

-- Add permissions for EstimationVote entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_ESTIMATION_VOTE', 'Creare nuovi voti di stima'),
                                                        ('READ_ESTIMATION_VOTE', 'Visualizzare voti di stima'),
                                                        ('UPDATE_ESTIMATION_VOTE', 'Modificare voti di stima'),
                                                        ('DELETE_ESTIMATION_VOTE', 'Eliminare voti di stima');

-- Add permissions for Milestone entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_MILESTONE', 'Creare nuove milestone'),
                                                        ('READ_MILESTONE', 'Visualizzare milestone'),
                                                        ('UPDATE_MILESTONE', 'Modificare milestone'),
                                                        ('DELETE_MILESTONE', 'Eliminare milestone');

-- Add permissions for Epic entity
INSERT INTO permissions (name, description) VALUES
                                                        ('CREATE_EPIC', 'Creare nuove epic'),
                                                        ('READ_EPIC', 'Visualizzare epic'),
                                                        ('UPDATE_EPIC', 'Modificare epic'),
                                                        ('DELETE_EPIC', 'Eliminare epic');

-- Associate all new permissions with ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CREATE_PROJECT', 'READ_PROJECT', 'UPDATE_PROJECT', 'DELETE_PROJECT',
                'CREATE_USER_STORY', 'READ_USER_STORY', 'UPDATE_USER_STORY', 'DELETE_USER_STORY',
                'CREATE_SPRINT', 'READ_SPRINT', 'UPDATE_SPRINT', 'DELETE_SPRINT',
                'CREATE_TASK', 'READ_TASK', 'UPDATE_TASK', 'DELETE_TASK',
                'CREATE_PLANNING_POKER_SESSION', 'READ_PLANNING_POKER_SESSION', 'UPDATE_PLANNING_POKER_SESSION', 'DELETE_PLANNING_POKER_SESSION',
                'CREATE_ESTIMATION_VOTE', 'READ_ESTIMATION_VOTE', 'UPDATE_ESTIMATION_VOTE', 'DELETE_ESTIMATION_VOTE',
                'CREATE_MILESTONE', 'READ_MILESTONE', 'UPDATE_MILESTONE', 'DELETE_MILESTONE',
                'CREATE_EPIC', 'READ_EPIC', 'UPDATE_EPIC', 'DELETE_EPIC');
