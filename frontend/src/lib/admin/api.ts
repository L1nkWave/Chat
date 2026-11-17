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
    decision?: ResolveDecision | null;
    resolutionNote?: string | null;
    resolvedAt?: string | null;
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
export type ReportStatusFilter = "OPEN" | "RESOLVED" | "ALL";

export type ReportFilter = {
    status: ReportStatusFilter;
    category: ReportCategory | "";
    authorId: number | null;
    reporterId: number | null;
    /** yyyy-mm-dd, chuỗi rỗng nghĩa là không lọc */
    from: string;
    to: string;
    q: string;
};

export type ReportPage = {
    items: ModerationReport[];
    total: number;
    page: number;
    size: number;
};

export function searchReports(filter: ReportFilter, page: number, size: number): Promise<ReportPage> {
    const params = new URLSearchParams({ status: filter.status, page: String(page), size: String(size) });
    if (filter.category) params.set("category", filter.category);
    if (filter.authorId != null) params.set("authorId", String(filter.authorId));
    if (filter.reporterId != null) params.set("reporterId", String(filter.reporterId));
    if (filter.from) params.set("from", filter.from);
    if (filter.to) params.set("to", filter.to);
    if (filter.q.trim()) params.set("q", filter.q.trim());
    return apiFetch<ReportPage>(`/api/v1/moderation/reports?${params.toString()}`);
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