
-- V4: Profiles and authentication

CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,

    gender VARCHAR(8),
    birth_date DATE,

    status VARCHAR(16),

    phone VARCHAR(20),
    birthplace VARCHAR(255),
    nationality VARCHAR(64),
    address VARCHAR(255),

    cccd_enc TEXT,
    cccd_index VARCHAR(64),

    ethnicity_enc TEXT,
    religion_enc TEXT,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_user_profiles_gender
        CHECK (
            gender IS NULL
            OR gender IN ('MALE', 'FEMALE', 'OTHER')
        ),

    CONSTRAINT chk_user_profiles_status
        CHECK (
            status IS NULL
            OR status IN (
                'STUDYING',
                'GRADUATED',
                'SUSPENDED',
                'DROPPED_OUT',
                'WORKING',
                'ON_LEAVE',
                'RETIRED'
            )
        )
);

CREATE INDEX idx_user_profiles_cccd_index
    ON user_profiles(cccd_index);

-- Auth: revoked tokens

CREATE TABLE deactivated_tokens (
    id UUID PRIMARY KEY,
    expiration TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_deactivated_tokens_expiration
    ON deactivated_tokens(expiration);

-- Auth: TOTP secret

CREATE TABLE totp_secrets (
    user_id BIGINT PRIMARY KEY,

    secret_encrypted TEXT NOT NULL,
    key_version INTEGER NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMPTZ,

    last_used_step BIGINT NOT NULL DEFAULT -1,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    enabled_at TIMESTAMPTZ,

    CONSTRAINT fk_totp_secrets_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Auth: one-time recovery codes

CREATE TABLE totp_recovery_codes (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    code_hash VARCHAR(100) NOT NULL,

    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_totp_recovery_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_totp_recovery_user_unused
    ON totp_recovery_codes(user_id)
    WHERE used_at IS NULL;
