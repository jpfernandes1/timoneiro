-- Add cpf column to users table
ALTER TABLE users
ADD COLUMN cpf VARCHAR(11) UNIQUE;

-- Create index for cpf field
CREATE INDEX idx_users_cpf ON users(cpf);

-- Comment on column
COMMENT ON COLUMN users.cpf IS 'User CPF document (11 digits without formatting)';