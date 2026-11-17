import { apiFetch } from "@/lib/api/client";

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