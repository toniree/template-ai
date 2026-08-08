-- Runs once, on first boot of an empty data directory. Creates the database the test suite uses;
-- POSTGRES_DB already created `sandbox` for the app.
CREATE DATABASE sandbox_test;
