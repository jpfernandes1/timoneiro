-- Expand payments table with comprehensive payment processing fields
-- Design Decisions:
-- - Add gateway-specific fields for transaction tracking and reconciliation
-- - Include audit fields for compliance and debugging
-- - Support multiple payment methods (CREDIT_CARD, PIX, BOLETO)
-- - Maintain backward compatibility with existing pending payments

-- 1. Add new columns for payment processing
ALTER TABLE "payments"
ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CREDIT_CARD',
ADD COLUMN transaction_id VARCHAR(100) UNIQUE,
ADD COLUMN gateway_message VARCHAR(500),
ADD COLUMN processed_at TIMESTAMP,
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(),
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
ADD COLUMN gateway_response TEXT,
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2. Update existing payments to have created_at and updated_at
UPDATE "payments"
SET created_at = COALESCE(payment_date, NOW()),
    updated_at = COALESCE(payment_date, NOW());

-- 3. Add comments for documentation
COMMENT ON COLUMN "payments".payment_method IS 'Payment method used: CREDIT_CARD, PIX, or BOLETO';
COMMENT ON COLUMN "payments".transaction_id IS 'Unique identifier from payment gateway for reconciliation';
COMMENT ON COLUMN "payments".gateway_message IS 'Status message or description from payment gateway';
COMMENT ON COLUMN "payments".processed_at IS 'Timestamp when payment was processed by gateway';
COMMENT ON COLUMN "payments".created_at IS 'Audit field - when payment record was created';
COMMENT ON COLUMN "payments".updated_at IS 'Audit field - when payment record was last updated';
COMMENT ON COLUMN "payments".gateway_response IS 'Raw response data from payment gateway for debugging';
COMMENT ON COLUMN "payments".version IS 'Optimistic locking version for concurrent updates';

-- 4. Create indexes for performance
CREATE INDEX idx_payments_transaction_id ON "payments"(transaction_id);
CREATE INDEX idx_payments_processed_at ON "payments"(processed_at);
CREATE INDEX idx_payments_created_at ON "payments"(created_at);
CREATE INDEX idx_payments_payment_method ON "payments"(payment_method);

-- 5. Add check constraint for payment method
ALTER TABLE "payments"
ADD CONSTRAINT chk_payment_method
CHECK (payment_method IN ('CREDIT_CARD', 'PIX', 'BOLETO'));

-- 6. Update status constraint to include new enum values
-- First, drop the existing implicit constraint (if any)
ALTER TABLE "payments"
DROP CONSTRAINT IF EXISTS chk_payment_status;

-- Add new constraint with expanded status values
ALTER TABLE "payments"
ADD CONSTRAINT chk_payment_status
CHECK (status IN ('PENDING', 'PROCESSING', 'CONFIRMED', 'DECLINED', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'UNKNOWN'));

-- 7. Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 8. Create trigger to automatically update updated_at
CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON "payments"
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();