import { getAccessToken, setAccessToken } from "@/lib/auth/session";

export class ApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
        super(message);
        this.name = "ApiError";
        this.status = status;
    }
}

type ApiRequestInit = RequestInit & {
    /** Do not attach the access token (login, register). */
    skipAuth?: boolean;
    /** Do not try to refresh after a 401 (used by the refresh call itself). */
    skipRefresh?: boolean;
};

const FALLBACK_ERROR = "Có lỗi xảy ra, vui lòng thử lại.";

// Shared promise so that parallel 401s trigger a single refresh call
let pendingRefresh: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
    if (!pendingRefresh) {
        pendingRefresh = (async () => {
            try {
                const res = await fetch("/api/v1/auth/refresh-tokens", {
                    method: "POST",
                    credentials: "include",
                });
                if (!res.ok) {
                    setAccessToken(null);
                    return false;
                }
                const body = (await res.json()) as { accessToken: string };
                setAccessToken(body.accessToken);
                return true;
            } catch {
                setAccessToken(null);
                return false;
            } finally {
                pendingRefresh = null;
            }
        })();
    }
    return pendingRefresh;
}

async function readErrorMessage(res: Response): Promise<string> {
    try {
        const data = (await res.json()) as { message?: string };
        if (data.message) {
            return data.message;
        }
    } catch {
        // The body is not JSON; fall back to the generic message
    }
    return FALLBACK_ERROR;
}

export async function apiFetch<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
    const { skipAuth = false, skipRefresh = false, headers, ...rest } = init;

    const send = (): Promise<Response> => {
        const requestHeaders = new Headers(headers);
        const hasBody = rest.body !== undefined && rest.body !== null;
        const isFormData = typeof FormData !== "undefined" && rest.body instanceof FormData;
        if (hasBody && !isFormData && !requestHeaders.has("Content-Type")) {
            requestHeaders.set("Content-Type", "application/json");
        }
        const token = getAccessToken();
        if (!skipAuth && token) {
            requestHeaders.set("Authorization", `Bearer ${token}`);
        }
        return fetch(path, { ...rest, headers: requestHeaders, credentials: "include" });
    };

    let res = await send();
    if (res.status === 401 && !skipRefresh && (await refreshAccessToken())) {
        res = await send();
    }

    if (!res.ok) {
        throw new ApiError(res.status, await readErrorMessage(res));
    }

    // 201 Created from register and 204 No Content have no body
    const text = await res.text();
    return (text ? JSON.parse(text) : undefined) as T;
}