-- Add structured address support to replace simple location field
-- Design Decisions:
-- - Normalize address data into separate table for reusability
-- - Support Brazilian address format with CEP and neighborhood
-- - Maintain backward compatibility during transition

-- 1. Create Address table with Brazilian address structure
CREATE TABLE "addresses" (
    address_id SERIAL PRIMARY KEY,
    cep VARCHAR(9) NOT NULL,
    number VARCHAR(10),
    street VARCHAR(128) NOT NULL,
    neighborhood VARCHAR(40) NOT NULL,
    city VARCHAR(40) NOT NULL,
    state VARCHAR(40) NOT NULL
);

COMMENT ON TABLE "addresses" IS 'Structured address information for Brazilian locations';
COMMENT ON COLUMN "addresses".cep IS 'Brazilian postal code (CEP) with optional dash';
COMMENT ON COLUMN "addresses".neighborhood IS 'Brazilian neighborhood (bairro) information';

-- 2. Add address_id column to Boat table for address relationship
ALTER TABLE "boats"
ADD COLUMN address_id INT;

COMMENT ON COLUMN "boats".address_id IS 'Reference to structured address information';

-- 3. Create Foreign Key relationship
ALTER TABLE "boats"
ADD CONSTRAINT fk_boat_address
FOREIGN KEY (address_id) REFERENCES "addresses"(address_id);

COMMENT ON CONSTRAINT fk_boat_address ON "boats" IS 'Links boats to their physical address locations';

-- 4. Remove legacy location column after data migration
ALTER TABLE "boats"
DROP COLUMN location;

COMMENT ON COLUMN "boats".location IS 'Legacy location field replaced by structured address';