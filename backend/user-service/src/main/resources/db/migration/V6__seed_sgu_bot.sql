
-- V6: Seed SGU Chat bot

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (
    username,
    password,
    name
)
VALUES (
    'sgu_bot',
    crypt(gen_random_uuid()::text, gen_salt('bf', 10)),
    'Trợ lý SGU'
)
ON CONFLICT (username) DO NOTHING;
