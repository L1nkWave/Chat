
-- V1: Identity tables

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32),
    password VARCHAR(255) NOT NULL,
    name VARCHAR(64) NOT NULL,
    avatar_path VARCHAR(255),

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    theme BOOLEAN NOT NULL DEFAULT FALSE,
    bio VARCHAR(255),

    tokens_valid_after TIMESTAMPTZ,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(128),

    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    users_id BIGINT NOT NULL,
    roles_id INTEGER NOT NULL,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (users_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (roles_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_roles
        UNIQUE (roles_id, users_id)
);

CREATE INDEX idx_user_roles_users_id
    ON user_roles(users_id);

CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,

    user_id_1 BIGINT,
    user_id_2 BIGINT,

    added_at TIMESTAMPTZ DEFAULT NOW(),
    alias VARCHAR(64) NOT NULL,

    CONSTRAINT fk_contacts_owner
        FOREIGN KEY (user_id_1)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_contacts_user
        FOREIGN KEY (user_id_2)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_contacts_user_id_1
    ON contacts(user_id_1);

CREATE INDEX idx_contacts_user_id_2
    ON contacts(user_id_2);
