-- V3__add_updated_at_field_to_generation_requests.sql

-- updated_at 컬럼 추가 (기본값 현재 시간)
ALTER TABLE generation_requests ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 기존 데이터의 updated_at을 created_at과 동일하게 설정
UPDATE generation_requests SET updated_at = created_at;

COMMENT ON COLUMN generation_requests.updated_at IS '수정 일시';
