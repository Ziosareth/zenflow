-- Add passwordChangeRequired column to users table
ALTER TABLE zenflow.users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;