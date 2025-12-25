
ALTER TABLE boat_photos
    ADD COLUMN public_id VARCHAR(500) NOT NULL DEFAULT 'temp_public_id';

ALTER TABLE boat_photos
    ADD COLUMN file_name VARCHAR(255);

ALTER TABLE boat_photos
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
