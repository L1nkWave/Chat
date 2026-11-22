
-- V5: Seed roles

INSERT INTO roles (name, display_name)
VALUES
    ('ROLE_USER', 'Người dùng'),
    ('ROLE_ADMIN', 'Quản trị viên'),
    ('ROLE_LECTURER', 'Giảng viên'),
    ('ROLE_STUDENT', 'Sinh viên'),
    ('ROLE_ROOT', 'Quản trị cao nhất')
ON CONFLICT (name)
DO UPDATE SET
    display_name = COALESCE(
        roles.display_name,
        EXCLUDED.display_name
    );

-- Seed permissions

INSERT INTO permissions (code, description)
VALUES
    ('user.view', 'Xem thông tin tài khoản trong phạm vi'),
    ('user.lock', 'Khóa/mở khóa tài khoản trong phạm vi'),
    ('user.role.assign', 'Gán/thu hồi vai trò'),
    ('group.view', 'Xem nhóm trong phạm vi'),
    ('group.manage', 'Quản lý nhóm trong phạm vi'),
    ('announce.send', 'Gửi thông báo cho nhóm/lớp'),
    ('moderation.view', 'Xem tin nhắn bị báo cáo'),
    ('moderation.resolve', 'Xử lý báo cáo'),
    ('audit.view', 'Xem nhật ký hệ thống'),
    ('import.run', 'Chạy import Excel/CSV')
ON CONFLICT (code)
DO UPDATE SET
    description = EXCLUDED.description;

-- ROOT: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_ROOT'
ON CONFLICT DO NOTHING;

-- ADMIN permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
AND p.code IN (
    'user.view',
    'user.lock',
    'group.view',
    'group.manage',
    'announce.send',
    'moderation.view',
    'moderation.resolve',
    'audit.view',
    'import.run'
)
ON CONFLICT DO NOTHING;

-- LECTURER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_LECTURER'
AND p.code IN (
    'group.view',
    'group.manage',
    'announce.send'
)
ON CONFLICT DO NOTHING;
