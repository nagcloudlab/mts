-- ============================================
-- Seed data for H2 (default profile)
-- Auto-loaded on every startup since ddl-auto: create-drop
-- ============================================

-- Users
INSERT INTO users (username, email, password_hash, full_name, phone, is_active) VALUES
('ravi.kumar', 'ravi@example.com', '$2a$10$dummyhash1', 'Ravi Kumar', '9876543210', true);
INSERT INTO users (username, email, password_hash, full_name, phone, is_active) VALUES
('priya.sharma', 'priya@example.com', '$2a$10$dummyhash2', 'Priya Sharma', '9876543211', true);
INSERT INTO users (username, email, password_hash, full_name, phone, is_active) VALUES
('amit.patel', 'amit@example.com', '$2a$10$dummyhash3', 'Amit Patel', '9876543212', true);

-- Accounts
INSERT INTO accounts (account_number, user_id, account_type, balance, currency, is_active) VALUES
('ACC001', 1, 'SAVINGS', 50000.00, 'INR', true);
INSERT INTO accounts (account_number, user_id, account_type, balance, currency, is_active) VALUES
('ACC002', 1, 'CURRENT', 120000.00, 'INR', true);
INSERT INTO accounts (account_number, user_id, account_type, balance, currency, is_active) VALUES
('ACC003', 2, 'SAVINGS', 75000.00, 'INR', true);
INSERT INTO accounts (account_number, user_id, account_type, balance, currency, is_active) VALUES
('ACC004', 3, 'SAVINGS', 30000.00, 'INR', true);
INSERT INTO accounts (account_number, user_id, account_type, balance, currency, is_active) VALUES
('ACC005', 3, 'CURRENT', 200000.00, 'INR', true);
