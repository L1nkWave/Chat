import { apiFetch } from "@/lib/api/client";

export type ImportKind =
    | "faculties"
    | "classes"
    | "subjects"
    | "lecturers"
    | "students"
    | "timetable"
    | "exams"
    | "duties"
    | "terms";

export type RowError = { row: number; message: string };

export type CreatedAccount = { username: string; name: string; temporaryPassword: string };

export type ImportReport = {
    kind: string;
    dryRun: boolean;
    total: number;
    valid: number;
    written: number;
    /** Số dòng thêm mới */
    inserted: number;
    /** Số dòng đã có và được cập nhật */
    updated: number;
    errors: RowError[];
    /** Chỉ có khi nhập thật và có tài khoản sinh viên mới */
    createdAccounts: CreatedAccount[];
};

export type TermPayload = {
    code: string;
    name: string;
    /** yyyy-MM-dd */
    startDate: string;
    /** yyyy-MM-dd */
    endDate: string;
};

// dryRun = true: chỉ kiểm tra file và báo lỗi, không ghi gì
export function importAcademicFile(kind: ImportKind, file: File, dryRun: boolean): Promise<ImportReport> {
    const form = new FormData();
    form.append("file", file);
    return apiFetch<ImportReport>(`/api/v1/admin/academic/import/${kind}?dryRun=${dryRun ? "true" : "false"}`, {
        method: "POST",
        body: form,
    });
}

export function saveAcademicTerm(term: TermPayload): Promise<void> {
    return apiFetch<void>("/api/v1/admin/academic/terms", {
        method: "POST",
        body: JSON.stringify(term),
    });
}

export type FacultyRow = { id: number; code: string; name: string; classCount: number };

export type ClassRow = {
    id: number;
    code: string;
    name: string;
    facultyId: number;
    facultyCode: string;
    cohort: string | null;
    studentCount: number;
    /** Giảng viên chủ nhiệm, null nếu chưa gán */
    advisorId: number | null;
    advisorName: string | null;
};

export type LecturerOption = { id: number; username: string; name: string };

export type FacultyPayload = { code: string; name: string };

export type ClassPayload = {
    code: string;
    name: string;
    facultyId: number;
    cohort: string | null;
    advisorId: number | null;
};

const ACADEMIC = "/api/v1/admin/academic";

export function listFaculties(): Promise<FacultyRow[]> {
    return apiFetch<FacultyRow[]>(`${ACADEMIC}/faculties`);
}

/** id = null là thêm mới, có id là sửa */
export function saveFaculty(id: number | null, body: FacultyPayload): Promise<void> {
    return apiFetch<void>(id === null ? `${ACADEMIC}/faculties` : `${ACADEMIC}/faculties/${id}`, {
        method: id === null ? "POST" : "PUT",
        body: JSON.stringify(body),
    });
}

export function deleteFaculty(id: number): Promise<void> {
    return apiFetch<void>(`${ACADEMIC}/faculties/${id}`, { method: "DELETE" });
}

export function listClasses(): Promise<ClassRow[]> {
    return apiFetch<ClassRow[]>(`${ACADEMIC}/classes`);
}

export function saveClass(id: number | null, body: ClassPayload): Promise<void> {
    return apiFetch<void>(id === null ? `${ACADEMIC}/classes` : `${ACADEMIC}/classes/${id}`, {
        method: id === null ? "POST" : "PUT",
        body: JSON.stringify(body),
    });
}

/** Danh sách giảng viên để chọn GVCN */
export function listLecturers(): Promise<LecturerOption[]> {
    return apiFetch<LecturerOption[]>(`${ACADEMIC}/lecturers`);
}

export function deleteClass(id: number): Promise<void> {
    return apiFetch<void>(`${ACADEMIC}/classes/${id}`, { method: "DELETE" });
}

// ---------------------------------------------------------------- bản ghi trong các tab phụ

export type RecordKind = "subjects" | "timetable" | "exams" | "duties" | "terms";

/** key là mã (môn, học kỳ) hoặc id (TKB, lịch thi, lịch gác thi); mọi trường khác theo tên cột */
export type RecordRow = { key: string | number } & Record<string, string | number | null>;

export type RecordPage = { items: RecordRow[]; total: number; page: number; size: number };

export function listRecords(kind: RecordKind, q: string, page: number, size: number): Promise<RecordPage> {
    const params = new URLSearchParams({ q, page: String(page), size: String(size) });
    return apiFetch<RecordPage>(`${ACADEMIC}/records/${kind}?${params.toString()}`);
}

export function createSubject(code: string, name: string): Promise<void> {
    return apiFetch<void>(`${ACADEMIC}/records/subjects`, {
        method: "POST",
        body: JSON.stringify({ code, name }),
    });
}

/** body chỉ gồm các trường được sửa, giá trị là chuỗi (rỗng nghĩa là để trống) */
export function updateRecord(kind: RecordKind, key: string, body: Record<string, string>): Promise<void> {
    return apiFetch<void>(`${ACADEMIC}/records/${kind}/${encodeURIComponent(key)}`, {
        method: "PUT",
        body: JSON.stringify(body),
    });
}

export function deleteRecord(kind: RecordKind, key: string): Promise<void> {
    return apiFetch<void>(`${ACADEMIC}/records/${kind}/${encodeURIComponent(key)}`, { method: "DELETE" });
}
// ---------------------------------------------------------------- thêm một dòng bằng form

/** Các mục có form thêm tay. Môn học, khoa, lớp, học kỳ có form riêng. */
export type ManualKind = "lecturers" | "students" | "timetable" | "exams" | "duties";

/** Khóa của body là tên cột trong file import (ví dụ "MSSV", "Mã GV"). Ngày dạng dd/mm/yyyy, giờ dạng HH:mm. */
export function addManualEntry(kind: ManualKind, body: Record<string, string>): Promise<void> {
    return apiFetch<void>(`${ACADEMIC}/manual/${kind}`, {
        method: "POST",
        body: JSON.stringify(body),
    });
}