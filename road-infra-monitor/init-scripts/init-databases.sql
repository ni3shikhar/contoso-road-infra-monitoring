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
