import { apiFetch } from "@/lib/api/client";
import { setAccessToken, setPasswordChangeRequired } from "@/lib/auth/session";

export type MyProfileView = {
    username: string;
    name: string;
    roles: string[];
    studentCode: string | null;
    classCode: string | null;
    profile: {
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
};

const ME = "/api/v1/users/me/profile";

export function getMyProfile(): Promise<MyProfileView> {
    return apiFetch<MyProfileView>(ME);
}

/** Chỉ sửa số điện thoại và địa chỉ. Chuỗi rỗng nghĩa là xóa. */
export function updateMyContact(phone: string, address: string): Promise<MyProfileView> {
    return apiFetch<MyProfileView>(ME, {
        method: "PUT",
        body: JSON.stringify({ phone, address }),
    });
}

/** Đăng xuất mọi thiết bị. Máy chủ thu hồi mọi phiên, nên xóa token ở trình duyệt này luôn. */
export async function logoutAllDevices(): Promise<void> {
    try {
        await apiFetch<void>("/api/v1/auth/logout-all", { method: "POST", skipRefresh: true });
    } finally {
        setAccessToken(null);
        setPasswordChangeRequired(false);
    }
}
