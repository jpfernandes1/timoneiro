-- 1. Create Address table
CREATE TABLE "Address" (
    address_id SERIAL PRIMARY KEY,
    cep VARCHAR(9) NOT NULL,
    number INT,
    street VARCHAR(128) NOT NULL,
    neighborhood VARCHAR(40) NOT NULL,
    city VARCHAR(40) NOT NULL,
    state VARCHAR(40) NOT NULL
);

-- 2. Add adress_id column on Boat table
ALTER TABLE "Boat"
ADD COLUMN address_id INT;

-- 3. Create the Foreign Key
ALTER TABLE "Boat"
ADD CONSTRAINT fk_boat_address
FOREIGN KEY (address_id) REFERENCES "Address"(address_id);

-- 4. REmove the old location column
ALTER TABLE "Boat"
DROP COLUMN location;
