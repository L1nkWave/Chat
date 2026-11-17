import { apiFetch } from "@/lib/api/client";

export type RoleOverview = {
    id: number;
    name: string;
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

// Chỉ ROOT. Trả về cả người được gán toàn trường và người có phạm vi.
export function listRoleMembers(roleName: string): Promise<RoleMember[]> {
    return apiFetch<RoleMember[]>(`/api/v1/admin/roles/${encodeURIComponent(roleName)}/members`);
}