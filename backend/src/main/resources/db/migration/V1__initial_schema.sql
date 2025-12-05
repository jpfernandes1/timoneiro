-- Initial database schema for Timoneiro Boat Rental Platform
-- Design Decisions:
-- - Uses SERIAL primary keys for auto-incrementing identifiers
-- - Enforces data integrity with constraints and references
-- - Implements booking overlap prevention with exclusion constraints
-- - Includes comprehensive indexing for performance optimization

-- Users table: Platform user accounts
CREATE TABLE "users" (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE "users" IS 'Stores user accounts for the boat rental platform';
COMMENT ON COLUMN "users".user_id IS 'Primary identifier for user records';
COMMENT ON COLUMN "users".email IS 'Unique email address for authentication and communication';
COMMENT ON COLUMN "users".role IS 'User role: customer, owner, or admin';

-- Boats table: Boat listings with owner information
CREATE TABLE "boats" (
    boat_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(50),
    capacity INT,
    price_per_hour NUMERIC(10,2),
    location VARCHAR(100),
    photo_url VARCHAR(255),
    owner_id INT NOT NULL REFERENCES "users"(user_id)
);

COMMENT ON TABLE "boats" IS 'Boat listings available for rental';
COMMENT ON COLUMN "boats".price_per_hour IS 'Rental price per hour in BRL';
COMMENT ON COLUMN "boats".owner_id IS 'Reference to user who owns this boat';

-- Boat Availability: Time slots when boats are available
CREATE TABLE "boats_availability" (
    availability_id SERIAL PRIMARY KEY,
    boat_id INT NOT NULL REFERENCES "boats"(boat_id),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL
);

COMMENT ON TABLE "boats_availability" IS 'Available time slots for boat rentals';
COMMENT ON COLUMN "boats_availability".start_date IS 'Start of available time period';
COMMENT ON COLUMN "boats_availability".end_date IS 'End of available time period';

-- Enable btree_gist for exclusion constraints
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Bookings: Rental reservations with pricing
CREATE TABLE "bookings" (
    booking_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "users"(user_id),
    boat_id INT NOT NULL REFERENCES "boats"(boat_id),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    total_price NUMERIC(10,2)
);

COMMENT ON TABLE "bookings" IS 'Boat rental reservations and bookings';
COMMENT ON COLUMN "bookings".status IS 'Booking status: pending, confirmed, cancelled, completed';
COMMENT ON COLUMN "bookings".total_price IS 'Calculated total price for the booking duration';

-- Reviews: User reviews and ratings for boats
CREATE TABLE "reviews" (
    review_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "users"(user_id),
    boat_id INT NOT NULL REFERENCES "boats"(boat_id),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    date TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE "reviews" IS 'User reviews and ratings for boat experiences';
COMMENT ON COLUMN "reviews".rating IS 'Rating from 1 to 5 stars';

-- Payments: Payment transactions for bookings
CREATE TABLE "payments" (
    payment_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES "bookings"(booking_id),
    amount NUMERIC(10,2),
    status VARCHAR(20) DEFAULT 'pending',
    payment_date TIMESTAMP
);

COMMENT ON TABLE "payments" IS 'Payment transactions associated with bookings';
COMMENT ON COLUMN "payments".status IS 'Payment status: pending, confirmed, failed, refunded';

-- Messages: Communication between users about bookings
CREATE TABLE "messages" (
    message_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES "bookings"(booking_id),
    sender_id INT NOT NULL REFERENCES "users"(user_id),
    content TEXT,
    sent_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE "messages" IS 'Messages between users regarding bookings';

-- Performance Indexes
CREATE INDEX idx_boat_location_type ON "boats"(location, type);
CREATE INDEX idx_booking_boat_dates ON "bookings"(boat_id, start_date, end_date);
CREATE INDEX idx_booking_user ON "bookings"(user_id);
CREATE INDEX idx_payment_status ON "payments"(status);
CREATE INDEX idx_review_boat ON "reviews"(boat_id);

COMMENT ON INDEX idx_boat_location_type IS 'Optimizes boat search by location and type';
COMMENT ON INDEX idx_booking_boat_dates IS 'Speeds up booking availability queries';
COMMENT ON INDEX idx_booking_user IS 'Optimizes querying bookings by user';
COMMENT ON INDEX idx_payment_status IS 'Improves payment status filtering performance';

-- Constraint to prevent overlapping bookings for the same boat
ALTER TABLE "bookings"
ADD CONSTRAINT booking_no_overlap
EXCLUDE USING gist (
    boat_id WITH =,
    tsrange(start_date, end_date) WITH &&
);

COMMENT ON CONSTRAINT booking_no_overlap ON "bookings" IS 'Prevents double-booking by ensuring no overlapping time ranges for the same boat';