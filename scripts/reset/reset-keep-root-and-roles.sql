-- =====================================================================
-- reset-keep-root-and-roles.sql
-- Database: lw-users (PostgreSQL 16, docker host port 15432, schema public)
--
-- TRẠNG THÁI: CHỈ LÀ FILE. Chưa chạy. Chạy thủ công sau khi đã đọc kỹ:
--   psql -h localhost -p 15432 -U postgres -d lw-users -f reset-keep-root-and-roles.sql
-- Mặc định kết thúc bằng COMMIT bị comment: nếu không bỏ comment,
-- giao dịch bị hủy khi phiên psql kết thúc (không có thay đổi nào được ghi).
--
-- GIỮ LẠI (không xóa):
--   users               chỉ tài khoản username = 'root'
--   user_roles          các hàng của root
--   roles               toàn bộ (gồm ROLE_ROOT, ROLE_ADMIN, ROLE_LECTURER, ROLE_STUDENT, ROLE_USER)
--   permissions         toàn bộ
--   role_permissions    toàn bộ
--   scoped_roles        các grant có user_id = root
--   totp_secrets        hàng của root
--   totp_recovery_codes hàng của root
--
-- XÓA:
--   users               mọi tài khoản khác root (gồm sgu_bot, giảng viên, sinh viên, admin)
--   user_roles          hàng của tài khoản khác root
--   scoped_roles        grant của tài khoản khác root
--   totp_secrets        của tài khoản khác root
--   totp_recovery_codes của tài khoản khác root
--   contacts            toàn bộ (bảng không có trong danh sách yêu cầu, xóa để không còn tham chiếu tới user đã xóa)
--   student_profiles    toàn bộ (học vụ)
--   user_profiles       toàn bộ, kể cả hồ sơ của root (theo yêu cầu "xóa sạch")
--   course_sessions     toàn bộ (học vụ, thời khóa biểu)
--   exam_schedules      toàn bộ (học vụ, lịch thi)
--   exam_duties         toàn bộ (học vụ, lịch gác thi)
--   academic_terms      toàn bộ (học vụ, học kỳ)
--   subjects            toàn bộ (học vụ, môn học)
--   school_classes      toàn bộ (học vụ, lớp)
--   faculties           toàn bộ (học vụ, khoa)
--   audit_logs          toàn bộ, NHẬT KÝ KIỂM TOÁN (cùng tên bảng dùng cho auth-service và user-service).
--                       Nếu muốn giữ nhật ký, comment dòng "DELETE FROM audit_logs;" bên dưới.
--   deactivated_tokens  toàn bộ (danh sách token bị vô hiệu hóa, tức bảng token). Hệ quả: token đã bị thu hồi
--                       trước đó có thể dùng lại cho tới khi hết hạn tự nhiên.
--
-- KHÔNG ĐỤNG VÀO: các bảng khác (không có trong danh sách trên).
-- REDIS (redis-sessions, host port 16379): script này KHÔNG chạy lệnh Redis. Lệnh tùy chọn, chỉ để tham khảo:
--   -- redis-cli -h localhost -p 16379 FLUSHDB      (xóa phiên đăng nhập, bộ đếm đăng nhập sai, khóa tài khoản;
--                                                   kiểm tra không có dữ liệu khác trước khi chạy)
--
-- SAU KHI RESET: chạy lại db/phase2-bot.sql để tạo lại tài khoản sgu_bot (đã bị xóa).
-- Các bảng roles/permissions/role_permissions đã giữ nên không cần chạy lại phase3.
-- Nếu một bảng ở trên không tồn tại trong DB, câu lệnh tương ứng báo lỗi và giao dịch bị hủy (không ghi gì).
-- =====================================================================

-- ---------------------------------------------------------------------
-- A. ĐẾM DÒNG TRƯỚC KHI RESET (chạy riêng, ngoài giao dịch)
-- ---------------------------------------------------------------------
SELECT 'users' AS bang, count(*) AS so_dong FROM users
UNION ALL SELECT 'user_roles', count(*) FROM user_roles
UNION ALL SELECT 'roles', count(*) FROM roles
UNION ALL SELECT 'permissions', count(*) FROM permissions
UNION ALL SELECT 'role_permissions', count(*) FROM role_permissions
UNION ALL SELECT 'scoped_roles', count(*) FROM scoped_roles
UNION ALL SELECT 'totp_secrets', count(*) FROM totp_secrets
UNION ALL SELECT 'totp_recovery_codes', count(*) FROM totp_recovery_codes
UNION ALL SELECT 'contacts', count(*) FROM contacts
UNION ALL SELECT 'deactivated_tokens', count(*) FROM deactivated_tokens
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs
UNION ALL SELECT 'faculties', count(*) FROM faculties
UNION ALL SELECT 'school_classes', count(*) FROM school_classes
UNION ALL SELECT 'student_profiles', count(*) FROM student_profiles
UNION ALL SELECT 'user_profiles', count(*) FROM user_profiles
UNION ALL SELECT 'course_sessions', count(*) FROM course_sessions
UNION ALL SELECT 'exam_schedules', count(*) FROM exam_schedules
UNION ALL SELECT 'exam_duties', count(*) FROM exam_duties
UNION ALL SELECT 'academic_terms', count(*) FROM academic_terms
UNION ALL SELECT 'subjects', count(*) FROM subjects;

-- ---------------------------------------------------------------------
-- B. RESET (một giao dịch)
-- ---------------------------------------------------------------------
BEGIN;

-- B1. Kiểm tra an toàn: phải có đúng một tài khoản root, nếu không thì dừng
DO $$
BEGIN
  IF (SELECT count(*) FROM users WHERE username = 'root') <> 1 THEN
    RAISE EXCEPTION 'Phải có đúng 1 tài khoản username = root. Dừng reset.';
  END IF;

  IF (
      SELECT count(*)
      FROM users
      WHERE username = 'sgu_bot'
  ) <> 1 THEN
    RAISE EXCEPTION
      'Phải có đúng 1 tài khoản sgu_bot. Dừng reset.';
  END IF;
END $$;

-- B2. Lưu id của root (bảng tạm, tự xóa khi kết thúc giao dịch)
CREATE TEMP TABLE keep_system_users ON COMMIT DROP AS
SELECT id
FROM users
WHERE username IN ('root', 'sgu_bot');

-- B3. Xóa theo thứ tự bảng con trước, bảng cha sau
DELETE FROM contacts;
DELETE FROM scoped_roles       WHERE user_id NOT IN (SELECT id FROM keep_system_users);
DELETE FROM user_roles         WHERE users_id NOT IN (SELECT id FROM keep_system_users);
DELETE FROM totp_recovery_codes WHERE user_id NOT IN (SELECT id FROM keep_system_users);
DELETE FROM totp_secrets       WHERE user_id NOT IN (SELECT id FROM keep_system_users);

DELETE FROM student_profiles;
DELETE FROM user_profiles;
DELETE FROM course_sessions;
DELETE FROM exam_schedules;
DELETE FROM exam_duties;
DELETE FROM academic_terms;
DELETE FROM subjects;
DELETE FROM school_classes;     -- phải xóa trước faculties (khóa ngoại faculty_id)
DELETE FROM faculties;

DELETE FROM audit_logs;         -- nhật ký kiểm toán: comment dòng này nếu muốn giữ

DELETE FROM users
WHERE id NOT IN (
    SELECT id FROM keep_system_users
);

-- ---------------------------------------------------------------------
-- C. ĐẾM DÒNG SAU KHI RESET (vẫn trong giao dịch)
-- Kỳ vọng:
-- users = 2 (root và sgu_bot)
-- roles / permissions / role_permissions không đổi
-- user_roles chỉ còn các bản ghi liên quan đến tài khoản hệ thống
-- user_profiles và dữ liệu học vụ = 0
-- contacts = 0
-- audit_logs = 0
-- deactivated_tokens được giữ nguyên
-- ---------------------------------------------------------------------
SELECT 'users' AS bang, count(*) AS so_dong FROM users
UNION ALL SELECT 'user_roles', count(*) FROM user_roles
UNION ALL SELECT 'roles', count(*) FROM roles
UNION ALL SELECT 'permissions', count(*) FROM permissions
UNION ALL SELECT 'role_permissions', count(*) FROM role_permissions
UNION ALL SELECT 'scoped_roles', count(*) FROM scoped_roles
UNION ALL SELECT 'totp_secrets', count(*) FROM totp_secrets
UNION ALL SELECT 'totp_recovery_codes', count(*) FROM totp_recovery_codes
UNION ALL SELECT 'contacts', count(*) FROM contacts
UNION ALL SELECT 'deactivated_tokens', count(*) FROM deactivated_tokens
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs
UNION ALL SELECT 'faculties', count(*) FROM faculties
UNION ALL SELECT 'school_classes', count(*) FROM school_classes
UNION ALL SELECT 'student_profiles', count(*) FROM student_profiles
UNION ALL SELECT 'user_profiles', count(*) FROM user_profiles
UNION ALL SELECT 'course_sessions', count(*) FROM course_sessions
UNION ALL SELECT 'exam_schedules', count(*) FROM exam_schedules
UNION ALL SELECT 'exam_duties', count(*) FROM exam_duties
UNION ALL SELECT 'academic_terms', count(*) FROM academic_terms
UNION ALL SELECT 'subjects', count(*) FROM subjects;

SELECT id, username
FROM users
WHERE username IN ('root', 'sgu_bot')
ORDER BY username;

-- Bỏ comment COMMIT khi đã kiểm tra số dòng ở mục A và C.
-- COMMIT;
