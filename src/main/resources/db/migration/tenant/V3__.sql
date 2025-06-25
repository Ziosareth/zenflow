-- Add user_story_id column to planning_poker_sessions table
ALTER TABLE planning_poker_sessions ADD COLUMN user_story_id BIGINT;

-- Add foreign key constraint
ALTER TABLE planning_poker_sessions ADD CONSTRAINT fk_planning_poker_sessions_user_story FOREIGN KEY (user_story_id) REFERENCES user_stories (id);

-- Create index for better performance
CREATE INDEX idx_planning_poker_sessions_user_story_id ON planning_poker_sessions (user_story_id);

-- Drop the poker_session_user_stories table if it exists
DROP TABLE IF EXISTS poker_session_user_stories;