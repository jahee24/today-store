-- v1__init_schema.sql

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id)
);

CREATE TABLE IF NOT EXISTS stores (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    store_name VARCHAR(200) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    preferred_style VARCHAR(50) NOT NULL DEFAULT 'CLEAN',
    sns_instagram VARCHAR(255),
    sns_naver_url TEXT,
    sns_karrot_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stores_user FOREIGN KEY (user_id) REFERENCES users (id)
);


CREATE TABLE IF NOT EXISTS generation_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    concept VARCHAR(255) NOT NULL,
    additional_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_generation_requests_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS input_images (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_input_images_generation_request FOREIGN KEY (request_id) REFERENCES generation_requests (id)
);

CREATE TABLE IF NOT EXISTS contents (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    instagram_text TEXT,
    instagram_hashtags JSONB,
    karrot_text TEXT,
    karrot_tags JSONB,
    naver_text TEXT,
    naver_keywords JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generation_type VARCHAR(20) NOT NULL,
    ai_model VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_contents_generation_request FOREIGN KEY (request_id) REFERENCES generation_requests (id)
);

CREATE TABLE IF NOT EXISTS content_images (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    input_image_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_images_content FOREIGN KEY (content_id) REFERENCES contents (id),
    CONSTRAINT fk_content_images_input_image FOREIGN KEY (input_image_id) REFERENCES input_images (id)
);


CREATE TABLE IF NOT EXISTS api_logs (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    content_id UUID,
    model VARCHAR(100) NOT NULL,
    input_tokens INT,
    output_tokens INT,
    cost_usd DECIMAL(10, 6),
    response_time_ms INT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_api_logs_generation_request FOREIGN KEY (request_id) REFERENCES generation_requests (id),
    CONSTRAINT fk_api_logs_content FOREIGN KEY (content_id) REFERENCES contents (id)
);

CREATE TABLE IF NOT EXISTS content_posts (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    platform VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    external_id VARCHAR(255),
    post_url VARCHAR(500),
    published_at TIMESTAMP,
    CONSTRAINT fk_content_posts_content FOREIGN KEY (content_id) REFERENCES contents (id)
);

CREATE TABLE IF NOT EXISTS user_social_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    platform VARCHAR(20) NOT NULL,
    social_user_id VARCHAR(255) NOT NULL,
    username VARCHAR(100),
    access_token TEXT NOT NULL,
    token_expires_at TIMESTAMP,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_user_social_account_platform_social_user_id UNIQUE (platform, social_user_id)
);

CREATE TABLE IF NOT EXISTS billing_methods (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    billing_key VARCHAR(200) UNIQUE NOT NULL,
    customer_key VARCHAR(300) NOT NULL,
    card_company VARCHAR(50),
    card_number VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    authenticated_at TIMESTAMP NOT NULL,
    bank_name VARCHAR(50),
    bank_account_number VARCHAR(30),
    CONSTRAINT fk_billing_methods_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    billing_method_id UUID,
    tier VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    next_billing_at TIMESTAMP,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    monthly_fee INT NOT NULL,
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_subscriptions_billing_method FOREIGN KEY (billing_method_id) REFERENCES billing_methods (id)
);

CREATE TABLE IF NOT EXISTS payment_history (
    id UUID PRIMARY KEY,
    subscription_id UUID,
    order_id VARCHAR(100) UNIQUE NOT NULL,
    payment_key VARCHAR(200),
    amount INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    failure_code VARCHAR(50),
    receipt_url TEXT,
    CONSTRAINT fk_payment_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
);

-- create index
-- 1. 히스토리 조회 최적화
CREATE INDEX idx_generation_requests_user_id ON generation_requests (user_id);

-- 2. 콘텐츠, 이미지 조회
CREATE INDEX idx_input_images_request_id ON input_images (request_id);
CREATE INDEX idx_contents_request_id ON contents (request_id);
CREATE INDEX idx_content_images_content_id ON content_images (content_id);
CREATE INDEX idx_content_posts_content_id ON content_posts (content_id);

-- 3. AI 호출 로그
CREATE INDEX idx_api_logs_request_id ON api_logs (request_id);

-- 4. 결제 및 구독
CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_payment_history_subscription_id ON payment_history (subscription_id);