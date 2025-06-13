-- Add user_story_id column to planning_poker_sessions_aud table
ALTER TABLE planning_poker_sessions_aud ADD COLUMN user_story_id BIGINT;

-- Create index for better performance
CREATE INDEX idx_planning_poker_sessions_aud_user_story_id ON planning_poker_sessions_aud (user_story_id);

-- Drop the poker_session_user_stories_aud table if it exists
DROP TABLE IF EXISTS poker_session_user_stories_aud;