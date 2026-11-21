-- Ensure avatar_path exists in older databases.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_path VARCHAR(255);


-- Ensure auth-service token table exists.
CREATE TABLE IF NOT EXISTS deactivated_tokens
(
    id         UUID PRIMARY KEY,
    expiration TIMESTAMPTZ NOT NULL
);


-- Ensure default roles exist.
INSERT INTO roles (name)
VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;