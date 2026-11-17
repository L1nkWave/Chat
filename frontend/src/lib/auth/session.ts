/**
 * The access token lives only in memory. It is never written to localStorage or
 * sessionStorage, so an XSS payload cannot read it after the tab is closed.
 * The refresh token stays in an httpOnly cookie that JavaScript cannot read.
 */
let accessToken: string | null = null;

// Cờ do máy chủ trả về: tài khoản phải đổi mật khẩu trước khi dùng tiếp.
// Lưu trong bộ nhớ, được cập nhật lại mỗi lần đăng nhập hoặc làm mới phiên.
let passwordChangeRequired = false;

export function getAccessToken(): string | null {
    return accessToken;
}

export function setAccessToken(token: string | null): void {
    accessToken = token;
}

export function isPasswordChangeRequired(): boolean {
    return passwordChangeRequired;
}

export function setPasswordChangeRequired(value: boolean): void {
    passwordChangeRequired = value;
}