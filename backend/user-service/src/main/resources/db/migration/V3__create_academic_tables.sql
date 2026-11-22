
-- V3: Academic tables

CREATE TABLE faculties (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE school_classes (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,

    faculty_id BIGINT NOT NULL,
    cohort VARCHAR(8),
    advisor_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_school_classes_faculty
        FOREIGN KEY (faculty_id)
        REFERENCES faculties(id)
);

CREATE TABLE student_profiles (
    user_id BIGINT PRIMARY KEY,

    student_code VARCHAR(16) NOT NULL UNIQUE,
    class_code VARCHAR(32),

    cccd_enc TEXT,
    cccd_index VARCHAR(64) UNIQUE,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_student_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_student_profiles_class
    ON student_profiles(class_code);

CREATE TABLE subjects (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE academic_terms (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    CONSTRAINT chk_academic_terms_dates
        CHECK (end_date >= start_date)
);

CREATE TABLE course_sessions (
    id BIGSERIAL PRIMARY KEY,

    class_code VARCHAR(32) NOT NULL,
    subject_code VARCHAR(32) NOT NULL,
    subject_name VARCHAR(255),

    group_code VARCHAR(32) NOT NULL DEFAULT '',
    room VARCHAR(32),

    weekday SMALLINT NOT NULL,
    start_period SMALLINT NOT NULL,
    period_count SMALLINT NOT NULL,

    lecturer_code VARCHAR(32),
    lecturer_name VARCHAR(255),

    session_date DATE NOT NULL,
    start_time TIME NOT NULL,

    CONSTRAINT uq_course_sessions
        UNIQUE (
            class_code,
            subject_code,
            group_code,
            session_date,
            start_period
        )
);

CREATE INDEX idx_course_sessions_class
    ON course_sessions(class_code, session_date);

CREATE INDEX idx_course_sessions_lecturer
    ON course_sessions(lecturer_code, session_date);

CREATE INDEX idx_course_sessions_subject
    ON course_sessions(subject_code);

CREATE TABLE exam_schedules (
    id BIGSERIAL PRIMARY KEY,

    subject_code VARCHAR(32) NOT NULL,
    subject_name VARCHAR(255),

    candidate_count INTEGER,

    exam_date DATE NOT NULL,
    exam_time TIME NOT NULL,

    room VARCHAR(32) NOT NULL,
    campus VARCHAR(8),

    CONSTRAINT uq_exam_schedules
        UNIQUE (
            subject_code,
            exam_date,
            exam_time,
            room
        )
);

CREATE INDEX idx_exam_schedules_subject
    ON exam_schedules(subject_code, exam_date);

CREATE TABLE exam_duties (
    id BIGSERIAL PRIMARY KEY,

    lecturer_code VARCHAR(32) NOT NULL,
    subject_code VARCHAR(32),

    exam_date DATE NOT NULL,
    exam_time TIME NOT NULL,

    room VARCHAR(32) NOT NULL,
    campus VARCHAR(8),

    CONSTRAINT uq_exam_duties
        UNIQUE (
            lecturer_code,
            exam_date,
            exam_time,
            room
        )
);

CREATE INDEX idx_exam_duties_lecturer
    ON exam_duties(lecturer_code, exam_date);
