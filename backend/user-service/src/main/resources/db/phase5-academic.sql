-- Giai doan 5 - P1: du lieu hoc vu (sinh vien, TKB, lich thi, lich gac thi, hoc ky).
-- Chay 1 lan tren DB lw-users, sau phase4-admin.sql. Idempotent.

-- Ho so sinh vien: MSSV, lop, CCCD (ma hoa AES-GCM, chi muc tra cuu la HMAC). username cua users = MSSV.
create table if not exists student_profiles (
    user_id      bigint primary key references users (id) on delete cascade,
    student_code varchar(16) not null unique,
    class_code   varchar(32),
    cccd_enc     text,
    cccd_index   varchar(64) unique,
    updated_at   timestamptz not null default now()
);
create index if not exists idx_student_profiles_class on student_profiles (class_code);

-- Hoc ky (lich hoc ky do nha truong cap)
create table if not exists academic_terms (
    code       varchar(32) primary key,
    name       varchar(255) not null,
    start_date date not null,
    end_date   date not null,
    check (end_date >= start_date)
);

-- Thoi khoa bieu: moi dong la mot buoi hoc (co ngay cu the)
create table if not exists course_sessions (
    id            bigserial primary key,
    class_code    varchar(32) not null,
    subject_code  varchar(32) not null,
    subject_name  varchar(255),
    group_code    varchar(32) not null default '',
    room          varchar(32),
    weekday       smallint not null,
    start_period  smallint not null,
    period_count  smallint not null,
    lecturer_code varchar(32),
    session_date  date not null,
    start_time    time not null,
    unique (class_code, subject_code, group_code, session_date, start_period)
);
create index if not exists idx_course_sessions_class on course_sessions (class_code, session_date);
create index if not exists idx_course_sessions_lecturer on course_sessions (lecturer_code, session_date);

-- Lich thi ket thuc mon
create table if not exists exam_schedules (
    id              bigserial primary key,
    subject_code    varchar(32) not null,
    subject_name    varchar(255),
    candidate_count integer,
    exam_date       date not null,
    exam_time       time not null,
    room            varchar(32) not null,
    campus          varchar(8),
    unique (subject_code, exam_date, exam_time, room)
);
create index if not exists idx_exam_schedules_subject on exam_schedules (subject_code, exam_date);

-- Lich gac thi cua giang vien
create table if not exists exam_duties (
    id            bigserial primary key,
    lecturer_code varchar(32) not null,
    subject_code  varchar(32),
    exam_date     date not null,
    exam_time     time not null,
    room          varchar(32) not null,
    campus        varchar(8),
    unique (lecturer_code, exam_date, exam_time, room)
);
create index if not exists idx_exam_duties_lecturer on exam_duties (lecturer_code, exam_date);