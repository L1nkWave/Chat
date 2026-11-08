import { ApiError } from "@/lib/api/client";
import { getAccessToken } from "@/lib/auth/session";

// Tệp đã được lưu thành tin nhắn trên server (khớp với CreatedFileMessage của chat-service).
export type CreatedFile = {
    id: string;
    createdAt: string;
    filename: string;
    contentType: string;
    size: number;
};

export const MAX_FILE_BYTES = 10 * 1024 * 1024;

export const ALLOWED_TYPES = [
    "image/png",
    "image/jpeg",
    "image/gif",
    "image/webp",
    "application/pdf",
    "text/plain",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
];

export const ACCEPT = ALLOWED_TYPES.join(",");

// Kiểm tra phía client chỉ để phản hồi nhanh. Server mới là nơi kiểm tra thật
// (magic bytes, kích thước, tên tệp).
export function validateFile(file: File): string | null {
    if (!ALLOWED_TYPES.includes(file.type)) return "Định dạng tệp không được hỗ trợ.";
    if (file.size === 0) return "Tệp rỗng.";
    if (file.size > MAX_FILE_BYTES) return "Tệp vượt quá 10 MB.";
    return null;
}

export function isImage(contentType: string): boolean {
    return contentType.startsWith("image/");
}

export function formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

async function errorFrom(res: Response): Promise<ApiError> {
    let message = "Đã có lỗi xảy ra. Vui lòng thử lại.";
    try {
        const data = (await res.json()) as { message?: unknown };
        if (typeof data.message === "string") message = data.message;
    } catch {
        // Giữ thông báo mặc định nếu phản hồi không phải JSON.
    }
    return new ApiError(res.status, message);
}

async function authedFetch(path: string, init: RequestInit = {}): Promise<Response> {
    const headers = new Headers(init.headers);
    const token = getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const res = await fetch(path, { ...init, headers, credentials: "include" });
    if (!res.ok) throw await errorFrom(res);
    return res;
}

// Tải tệp lên một cuộc trò chuyện (multipart, trường "file"). Server lưu thành tin nhắn
// và trả về thông tin tệp. Việc phát tới người nhận làm qua WebSocket /chat/{id}/send_file.
// Không tự đặt Content-Type: trình duyệt tự thêm boundary cho multipart.
export async function uploadFile(chatId: string, file: File): Promise<CreatedFile> {
    const form = new FormData();
    form.append("file", file, file.name);
    const res = await authedFetch(`/api/v1/chats/${encodeURIComponent(chatId)}/messages/file`, {
        method: "POST",
        body: form,
    });
    return (await res.json()) as CreatedFile;
}

// Tải tệp về dạng blob để gắn token. <img src> không gửi được header Authorization.
export async function fetchAttachmentBlob(id: string): Promise<Blob> {
    const res = await authedFetch(`/api/v1/chats/messages/${encodeURIComponent(id)}/file`);
    return res.blob();
}