import { apiFetch } from "@/lib/api/client";
import { setAccessToken } from "@/lib/auth/session";

type LoginResponse = {
    refreshExpiration: string;
    accessToken: string;
};

export type RegisterInput = {
    username: string;
    password: string;
    name: string;
};

export async function login(username: string, password: string): Promise<void> {
    const body = await apiFetch<LoginResponse>("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
        skipAuth: true,
        skipRefresh: true,
    });
    setAccessToken(body.accessToken);
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