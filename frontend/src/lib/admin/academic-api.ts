import { apiFetch } from "@/lib/api/client";

export type ImportKind = "students" | "timetable" | "exams" | "duties" | "terms";

export type RowError = { row: number; message: string };

export type CreatedAccount = { username: string; name: string; temporaryPassword: string };

export type ImportReport = {
    kind: string;
    dryRun: boolean;
    total: number;
    valid: number;
    written: number;
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