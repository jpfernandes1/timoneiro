-- Add active column to users table
ALTER TABLE users
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Comment on column
COMMENT ON COLUMN users.active IS 'User account status (true = active, false = inactive)';