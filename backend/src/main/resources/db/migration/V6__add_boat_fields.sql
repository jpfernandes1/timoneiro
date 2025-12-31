-- Add a "marina" column to the addresses table.
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS marina VARCHAR(255);

-- Add new columns on boats table
ALTER TABLE boats ADD COLUMN IF NOT EXISTS length DOUBLE PRECISION;
ALTER TABLE boats ADD COLUMN IF NOT EXISTS speed DOUBLE PRECISION;
ALTER TABLE boats ADD COLUMN IF NOT EXISTS fabrication INTEGER;

-- Remove the photo_url column (it will be replaced by a separate table)
ALTER TABLE boats DROP COLUMN IF EXISTS photo_url;

-- Create a table for photos.
CREATE TABLE IF NOT EXISTS boat_photos (
    id BIGSERIAL PRIMARY KEY,
    boat_id BIGINT NOT NULL REFERENCES boats(boat_id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    ordem INTEGER DEFAULT 0
);

-- Create a table for amenities.
CREATE TABLE IF NOT EXISTS boat_amenities (
    boat_id BIGINT NOT NULL REFERENCES boats(boat_id) ON DELETE CASCADE,
    amenity VARCHAR(100) NOT NULL,
    PRIMARY KEY (boat_id, amenity)
);