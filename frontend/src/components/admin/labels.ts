/** Nhãn tiếng Việt dùng chung cho màn hình quản trị. Mã kỹ thuật chỉ hiện khi không có nhãn. */

export const ROLE_LABEL: Record<string, string> = {
    ROLE_ROOT: "Quản trị cao nhất",
    ROLE_ADMIN: "Quản trị viên",
    ROLE_LECTURER: "Giảng viên",
    ROLE_STUDENT: "Sinh viên",
    ROLE_USER: "Người dùng",
};

export const SCOPE_LABEL: Record<"SCHOOL" | "FACULTY" | "CLASS", string> = {
    SCHOOL: "Toàn trường",
    FACULTY: "Khoa",
    CLASS: "Lớp",
};

export const GENDER_LABEL: Record<string, string> = {
    MALE: "Nam",
    FEMALE: "Nữ",
    OTHER: "Khác",
};

/** Tình trạng: sinh viên là tình trạng học tập, giảng viên và quản trị viên là tình trạng làm việc */
export const STATUS_LABEL: Record<string, string> = {
    STUDYING: "Đang học",
    GRADUATED: "Đã tốt nghiệp",
    SUSPENDED: "Bảo lưu",
    DROPPED_OUT: "Thôi học",
    WORKING: "Đang làm việc",
    ON_LEAVE: "Nghỉ phép",
    RETIRED: "Nghỉ hưu",
};

/** Tình trạng theo vai trò, dùng cho ô chọn trong form */
export const STATUS_OPTIONS_BY_ROLE: Record<string, { value: string; label: string }[]> = {
    ROLE_STUDENT: [
        { value: "STUDYING", label: "Đang học" },
        { value: "GRADUATED", label: "Đã tốt nghiệp" },
        { value: "SUSPENDED", label: "Bảo lưu" },
        { value: "DROPPED_OUT", label: "Thôi học" },
    ],
    ROLE_LECTURER: [
        { value: "WORKING", label: "Đang làm việc" },
        { value: "ON_LEAVE", label: "Nghỉ phép" },
        { value: "RETIRED", label: "Nghỉ hưu" },
    ],
    ROLE_ADMIN: [{ value: "WORKING", label: "Đang làm việc" }],
};

const KIND_LABEL: Record<string, string> = {
    FACULTIES: "khoa",
    CLASSES: "lớp",
    SUBJECTS: "môn học",
    LECTURERS: "giảng viên",
    STUDENTS: "sinh viên",
    TIMETABLE: "thời khóa biểu",
    EXAMS: "lịch thi",
    DUTIES: "lịch gác thi",
    TERMS: "học kỳ",
};

const ACTION_LABEL: Record<string, string> = {
    USER_CREATE: "Tạo tài khoản",
    USER_EDIT: "Sửa thông tin người dùng",
    USER_LOCK: "Khóa tài khoản",
    USER_UNLOCK: "Mở khóa tài khoản",
    PASSWORD_RESET: "Đặt lại mật khẩu",
    CCCD_REVEAL: "Xem CCCD đầy đủ",
    SENSITIVE_REVEAL: "Xem thông tin nhạy cảm",
    ROLE_SET: "Đổi vai trò toàn trường",
    ROLE_CREATE: "Tạo vai trò",
    ROLE_PERMISSIONS_UPDATE: "Sửa quyền của vai trò",
    ROLE_GRANT: "Cấp vai trò",
    ROLE_REVOKE: "Thu hồi vai trò",
    FACULTY_CREATE: "Thêm khoa",
    FACULTY_UPDATE: "Sửa khoa",
    FACULTY_DELETE: "Xóa khoa",
    CLASS_CREATE: "Thêm lớp",
    CLASS_UPDATE: "Sửa lớp",
    CLASS_DELETE: "Xóa lớp",
    SUBJECT_CREATE: "Thêm môn học",
    RECORD_UPDATE: "Sửa bản ghi học vụ",
    RECORD_DELETE: "Xóa bản ghi học vụ",
    TERM_SAVE: "Lưu học kỳ",
};

/** Nhãn cho một mã hành động nhật ký, kể cả các hành động nhập và thêm thủ công theo loại dữ liệu */
export function actionLabel(action: string): string {
    if (ACTION_LABEL[action]) return ACTION_LABEL[action];
    if (action.startsWith("IMPORT_")) {
        const kind = action.slice("IMPORT_".length);
        return `Nhập dữ liệu ${KIND_LABEL[kind] ?? kind}`;
    }
    if (action.startsWith("MANUAL_ADD_")) {
        const kind = action.slice("MANUAL_ADD_".length);
        return `Thêm thủ công ${KIND_LABEL[kind] ?? kind}`;
    }
    return action;
}

/** Lựa chọn cho bộ lọc nhật ký: các hành động chính, các hành động nhập/thêm không liệt kê hết */
export const AUDIT_ACTION_OPTIONS: { value: string; label: string }[] = [
    { value: "", label: "Tất cả hành động" },
    { value: "USER_CREATE", label: ACTION_LABEL.USER_CREATE },
    { value: "USER_EDIT", label: ACTION_LABEL.USER_EDIT },
    { value: "USER_LOCK", label: ACTION_LABEL.USER_LOCK },
    { value: "USER_UNLOCK", label: ACTION_LABEL.USER_UNLOCK },
    { value: "PASSWORD_RESET", label: ACTION_LABEL.PASSWORD_RESET },
    { value: "CCCD_REVEAL", label: ACTION_LABEL.CCCD_REVEAL },
    { value: "SENSITIVE_REVEAL", label: ACTION_LABEL.SENSITIVE_REVEAL },
    { value: "ROLE_SET", label: ACTION_LABEL.ROLE_SET },
    { value: "ROLE_CREATE", label: ACTION_LABEL.ROLE_CREATE },
    { value: "ROLE_PERMISSIONS_UPDATE", label: ACTION_LABEL.ROLE_PERMISSIONS_UPDATE },
    { value: "ROLE_GRANT", label: ACTION_LABEL.ROLE_GRANT },
    { value: "ROLE_REVOKE", label: ACTION_LABEL.ROLE_REVOKE },
    { value: "FACULTY_CREATE", label: ACTION_LABEL.FACULTY_CREATE },
    { value: "FACULTY_UPDATE", label: ACTION_LABEL.FACULTY_UPDATE },
    { value: "FACULTY_DELETE", label: ACTION_LABEL.FACULTY_DELETE },
    { value: "CLASS_CREATE", label: ACTION_LABEL.CLASS_CREATE },
    { value: "CLASS_UPDATE", label: ACTION_LABEL.CLASS_UPDATE },
    { value: "CLASS_DELETE", label: ACTION_LABEL.CLASS_DELETE },
    { value: "SUBJECT_CREATE", label: ACTION_LABEL.SUBJECT_CREATE },
    { value: "RECORD_UPDATE", label: ACTION_LABEL.RECORD_UPDATE },
    { value: "RECORD_DELETE", label: ACTION_LABEL.RECORD_DELETE },
    { value: "TERM_SAVE", label: ACTION_LABEL.TERM_SAVE },
    { value: "IMPORT_FACULTIES", label: actionLabel("IMPORT_FACULTIES") },
    { value: "IMPORT_CLASSES", label: actionLabel("IMPORT_CLASSES") },
    { value: "IMPORT_SUBJECTS", label: actionLabel("IMPORT_SUBJECTS") },
    { value: "IMPORT_LECTURERS", label: actionLabel("IMPORT_LECTURERS") },
    { value: "IMPORT_STUDENTS", label: actionLabel("IMPORT_STUDENTS") },
    { value: "IMPORT_TIMETABLE", label: actionLabel("IMPORT_TIMETABLE") },
    { value: "IMPORT_EXAMS", label: actionLabel("IMPORT_EXAMS") },
    { value: "IMPORT_DUTIES", label: actionLabel("IMPORT_DUTIES") },
    { value: "IMPORT_TERMS", label: actionLabel("IMPORT_TERMS") },
];