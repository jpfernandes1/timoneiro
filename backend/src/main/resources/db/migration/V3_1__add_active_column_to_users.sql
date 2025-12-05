-- Add user account status management
-- Design Decisions:
-- - Soft delete support via active flag instead of physical deletion
-- - Default active status for new users
-- - Enables account suspension/reactivation workflows

-- Add active column to users table for account status management
ALTER TABLE users
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Document the column purpose
COMMENT ON COLUMN users.active IS 'User account status (true = active, false = inactive) - enables soft delete functionality';

-- Add index for filtering active users
CREATE INDEX idx_users_active ON users(active);

COMMENT ON INDEX idx_users_active IS 'Optimizes queries filtering by user account status';