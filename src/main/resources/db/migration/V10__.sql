-- Add variance column to user_stories table
ALTER TABLE user_stories ADD COLUMN variance DOUBLE PRECISION;

-- Add variance column to user_stories_aud table
ALTER TABLE user_stories_aud ADD COLUMN variance DOUBLE PRECISION;

-- Initialize variance for existing records with PERT estimation
UPDATE user_stories
SET variance = POWER((pessimistic_estimate - optimistic_estimate) / 6, 2)
WHERE estimation_type = 'PERT'
  AND optimistic_estimate IS NOT NULL
  AND pessimistic_estimate IS NOT NULL
  AND most_likely_estimate IS NOT NULL;