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

/** Hồ sơ cá nhân hiển thị. CCCD đã che; dân tộc và tôn giáo chỉ cho biết có hay không. */
export type ProfileView = {
    gender: string | null;
    birthDate: string | null;
    status: string | null;
    phone: string | null;
    birthplace: string | null;
    nationality: string | null;
    address: string | null;
    cccdMasked: string | null;
    hasEthnicity: boolean;
    hasReligion: boolean;
};

export type SensitiveView = {
    cccd: string | null;
    ethnicity: string | null;
    religion: string | null;
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
    cccdMasked: string | null;
    profile: ProfileView;
};

/**
 * Dữ liệu hồ sơ gửi lên máy chủ.
 * Trường nhạy cảm (cccd, ethnicity, religion): null nghĩa là không đổi, chuỗi rỗng nghĩa là xóa.
 * Trường thường: gửi đầy đủ, null hoặc rỗng nghĩa là để trống.
 */
export type ProfilePayload = {
    gender: string | null;
    birthDate: string | null;
    status: string | null;
    phone: string | null;
    birthplace: string | null;
    nationality: string | null;
    address: string | null;
    cccd: string | null;
    ethnicity: string | null;
    religion: string | null;
};

export type CreateUserPayload = ProfilePayload & {
    username: string;
    name: string;
    role: string;
    classCode: string | null;
    status: string;
};

// Tìm theo tên hoặc tên đăng nhập; role là tên đầy đủ (ví dụ ROLE_STUDENT), "" là tất cả
export function searchUsers(params: {
    q: string;
    role: string;
    status: UserStatusFilter;
    /** Mã khoa, mã lớp, mã môn: lọc theo học vụ. Rỗng nghĩa là không lọc. */
    faculty: string;
    classCode: string;
    subject: string;
    page: number;
    size: number;
}): Promise<UserPage> {
    const query = new URLSearchParams({
        q: params.q,
        role: params.role,
        status: params.status,
        faculty: params.faculty,
        classCode: params.classCode,
        subject: params.subject,
        page: String(params.page),
        size: String(params.size),
    });
    return apiFetch<UserPage>(`/api/v1/admin/users?${query.toString()}`);
}

export function getUserDetail(userId: number): Promise<UserDetail> {
    return apiFetch<UserDetail>(`/api/v1/admin/users/${userId}`);
}

// Tạo tài khoản đơn lẻ. Mật khẩu mặc định Dhsg@123, người dùng phải đổi khi đăng nhập lần đầu.
export function createUser(body: CreateUserPayload): Promise<{ id: number; username: string }> {
    return apiFetch<{ id: number; username: string }>("/api/v1/admin/users", {
        method: "POST",
        body: JSON.stringify(body),
    });
}

// Sửa họ tên, tiểu sử và hồ sơ. Cần quyền xem người dùng và cấp cao hơn người được sửa.
export function updateUserProfile(
    userId: number,
    body: ProfilePayload & { name: string; bio: string | null },
): Promise<void> {
    return apiFetch<void>(`/api/v1/admin/users/${userId}`, {
        method: "PATCH",
        body: JSON.stringify(body),
    });
}

// Hiện đầy đủ CCCD. Mỗi lần gọi được ghi vào nhật ký.
export function revealUserCccd(userId: number): Promise<{ cccd: string }> {
    return apiFetch<{ cccd: string }>(`/api/v1/admin/users/${userId}/cccd/reveal`, { method: "POST" });
}

// Hiện đầy đủ CCCD, dân tộc, tôn giáo. Mỗi lần gọi được ghi vào nhật ký (chỉ ghi tên trường).
export function revealUserSensitive(userId: number): Promise<SensitiveView> {
    return apiFetch<SensitiveView>(`/api/v1/admin/users/${userId}/sensitive/reveal`, { method: "POST" });
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