-- Migration: Update reviews table schema for enhanced data integrity and tracking

-- Rename 'date' column to 'created_at' for naming consistency
ALTER TABLE reviews
RENAME COLUMN date TO created_at;

-- Add 'updated_at' column for tracking review modifications
ALTER TABLE reviews
ADD COLUMN updated_at TIMESTAMP;

-- Add length constraint to comment column (aligns with entity validation)
ALTER TABLE reviews
ALTER COLUMN comment TYPE VARCHAR(1000);

-- Update table comments to reflect new schema
COMMENT ON COLUMN reviews.created_at IS 'Timestamp when review was initially created';
COMMENT ON COLUMN reviews.updated_at IS 'Timestamp when review was last modified';
COMMENT ON COLUMN reviews.comment IS 'Review text content with 1000 character limit';