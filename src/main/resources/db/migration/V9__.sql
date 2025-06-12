-- Add estimation_type column to user_stories table
ALTER TABLE user_stories ADD COLUMN estimation_type VARCHAR(255) NOT NULL DEFAULT 'STORY_POINTS';

-- Add estimation_type column to user_stories_aud table
ALTER TABLE user_stories_aud ADD COLUMN estimation_type VARCHAR(255);