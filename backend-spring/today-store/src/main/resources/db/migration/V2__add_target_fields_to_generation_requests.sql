-- V2__add_target_fields_to_generation_requests.sql

ALTER TABLE generation_requests ADD COLUMN target_age VARCHAR(50);
ALTER TABLE generation_requests ADD COLUMN target_gender VARCHAR(20);

COMMENT ON COLUMN generation_requests.target_age IS '타겟 연령대 (예: 20대, 3040 등)';
COMMENT ON COLUMN generation_requests.target_gender IS '타겟 성별';
