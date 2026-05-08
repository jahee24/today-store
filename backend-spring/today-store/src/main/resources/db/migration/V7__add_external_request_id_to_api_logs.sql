-- V7: Add external_request_id to api_logs table
ALTER TABLE api_logs ADD COLUMN external_request_id VARCHAR(100);
