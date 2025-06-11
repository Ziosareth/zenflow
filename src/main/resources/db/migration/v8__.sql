-- Migration per aggiungere NOT NULL constraint
UPDATE permissions SET category = 'UNCATEGORIZED' WHERE category IS NULL;
ALTER TABLE permissions MODIFY COLUMN category VARCHAR(255) NOT NULL DEFAULT 'UNCATEGORIZED';