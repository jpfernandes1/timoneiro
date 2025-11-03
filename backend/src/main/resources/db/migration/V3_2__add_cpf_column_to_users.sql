-- Add CPF support for Brazilian user identification
-- Design Decisions:
-- - Store CPF without formatting for consistency
-- - Enforce uniqueness to prevent duplicate accounts
-- - Support Brazilian legal requirements

-- Add cpf column to users table for Brazilian identification
ALTER TABLE users
ADD COLUMN cpf VARCHAR(11) UNIQUE;

-- Create index for cpf field performance
CREATE INDEX idx_users_cpf ON users(cpf);

-- Document the column and constraints
COMMENT ON COLUMN users.cpf IS 'User CPF document (11 digits without formatting) - Brazilian tax identification';
COMMENT ON INDEX idx_users_cpf IS 'Optimizes CPF-based lookups and ensures uniqueness enforcement';

-- Add constraint for CPF format validation (optional)
-- ALTER TABLE users
-- ADD CONSTRAINT chk_cpf_format
-- CHECK (cpf ~ '^[0-9]{11}$');