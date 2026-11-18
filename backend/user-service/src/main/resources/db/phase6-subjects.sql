-- Danh mục môn học (import file Môn học, sửa và xóa trên giao diện).
-- Chạy một lần trên lw-users trước khi khởi động user-service.
create table if not exists subjects (
    code varchar(32) primary key,
    name varchar(255) not null,
    created_at timestamptz not null default now()
);

-- Tăng tốc tìm kiếm theo khoa, lớp, môn trên màn hình người dùng
create index if not exists idx_student_profiles_class on student_profiles (class_code);
create index if not exists idx_course_sessions_subject on course_sessions (subject_code);
create index if not exists idx_course_sessions_lecturer on course_sessions (lecturer_code);