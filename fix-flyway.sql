-- Fix Flyway schema history after dropping and recreating daily_summary table
-- Run this SQL in your PostgreSQL database (p2p_ledger)

-- Option 1: Delete only the problematic migration entry
DELETE FROM flyway_schema_history WHERE version = '010';

-- Option 2: If you want to completely reset (USE WITH CAUTION!)
-- This will delete ALL migration history and Flyway will re-run everything
-- TRUNCATE flyway_schema_history;

-- After running this, restart your Spring Boot application with:
-- mvn spring-boot:run
