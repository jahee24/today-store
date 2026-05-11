-- V6__create_input_image_variations_table.sql

CREATE TABLE IF NOT EXISTS input_image_variations (
    id UUID PRIMARY KEY,
    input_image_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    angle_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_input_image_variations_input_image FOREIGN KEY (input_image_id) REFERENCES input_images (id)
);

-- 인덱스 추가: 원본 이미지별 변형 이미지 조회 최적화
CREATE INDEX idx_input_image_variations_input_image_id ON input_image_variations (input_image_id);

COMMENT ON TABLE input_image_variations IS '원본 이미지의 각도별 AI 변형 이미지 저장 테이블';
COMMENT ON COLUMN input_image_variations.id IS '변형 이미지 ID';
COMMENT ON COLUMN input_image_variations.input_image_id IS '원본 이미지 ID';
COMMENT ON COLUMN input_image_variations.url IS 'GCS에 저장된 변형 이미지 URL';
COMMENT ON COLUMN input_image_variations.angle_type IS '변형 각도 타입 (예: Close up, Wide shot 등)';
COMMENT ON COLUMN input_image_variations.created_at IS '생성 일시';
