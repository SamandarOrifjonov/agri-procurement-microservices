-- PostgreSQL Database Setup Script
-- Run this script to create databases for all microservices
-- Execute: psql -U postgres -f setup-databases.sql

-- Create databases
CREATE DATABASE procurement_db;
CREATE DATABASE supplier_db;
CREATE DATABASE contract_db;

-- Grant privileges (if needed)
GRANT ALL PRIVILEGES ON DATABASE procurement_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE supplier_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE contract_db TO postgres;

-- Verify databases
