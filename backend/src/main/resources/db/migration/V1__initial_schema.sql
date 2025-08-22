-- Users
CREATE TABLE "User" (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Boats
CREATE TABLE "Boat" (
    boat_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(50),
    capacity INT,
    price_per_hour NUMERIC(10,2),
    location VARCHAR(100),
    photo_url VARCHAR(255),
    owner_id INT NOT NULL REFERENCES "User"(user_id)
);

-- Boat Availability
CREATE TABLE "BoatAvailability" (
    availability_id SERIAL PRIMARY KEY,
    boat_id INT NOT NULL REFERENCES "Boat"(boat_id),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL
);

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Bookings
CREATE TABLE "Booking" (
    booking_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "User"(user_id),
    boat_id INT NOT NULL REFERENCES "Boat"(boat_id),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    total_price NUMERIC(10,2)
);

-- Reviews
CREATE TABLE "Review" (
    review_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "User"(user_id),
    boat_id INT NOT NULL REFERENCES "Boat"(boat_id),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    date TIMESTAMP DEFAULT NOW()
);

-- Payments
CREATE TABLE "Payment" (
    payment_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES "Booking"(booking_id),
    amount NUMERIC(10,2),
    status VARCHAR(20) DEFAULT 'pending',
    payment_date TIMESTAMP
);

-- Messages
CREATE TABLE "Message" (
    message_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES "Booking"(booking_id),
    sender_id INT NOT NULL REFERENCES "User"(user_id),
    content TEXT,
    sent_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_boat_location_type ON "Boat"(location, type);
CREATE INDEX idx_booking_boat_dates ON "Booking"(boat_id, start_date, end_date);
CREATE INDEX idx_booking_user ON "Booking"(user_id);
CREATE INDEX idx_payment_status ON "Payment"(status);
CREATE INDEX idx_review_boat ON "Review"(boat_id);

-- Constraint to prevent overlapping bookings
ALTER TABLE "Booking"
ADD CONSTRAINT booking_no_overlap
EXCLUDE USING gist (
    boat_id WITH =,
    tsrange(start_date, end_date) WITH &&
);
