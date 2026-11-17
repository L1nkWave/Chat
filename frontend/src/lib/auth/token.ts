import { refreshAccessToken } from "@/lib/api/client";
import { getAccessToken } from "@/lib/auth/session";

// Giải mã phần payload của access token (JWT) để hiển thị giao diện.
// Không dùng để xác thực; server vẫn kiểm tra chữ ký ở mọi request.
function readClaims(): Record<string, unknown> | null {
    const part = getAccessToken()?.split(".")[1];
    if (!part) return null;
    try {
        const base64 = part.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
        return JSON.parse(atob(padded)) as Record<string, unknown>;
    } catch {
        return null;
    }
}

// Đọc claim "user-id" từ access token
export function currentUserId(): string | null {
    const id = readClaims()?.["user-id"];
    return id === undefined || id === null ? null : String(id);
}

export function currentAuthorities(): string[] {
    const list = readClaims()?.authorities;
    return Array.isArray(list) ? list.filter((item): item is string => typeof item === "string") : [];
}

/** ADMIN hoặc ROOT: được vào trang quản trị và bật 2FA. */
export function isAdminUser(): boolean {
    const roles = currentAuthorities();
    return roles.includes("ROLE_ADMIN") || roles.includes("ROLE_ROOT");
}

/** Đã xác thực 2FA (hoặc không thuộc nhóm bắt buộc). Trang quản trị cần cờ này. */
export function hasMfa(): boolean {
    return currentAuthorities().includes("MFA_VERIFIED");
}

export function isRootUser(): boolean {
    return currentAuthorities().includes("ROLE_ROOT");
}

/**
 * Access token chỉ nằm trong bộ nhớ. Khi trang vừa tải lại, token đã mất,
 * nên làm mới bằng refresh cookie. Trả về false nếu chưa đăng nhập.
 */
export async function ensureAccessToken(): Promise<boolean> {
    if (getAccessToken()) return true;
    return refreshAccessToken();
}