
-- V2: Authorization and audit

CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,

    CONSTRAINT pk_role_permissions
        PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE
);

CREATE TABLE scoped_roles (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    role_id INTEGER NOT NULL,

    scope_type VARCHAR(16) NOT NULL,
    scope_id BIGINT,

    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_scoped_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_scoped_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_scoped_roles_scope_type
        CHECK (
            scope_type IN ('SCHOOL', 'FACULTY', 'CLASS')
        ),

    CONSTRAINT chk_scoped_roles_scope_id
        CHECK (
            (scope_type = 'SCHOOL' AND scope_id IS NULL)
            OR
            (scope_type <> 'SCHOOL' AND scope_id IS NOT NULL)
        )
);

CREATE INDEX idx_scoped_roles_user
    ON scoped_roles(user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_scoped_roles_role
    ON scoped_roles(role_id);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,

    actor_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,

    target_type VARCHAR(32),
    target_id VARCHAR(64),

    detail TEXT,
    ip VARCHAR(64),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created
    ON audit_logs(created_at DESC);

CREATE INDEX idx_audit_logs_actor
    ON audit_logs(actor_id);
