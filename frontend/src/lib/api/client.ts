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

// Thông báo của máy chủ là tiếng Anh, nên hiển thị theo mã lỗi bằng tiếng Việt.
const STATUS_MESSAGES: Record<number, string> = {
    400: "Thông tin gửi lên chưa hợp lệ. Vui lòng kiểm tra lại.",
    401: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
    403: "Bạn không có quyền thực hiện thao tác này.",
    404: "Không tìm thấy dữ liệu yêu cầu.",
    409: "Dữ liệu đã tồn tại hoặc xung đột với dữ liệu khác.",
    413: "Tệp quá lớn để tải lên.",
    415: "Định dạng tệp không được hỗ trợ.",
    429: "Bạn thao tác quá nhanh. Vui lòng đợi một lúc rồi thử lại.",
};

async function readErrorMessage(res: Response): Promise<string> {
    if (res.status >= 500) return "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
    return STATUS_MESSAGES[res.status] ?? FALLBACK_ERROR;
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