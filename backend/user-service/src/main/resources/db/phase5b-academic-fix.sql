-- Phase 5b: thời khóa biểu ghi giảng viên bằng họ tên (cột GV), không phải mã.
-- Chạy sau phase5-academic.sql. Dữ liệu cũ giữ nguyên, lecturer_name để trống cho đến khi nhập lại file.
alter table course_sessions add column if not exists lecturer_name varchar(255);