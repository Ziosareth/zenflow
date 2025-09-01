-- Align planning_poker_sessions with JPA model: add user_story_id FK
-- Safe-add column if not exists
ALTER TABLE planning_poker_sessions
    ADD COLUMN IF NOT EXISTS user_story_id BIGINT;
