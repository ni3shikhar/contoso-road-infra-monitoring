-- Road Infrastructure Monitoring - Database Initialization Script
-- Creates all required databases with extensions and grants permissions

-- ============================================
-- Create Databases
-- ============================================

CREATE DATABASE sensor_db;
CREATE DATABASE asset_db;
CREATE DATABASE monitoring_db;
CREATE DATABASE alert_db;
CREATE DATABASE analytics_db;
CREATE DATABASE auth_db;

-- ============================================
-- Grant Privileges
-- ============================================

GRANT ALL PRIVILEGES ON DATABASE sensor_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE asset_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE monitoring_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE alert_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;

-- ============================================
-- Sensor Database Setup
-- ============================================
\c sensor_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

COMMENT ON DATABASE sensor_db IS 'Database for sensor management and telemetry data';

-- ============================================
-- Asset Database Setup
-- ============================================
\c asset_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "postgis";

COMMENT ON DATABASE asset_db IS 'Database for asset lifecycle management with geospatial support';

-- ============================================
-- Monitoring Database Setup
-- ============================================
\c monitoring_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

COMMENT ON DATABASE monitoring_db IS 'Database for real-time health monitoring data';

-- ============================================
-- Alert Database Setup
-- ============================================
\c alert_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

COMMENT ON DATABASE alert_db IS 'Database for alert engine and notifications';

-- ============================================
-- Analytics Database Setup
-- ============================================
\c analytics_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

COMMENT ON DATABASE analytics_db IS 'Database for KPI computation and reporting';

-- ============================================
-- Auth Database Setup
-- ============================================
\c auth_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

COMMENT ON DATABASE auth_db IS 'Database for authentication and authorization';

-- Insert default admin user (password: admin123, BCrypt encoded)
-- This will be handled by the application on first startup
