import { apiFetch } from "@/lib/api/client";

export type RoleOverview = {
    id: number;
    /** Mã kỹ thuật, ví dụ ROLE_GIANG_VIEN */
    name: string;
    /** Tên tiếng Việt hiển thị; null nếu chưa đặt */
    displayName: string | null;
    permissions: string[];
    globalMembers: number;
    scopedMembers: number;
};

export type RoleMember = {
    userId: number;
    username: string;
    name: string;
    source: "GLOBAL" | "SCOPED";
    scopeType: "SCHOOL" | "FACULTY" | "CLASS" | null;
    scopeId: number | null;
    grantId: number | null;
};

// Chỉ ROOT
export function listRoleOverview(): Promise<RoleOverview[]> {
    return apiFetch<RoleOverview[]>("/api/v1/admin/roles/overview");
}

export type RoleMemberPage = {
    total: number;
    items: RoleMember[];
};

// Chỉ ROOT. Trả về cả người được gán toàn trường và người có phạm vi, theo trang.
export function listRoleMembers(roleName: string, page: number, size: number): Promise<RoleMemberPage> {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    return apiFetch<RoleMemberPage>(
        `/api/v1/admin/roles/${encodeURIComponent(roleName)}/members?${query.toString()}`,
    );
}

// Chỉ ROOT. Thay toàn bộ bộ quyền của vai trò. Không sửa được ROLE_ROOT.
export function updateRolePermissions(roleName: string, permissions: string[]): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/roles/${encodeURIComponent(roleName)}/permissions`, {
        method: "PUT",
        body: JSON.stringify({ permissions }),
    });
}

// Chỉ ROOT. Đổi tên tiếng Việt hiển thị; mã kỹ thuật không đổi.
export function updateRoleDisplayName(roleName: string, displayName: string): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/roles/${encodeURIComponent(roleName)}/display-name`, {
        method: "PUT",
        body: JSON.stringify({ displayName }),
    });
}