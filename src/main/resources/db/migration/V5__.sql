-- Add passwordChangeRequired column to users_aud table
ALTER TABLE zenflow.users_aud ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;