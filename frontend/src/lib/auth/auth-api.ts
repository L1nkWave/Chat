import { ApiError, apiFetch } from "@/lib/api/client";
import { setAccessToken } from "@/lib/auth/session";

type TokensResponse = {
    refreshExpiration: string;
    accessToken: string;
};

type LoginResponse = Partial<TokensResponse> & {
    twoFactorRequired?: boolean;
    challenge?: string;
};

/** Đăng nhập xong, hoặc cần nhập mã 2FA (chỉ ADMIN/ROOT đã bật 2FA). */
export type LoginResult = { kind: "signedIn" } | { kind: "twoFactor"; challenge: string };

export type TwoFactorSetup = {
    secret: string;
    otpauthUri: string;
};

export type RegisterInput = {
    username: string;
    password: string;
    name: string;
};

export async function login(username: string, password: string): Promise<LoginResult> {
    const body = await apiFetch<LoginResponse>("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
        skipAuth: true,
        skipRefresh: true,
    });

    if (body.twoFactorRequired && body.challenge) {
        return { kind: "twoFactor", challenge: body.challenge };
    }
    if (!body.accessToken) {
        throw new Error("Phản hồi đăng nhập không hợp lệ");
    }
    setAccessToken(body.accessToken);
    return { kind: "signedIn" };
}

/** Bước 2 khi đăng nhập: challenge (5 phút) cộng mã TOTP hoặc mã khôi phục. */
export async function verifyTwoFactor(challenge: string, code: string): Promise<void> {
    const body = await apiFetch<TokensResponse>("/api/v1/auth/2fa/verify", {
        method: "POST",
        body: JSON.stringify({ challenge, code }),
        skipAuth: true,
        skipRefresh: true,
    });
    setAccessToken(body.accessToken);
}

/** Tạo bí mật mới. Cần access token của ADMIN/ROOT. */
export function setupTwoFactor(): Promise<TwoFactorSetup> {
    return apiFetch<TwoFactorSetup>("/api/v1/auth/2fa/setup", { method: "POST" });
}

/** Xác nhận mã đầu tiên và nhận 8 mã khôi phục (chỉ hiện một lần). */
const CODE_REJECTED = "Mã không đúng hoặc đã được dùng. Hãy đợi mã mới (mỗi mã đổi sau 30 giây) rồi thử lại.";

// Server trả 401 khi mã sai hoặc mã đã dùng trong cùng 30 giây. Thông báo mặc định của apiFetch
// lại nói "phiên hết hạn", nên thay bằng thông báo đúng.
async function withCodeErrors<T>(task: () => Promise<T>): Promise<T> {
    try {
        return await task();
    } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
            throw new ApiError(401, CODE_REJECTED);
        }
        throw err;
    }
}

export async function confirmTwoFactor(code: string): Promise<string[]> {
    return withCodeErrors(async () => {
        const body = await apiFetch<{ recoveryCodes: string[] }>("/api/v1/auth/2fa/confirm", {
            method: "POST",
            body: JSON.stringify({ code }),
        });
        return body.recoveryCodes;
    });
}

export function disableTwoFactor(code: string): Promise<void> {
    return withCodeErrors(() =>
        apiFetch<void>("/api/v1/auth/2fa/disable", {
            method: "POST",
            body: JSON.stringify({ code }),
        }),
    );
}

export async function register(input: RegisterInput): Promise<void> {
    await apiFetch<void>("/api/v1/users/register", {
        method: "POST",
        body: JSON.stringify(input),
        skipAuth: true,
        skipRefresh: true,
    });
}

export async function logout(): Promise<void> {
    try {
        await apiFetch<void>("/api/v1/auth/logout", { method: "POST", skipRefresh: true });
    } finally {
        setAccessToken(null);
    }
}