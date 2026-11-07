import { getAccessToken } from "@/lib/auth/session";

// Đọc claim "user-id" từ access token (JWT). Chỉ giải mã phần payload để lấy ID,
// không dùng để xác thực; server vẫn kiểm tra chữ ký.
export function currentUserId(): string | null {
    const token = getAccessToken();
    const part = token?.split(".")[1];
    if (!part) return null;
    try {
        const base64 = part.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
        const claims = JSON.parse(atob(padded)) as Record<string, unknown>;
        const id = claims["user-id"];
        return id === undefined || id === null ? null : String(id);
    } catch {
        return null;
    }
}