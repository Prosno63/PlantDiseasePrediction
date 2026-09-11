-- Fasol Doctor PostgreSQL schema
-- Database: inventory

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(255),
    profile_image_url VARCHAR(255),
    designation VARCHAR(255),
    qualification VARCHAR(255),
    specialization VARCHAR(255),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    district VARCHAR(255),
    upazila VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    role VARCHAR(32) NOT NULL DEFAULT 'FARMER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('FARMER', 'EXPERT', 'ADMIN', 'FIELD_WORKER'))
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refresh_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS refresh_tokens_user_idx ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS refresh_tokens_active_idx ON refresh_tokens (token, revoked);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(32) NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT role_permissions_permission_fk FOREIGN KEY (permission_id) REFERENCES permissions (id),
    CONSTRAINT role_permissions_unique UNIQUE (role, permission_id)
);

CREATE TABLE IF NOT EXISTS user_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT user_permissions_user_fk FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_permissions_permission_fk FOREIGN KEY (permission_id) REFERENCES permissions (id),
    CONSTRAINT user_permissions_unique UNIQUE (user_id, permission_id)
);

CREATE INDEX IF NOT EXISTS role_permissions_role_idx ON role_permissions (role);
CREATE INDEX IF NOT EXISTS user_permissions_user_idx ON user_permissions (user_id);

CREATE TABLE IF NOT EXISTS crop (
    id BIGSERIAL PRIMARY KEY,
    name_bn VARCHAR(255),
    name_en VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(255),
    selectable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS disease (
    id BIGSERIAL PRIMARY KEY,
    crop_id BIGINT NOT NULL,
    name_bn VARCHAR(255),
    name_en VARCHAR(255),
    model_class_label VARCHAR(255) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT disease_crop_fk FOREIGN KEY (crop_id) REFERENCES crop (id)
);

CREATE TABLE IF NOT EXISTS treatment (
    id BIGSERIAL PRIMARY KEY,
    disease_id BIGINT NOT NULL,
    text_bn TEXT,
    is_organic_priority BOOLEAN NOT NULL DEFAULT FALSE,
    source_note VARCHAR(255),
    verified_by VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT treatment_disease_fk FOREIGN KEY (disease_id) REFERENCES disease (id)
);

CREATE TABLE IF NOT EXISTS diagnosis (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT NOT NULL,
    crop_id BIGINT,
    input_type VARCHAR(32),
    input_text TEXT,
    image_path VARCHAR(255),
    disease_id BIGINT,
    disease_name_raw VARCHAR(255),
    confidence DOUBLE PRECISION,
    needs_expert_review BOOLEAN NOT NULL DEFAULT FALSE,
    ai_message TEXT,
    treatment_id BIGINT,
    outcome_feedback VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT diagnosis_farmer_fk FOREIGN KEY (farmer_id) REFERENCES users (id),
    CONSTRAINT diagnosis_crop_fk FOREIGN KEY (crop_id) REFERENCES crop (id),
    CONSTRAINT diagnosis_disease_fk FOREIGN KEY (disease_id) REFERENCES disease (id),
    CONSTRAINT diagnosis_treatment_fk FOREIGN KEY (treatment_id) REFERENCES treatment (id),
    CONSTRAINT diagnosis_input_type_check CHECK (input_type IN ('image', 'text', 'voice') OR input_type IS NULL),
    CONSTRAINT diagnosis_outcome_check CHECK (outcome_feedback IN ('yes', 'no', 'somewhat') OR outcome_feedback IS NULL)
);

CREATE TABLE IF NOT EXISTS conversation (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT NOT NULL,
    expert_id BIGINT,
    diagnosis_id BIGINT,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT conversation_farmer_fk FOREIGN KEY (farmer_id) REFERENCES users (id),
    CONSTRAINT conversation_expert_fk FOREIGN KEY (expert_id) REFERENCES users (id),
    CONSTRAINT conversation_diagnosis_fk FOREIGN KEY (diagnosis_id) REFERENCES diagnosis (id)
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    body TEXT,
    image_path VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT messages_conversation_fk FOREIGN KEY (conversation_id) REFERENCES conversation (id),
    CONSTRAINT messages_sender_fk FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS disease_crop_idx ON disease (crop_id);
CREATE INDEX IF NOT EXISTS treatment_disease_idx ON treatment (disease_id);
CREATE INDEX IF NOT EXISTS diagnosis_farmer_created_idx ON diagnosis (farmer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS diagnosis_disease_idx ON diagnosis (disease_id);
CREATE INDEX IF NOT EXISTS conversation_farmer_idx ON conversation (farmer_id);
CREATE INDEX IF NOT EXISTS conversation_expert_idx ON conversation (expert_id);
CREATE INDEX IF NOT EXISTS conversation_open_idx ON conversation (expert_id, resolved);
CREATE INDEX IF NOT EXISTS messages_conversation_created_idx ON messages (conversation_id, created_at);

ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS designation VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS qualification VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS available BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS online BOOLEAN NOT NULL DEFAULT FALSE;

select *
from user_permissions;

select *
from role_permissions;

select *
from permissions;
