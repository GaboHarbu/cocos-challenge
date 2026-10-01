ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255) UNIQUE;
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(userId, status);
