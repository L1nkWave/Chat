import { apiFetch } from "@/lib/api/client";
import type { AuditRow } from "@/lib/admin/audit-api";

export type UserStatusFilter = "ALL" | "LOCKED" | "ACTIVE";

export type UserRow = {
    id: number;
    username: string;
    name: string;
    locked: boolean;
    roles: string[];
    createdAt: string;
    lastSeen: string;
};

export type UserPage = {
    items: UserRow[];
    total: number;
    page: number;
    size: number;
};

export type ScopedGrant = {
    grantId: number;
    role: string;
    scopeType: "SCHOOL" | "FACULTY" | "CLASS";
    scopeId: number | null;
    grantedAt: string;
    grantedBy: number | null;
};

export type UserDetail = {
    id: number;
    username: string;
    name: string;
    bio: string | null;
    locked: boolean;
    createdAt: string;
    lastSeen: string;
    roles: string[];
    scopedRoles: ScopedGrant[];
    schoolPermissions: string[];
    /** null nếu máy chủ không đọc được trạng thái 2FA */
    twoFactorEnabled: boolean | null;
    /** Hoạt động gần nhất có đối tượng là người dùng này */
    recentAudit: AuditRow[];
    /** Chỉ có với sinh viên có hồ sơ học vụ */
    studentCode: string | null;
    classCode: string | null;
    /** 4 số đầu, phần còn lại là dấu * ; null nếu chưa có CCCD */
    cccdMasked: string | null;
};

// Tìm theo tên hoặc tên đăng nhập; role là tên đầy đủ (ví dụ ROLE_STUDENT), "" là tất cả
export function searchUsers(params: {
    q: string;
    role: string;
    status: UserStatusFilter;
    page: number;
    size: number;
}): Promise<UserPage> {
    const query = new URLSearchParams({
        q: params.q,
        role: params.role,
        status: params.status,
        page: String(params.page),
        size: String(params.size),
    });
    return apiFetch<UserPage>(`/api/v1/admin/users?${query.toString()}`);
}

export function getUserDetail(userId: number): Promise<UserDetail> {
    return apiFetch<UserDetail>(`/api/v1/admin/users/${userId}`);
}

// Hiện đầy đủ CCCD. Mỗi lần gọi được ghi vào nhật ký.
export function revealUserCccd(userId: number): Promise<{ cccd: string }> {
    return apiFetch<{ cccd: string }>(`/api/v1/admin/users/${userId}/cccd/reveal`, { method: "POST" });
}

// Sửa họ tên và tiểu sử. Cần quyền xem người dùng và cấp cao hơn người được sửa.
export function updateUserProfile(userId: number, name: string, bio: string): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/users/${userId}`, {
        method: "PATCH",
        body: JSON.stringify({ name, bio }),
    });
}

// Đặt mật khẩu tạm. Mật khẩu chỉ trả về một lần.
export function resetUserPassword(userId: number): Promise<{ temporaryPassword: string }> {
    return apiFetch<{ temporaryPassword: string }>(`/api/v1/admin/users/${userId}/reset-password`, {
        method: "POST",
    });
}

// Chỉ ROOT. Đặt vai trò toàn trường: ROLE_STUDENT, ROLE_LECTURER hoặc ROLE_ADMIN.
export function setUserGlobalRole(userId: number, role: string): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/users/${userId}/global-role`, {
        method: "PUT",
        body: JSON.stringify({ role }),
    });
}