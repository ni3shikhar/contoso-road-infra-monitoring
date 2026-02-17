-- Road Infrastructure Monitoring - Database Initialization Script
-- Creates all required databases and grants permissions

-- Create databases
CREATE DATABASE sensor_db;
CREATE DATABASE asset_db;
CREATE DATABASE monitoring_db;
CREATE DATABASE alert_db;
CREATE DATABASE analytics_db;
CREATE DATABASE auth_db;

-- Grant all privileges to postgres user on all databases
GRANT ALL PRIVILEGES ON DATABASE sensor_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE asset_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE monitoring_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE alert_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;

-- Connect to each database and create extensions

-- Sensor DB
\c sensor_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Asset DB
\c asset_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- Monitoring DB
\c monitoring_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Alert DB
\c alert_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Analytics DB
\c analytics_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Auth DB
\c auth_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create default admin user in auth_db (password: admin123)
-- BCrypt hash for 'admin123'
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_locked, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@roadinfra.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/N3RxFnL.CtPrUNhVF9.EK',
    'System',
    'Administrator',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- Create roles table and insert default roles
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO roles (name, description) VALUES 
    ('ROLE_ADMIN', 'System Administrator with full access'),
    ('ROLE_OPERATOR', 'Operator with monitoring and alert management access'),
    ('ROLE_VIEWER', 'Read-only access to dashboards and reports')
ON CONFLICT DO NOTHING;

-- Create user_roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

PRINT 'Database initialization completed successfully!';
