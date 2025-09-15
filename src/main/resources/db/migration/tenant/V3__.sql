-- Aggiornamento delle categorie per i permessi esistenti

-- Permessi relativi agli utenti
UPDATE permissions SET category = 'USER_MANAGEMENT' 
WHERE name IN ('CREATE_USER', 'READ_USER', 'UPDATE_USER', 'DELETE_USER');

-- Permessi relativi ai ruoli
UPDATE permissions SET category = 'ROLE_MANAGEMENT' 
WHERE name IN ('CREATE_ROLE', 'READ_ROLE', 'UPDATE_ROLE', 'DELETE_ROLE');

-- Permessi relativi ai progetti
UPDATE permissions SET category = 'PROJECT_MANAGEMENT' 
WHERE name IN ('CREATE_PROJECT', 'READ_PROJECT', 'UPDATE_PROJECT', 'DELETE_PROJECT');

-- Permessi relativi alle user story
UPDATE permissions SET category = 'USER_STORY_MANAGEMENT' 
WHERE name IN ('CREATE_USER_STORY', 'READ_USER_STORY', 'UPDATE_USER_STORY', 'DELETE_USER_STORY');

-- Permessi relativi agli sprint
UPDATE permissions SET category = 'SPRINT_MANAGEMENT' 
WHERE name IN ('CREATE_SPRINT', 'READ_SPRINT', 'UPDATE_SPRINT', 'DELETE_SPRINT');

-- Permessi relativi ai task
UPDATE permissions SET category = 'TASK_MANAGEMENT' 
WHERE name IN ('CREATE_TASK', 'READ_TASK', 'UPDATE_TASK', 'DELETE_TASK');

-- Permessi relativi al planning poker
UPDATE permissions SET category = 'PLANNING_POKER' 
WHERE name IN ('CREATE_PLANNING_POKER_SESSION', 'READ_PLANNING_POKER_SESSION', 
               'UPDATE_PLANNING_POKER_SESSION', 'DELETE_PLANNING_POKER_SESSION');

-- Permessi relativi ai voti di stima
UPDATE permissions SET category = 'ESTIMATION_MANAGEMENT' 
WHERE name IN ('CREATE_ESTIMATION_VOTE', 'READ_ESTIMATION_VOTE', 
               'UPDATE_ESTIMATION_VOTE', 'DELETE_ESTIMATION_VOTE');

-- Permessi relativi alle milestone
UPDATE permissions SET category = 'MILESTONE_MANAGEMENT'
WHERE name IN ('CREATE_MILESTONE', 'READ_MILESTONE', 'UPDATE_MILESTONE', 'DELETE_MILESTONE');

-- Permessi relativi alle epic
UPDATE permissions SET category = 'EPIC_MANAGEMENT'
WHERE name IN ('CREATE_EPIC', 'READ_EPIC', 'UPDATE_EPIC', 'DELETE_EPIC');

-- Permessi di amministrazione
UPDATE permissions SET category = 'ADMINISTRATION' 
WHERE name = 'ADMIN_ACCESS';