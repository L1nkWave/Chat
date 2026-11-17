import { ApiError, apiFetch } from "@/lib/api/client";

export type ReportCategory = "HARASSMENT" | "PROFANITY" | "SPAM" | "INAPPROPRIATE" | "OTHER";
export type ResolveDecision = "DISMISS" | "WARN" | "HIDE";
export type ScopeType = "SCHOOL" | "FACULTY" | "CLASS";

export type ModerationReport = {
    id: string;
    messageId: string;
    chatId: string;
    authorId: number;
    reporterId: number;
    category: ReportCategory;
    reason: string | null;
    /** Nội dung tin tại thời điểm báo cáo; null nếu tin là tệp hoặc đã bị xóa trước đó */
    messageText: string | null;
    messageCreatedAt: string | null;
    reportedAt: string;
    status: "OPEN" | "RESOLVED";
};

export type AuditEntry = {
    id: number;
    actorId: number;
    action: string;
    targetType: string | null;
    targetId: string | null;
    detail: string | null;
    ip: string | null;
    createdAt: string;
};

export type AccessInfo = {
    userId: number;
    level: number;
    schoolPermissions: string[];
};

export type UserInfo = {
    id: number;
    username: string;
    name: string;
};

export function adminErrorMessage(err: unknown): string {
    return err instanceof ApiError ? err.message : "Đã có lỗi xảy ra. Vui lòng thử lại.";
}

// Báo cáo tin nhắn (chat-service)
export function listOpenReports(page: number, size: number): Promise<ModerationReport[]> {
    return apiFetch<ModerationReport[]>(`/api/v1/moderation/reports?page=${page}&size=${size}`);
}

export function resolveReport(reportId: string, decision: ResolveDecision, note?: string): Promise<void> {
    return apiFetch<void>(`/api/v1/moderation/reports/${encodeURIComponent(reportId)}/resolve`, {
        method: "POST",
        body: JSON.stringify({ decision, note }),
    });
}

// Thông tin người dùng để hiện tên thay cho mã số (user-service)
export function getUsersByIds(ids: number[]): Promise<UserInfo[]> {
    if (ids.length === 0) return Promise.resolve([]);
    const query = [...new Set(ids)].map((id) => `usersIds=${id}`).join("&");
    return apiFetch<UserInfo[]>(`/api/v1/users/batch?${query}`);
}

// Nhật ký hệ thống (user-service)
export function listAudit(page: number, size: number): Promise<AuditEntry[]> {
    return apiFetch<AuditEntry[]>(`/api/v1/admin/audit?page=${page}&size=${size}`);
}

export function getAccess(userId: number): Promise<AccessInfo> {
    return apiFetch<AccessInfo>(`/api/v1/users/${userId}/access`);
}

export function setUserLocked(userId: number, locked: boolean): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/users/${userId}/${locked ? "lock" : "unlock"}`, { method: "POST" });
}

// Chỉ ROOT. Server tự thêm tiền tố ROLE_ vào mã.
export function createRole(code: string, permissions: string[]): Promise<void> {
    return apiFetch<void>("/api/v1/admin/roles", {
        method: "POST",
        body: JSON.stringify({ code, permissions }),
    });
}

// Chỉ ROOT. Tên vai trò đầy đủ, ví dụ ROLE_ADMIN. SCHOOL không có mã phạm vi.
export function grantRole(
    userId: number,
    role: string,
    scopeType: ScopeType,
    scopeId: number | null,
): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/users/${userId}/roles`, {
        method: "POST",
        body: JSON.stringify({ role, scopeType, scopeId }),
    });
}

// Chỉ ROOT. Đánh dấu thời điểm thu hồi; không xóa dòng.
export function revokeGrant(grantId: number): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/roles/grants/${grantId}`, { method: "DELETE" });
}