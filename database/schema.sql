-- Fasol Doctor PostgreSQL master schema.
-- Run this file against the application database in order.
-- It is safe to run repeatedly. Existing legacy refresh tokens are revoked.

BEGIN;

-- 1. Core users table.
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(255),
    profile_image_url VARCHAR(255),
    designation VARCHAR(255),
    qualification VARCHAR(255),
    specialization VARCHAR(255),
    visit_address VARCHAR(255),
    expert_type VARCHAR(255),
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

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS accepting_consultations BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. Add columns introduced after the original users table was deployed.
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS designation VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS qualification VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS visit_address VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS expert_type VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS available BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS online BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS district VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS upazila VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- 3. Refresh tokens store only SHA-256 hashes, never bearer tokens.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT,
    CONSTRAINT refresh_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Upgrade databases that still have the old plaintext token column.
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS version BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'refresh_tokens'
          AND column_name = 'token'
    ) THEN
        -- Old tokens cannot be trusted after changing storage format; revoke them.
        UPDATE refresh_tokens
        SET token_hash = md5(random()::text || clock_timestamp()::text)
                       || md5(random()::text || clock_timestamp()::text),
            revoked = TRUE
        WHERE token_hash IS NULL;
        ALTER TABLE refresh_tokens DROP COLUMN token;
    END IF;
END $$;

-- Any incomplete prior migration is also made safe by invalidating its null rows.
UPDATE refresh_tokens
SET token_hash = md5(random()::text || clock_timestamp()::text)
               || md5(random()::text || clock_timestamp()::text),
    revoked = TRUE
WHERE token_hash IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS refresh_tokens_token_hash_uidx ON refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS refresh_tokens_user_idx ON refresh_tokens (user_id);
DROP INDEX IF EXISTS refresh_tokens_active_idx;
CREATE INDEX IF NOT EXISTS refresh_tokens_active_idx ON refresh_tokens (token_hash, revoked);

-- 4. Crop, disease, and treatment knowledge base.
CREATE TABLE IF NOT EXISTS crop (
    id BIGSERIAL PRIMARY KEY,
    name_bn VARCHAR(255),
    name_en VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(255),
    selectable BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE crop
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Backfill existing rows that still hold the default so display_order becomes sequential 1..N.
UPDATE crop c
SET display_order = sub.rn
FROM (SELECT id, row_number() OVER (ORDER BY id) AS rn FROM crop) sub
WHERE c.id = sub.id AND (c.display_order = 0 OR c.display_order IS NULL);

UPDATE users u
SET display_order = sub.rn
FROM (SELECT id, row_number() OVER (ORDER BY id) AS rn FROM users WHERE role = 'EXPERT') sub
WHERE u.id = sub.id AND u.role = 'EXPERT' AND (u.display_order = 0 OR u.display_order IS NULL);

-- Expert crop areas (many-to-many between experts and crops).
CREATE TABLE IF NOT EXISTS user_crop_ids (
    user_id BIGINT NOT NULL,
    crop_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, crop_id),
    CONSTRAINT user_crop_ids_user_fk FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_crop_ids_crop_fk FOREIGN KEY (crop_id) REFERENCES crop (id)
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

-- 5. Diagnosis history.
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

-- 6. Expert conversations and messages.
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

-- 7. Query indexes used by the API.
CREATE INDEX IF NOT EXISTS disease_crop_idx ON disease (crop_id);
CREATE INDEX IF NOT EXISTS treatment_disease_idx ON treatment (disease_id);
CREATE INDEX IF NOT EXISTS diagnosis_farmer_created_idx ON diagnosis (farmer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS diagnosis_disease_idx ON diagnosis (disease_id);
CREATE INDEX IF NOT EXISTS conversation_farmer_idx ON conversation (farmer_id);
CREATE INDEX IF NOT EXISTS conversation_expert_idx ON conversation (expert_id);
CREATE INDEX IF NOT EXISTS conversation_open_idx ON conversation (expert_id, resolved);
CREATE INDEX IF NOT EXISTS messages_conversation_created_idx ON messages (conversation_id, created_at);

COMMIT;
