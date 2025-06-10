-- Add category column to permissions table
ALTER TABLE zenflow.permissions ADD COLUMN category VARCHAR(255);
ALTER TABLE zenflow.permissions_aud ADD COLUMN category VARCHAR(255);

-- Update existing permissions with categories
-- Administration permissions
UPDATE zenflow.permissions SET category = 'ADMINISTRATION' WHERE name IN (
    'CREATE_USER', 'READ_USER', 'UPDATE_USER', 'DELETE_USER',
    'CREATE_ROLE', 'READ_ROLE', 'UPDATE_ROLE', 'DELETE_ROLE',
    'ADMIN_ACCESS'
);

-- Project permissions
UPDATE zenflow.permissions SET category = 'PROJECT' WHERE name IN (
    'CREATE_PROJECT', 'READ_PROJECT', 'UPDATE_PROJECT', 'DELETE_PROJECT'
);

-- User Story permissions
UPDATE zenflow.permissions SET category = 'USER_STORY' WHERE name IN (
    'CREATE_USER_STORY', 'READ_USER_STORY', 'UPDATE_USER_STORY', 'DELETE_USER_STORY'
);

-- Sprint permissions
UPDATE zenflow.permissions SET category = 'SPRINT' WHERE name IN (
    'CREATE_SPRINT', 'READ_SPRINT', 'UPDATE_SPRINT', 'DELETE_SPRINT'
);

-- Task permissions
UPDATE zenflow.permissions SET category = 'TASK' WHERE name IN (
    'CREATE_TASK', 'READ_TASK', 'UPDATE_TASK', 'DELETE_TASK'
);

-- Planning Poker permissions
UPDATE zenflow.permissions SET category = 'PLANNING_POKER' WHERE name IN (
    'CREATE_PLANNING_POKER_SESSION', 'READ_PLANNING_POKER_SESSION', 'UPDATE_PLANNING_POKER_SESSION', 'DELETE_PLANNING_POKER_SESSION',
    'CREATE_ESTIMATION_VOTE', 'READ_ESTIMATION_VOTE', 'UPDATE_ESTIMATION_VOTE', 'DELETE_ESTIMATION_VOTE'
);