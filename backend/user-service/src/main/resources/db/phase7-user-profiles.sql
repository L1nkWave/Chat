-- Giai doan 7 - Ho so ca nhan nguoi dung (dung chung DB lw-users, user-service)
-- Chay 1 lan tren DB lw-users. Idempotent.
-- Khong backfill tu student_profiles: service doc CCCD tu ca hai bang (coalesce), nen du lieu cu van hien thi.

create table if not exists user_profiles (
    user_id       bigint primary key references users (id) on delete cascade,
    gender        varchar(8)  check (gender in ('MALE', 'FEMALE', 'OTHER')),
    birth_date    date,
    -- SV: STUDYING, GRADUATED, SUSPENDED, DROPPED_OUT | GV: WORKING, ON_LEAVE, RETIRED | QTV: WORKING
    status        varchar(16) check (status in (
                      'STUDYING', 'GRADUATED', 'SUSPENDED', 'DROPPED_OUT',
                      'WORKING', 'ON_LEAVE', 'RETIRED')),
    phone         varchar(20),
    birthplace    varchar(255),
    nationality   varchar(64),
    address       varchar(255),
    -- Trường nhạy cảm: mã hóa AES-GCM (CccdCipher), chỉ mục HMAC để chống trùng
    cccd_enc      text,
    cccd_index    varchar(64),
    ethnicity_enc text,
    religion_enc  text,
    updated_at    timestamptz not null default now()
);

create index if not exists idx_user_profiles_cccd_index on user_profiles (cccd_index);