import { ApiError, apiFetch } from "@/lib/api/client";
import { currentUserId } from "@/lib/auth/token";
import type { CreatedFile } from "@/lib/chat/files";
import type { Message } from "@/lib/chat/types";
import {
    decodeFrame,
    encodeFrame,
    WS_URL,
    type ChatEvent,
    type ErrorPayload,
    type SessionPayload,
} from "./protocol";

export type ConnectionStatus = "connecting" | "open" | "reconnecting" | "closed";

type Handlers = {
    onSession: (session: SessionPayload) => void;
    onChatEvent: (event: ChatEvent) => void;
    onError: (error: ErrorPayload) => void;
    onStatus: (status: ConnectionStatus) => void;
    onAuthFailed: () => void;
};

const MAX_BACKOFF_MS = 15_000;

// Mỗi lần start/stop tăng "generation". Callback cũ (ví dụ do React Strict Mode
// gọi effect hai lần) sẽ tự bỏ qua, tránh mở hai socket cùng lúc.
export class ChatSocket {
    private readonly handlers: Handlers;
    private ws: WebSocket | null = null;
    private attempt = 0;
    private timer: ReturnType<typeof setTimeout> | null = null;
    private generation = 0;

    constructor(handlers: Handlers) {
        this.handlers = handlers;
    }

    start() {
        this.generation += 1;
        this.attempt = 0;
        void this.connect(this.generation);
    }

    stop() {
        this.generation += 1;
        if (this.timer) clearTimeout(this.timer);
        this.ws?.close(1000, "client closed");
        this.ws = null;
        this.handlers.onStatus("closed");
    }

    isOpen(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }

    // Gửi tin chữ: server phát lại qua sự kiện MESSAGE.
    send(conversationId: string, text: string): boolean {
        if (!this.isOpen() || !this.ws) return false;
        this.ws.send(
            encodeFrame(`/chat/${conversationId}/send`, {
                tmpMessageId: crypto.randomUUID(),
                text,
            }),
        );
        return true;
    }

    // Báo server về tệp đã tải lên (REST). Server kiểm tra quyền rồi phát lại qua sự kiện FILE.
    sendFile(conversationId: string, file: CreatedFile): boolean {
        if (!this.isOpen() || !this.ws) return false;
        this.ws.send(encodeFrame(`/chat/${conversationId}/send_file`, file));
        return true;
    }

    private async connect(gen: number) {
        this.handlers.onStatus(this.attempt === 0 ? "connecting" : "reconnecting");

        let ticket: string;
        try {
            const res = await apiFetch<{ ticket: string }>("/api/v1/ws-tickets", { method: "POST" });
            ticket = res.ticket;
        } catch (err) {
            if (gen !== this.generation) return;
            if (err instanceof ApiError && err.status === 401) {
                this.handlers.onAuthFailed();
                return;
            }
            this.scheduleReconnect(gen);
            return;
        }
        if (gen !== this.generation) return;

        const ws = new WebSocket(`${WS_URL}?ticket=${encodeURIComponent(ticket)}`);
        this.ws = ws;

        ws.onopen = () => {
            if (gen !== this.generation) return;
            this.attempt = 0;
            const userId = currentUserId();
            if (userId) this.handlers.onSession({ userId, name: "" });
            this.handlers.onStatus("open");
        };

        ws.onmessage = (event) => {
            if (gen !== this.generation) return;
            this.dispatch(String(event.data));
        };

        ws.onclose = (event) => {
            if (gen !== this.generation) return;
            this.ws = null;
            if (event.code === 4401) {
                this.handlers.onAuthFailed();
                return;
            }
            this.scheduleReconnect(gen);
        };
    }

    private scheduleReconnect(gen: number) {
        this.handlers.onStatus("reconnecting");
        const delay = Math.min(1000 * 2 ** this.attempt, MAX_BACKOFF_MS);
        this.attempt += 1;
        this.timer = setTimeout(() => {
            if (gen === this.generation) void this.connect(gen);
        }, delay);
    }

    private dispatch(raw: string) {
        // Khung có "path=" là của client; server gửi JSON thuần có trường "action"
        const frame = decodeFrame(raw);
        if (frame) {
            if (frame.path === "/error") this.handlers.onError(frame.payload as ErrorPayload);
            return;
        }

        let data: Record<string, unknown>;
        try {
            data = JSON.parse(raw) as Record<string, unknown>;
        } catch {
            return;
        }
        this.dispatchAction(data);
    }

    private dispatchAction(data: Record<string, unknown>) {
        switch (data.action) {
            case "MESSAGE": {
                const message: Message = {
                    id: String(data.id),
                    conversationId: String(data.chatId),
                    senderId: String(data.senderId),
                    senderName: typeof data.senderName === "string" ? data.senderName : "",
                    text: typeof data.text === "string" ? data.text : "",
                    time: String(data.timestamp),
                    attachment: null,
                };
                this.handlers.onChatEvent({ type: "message", message });
                return;
            }
            case "FILE": {
                // Tin nhắn tệp: id của tin cũng là id dùng để tải tệp (GET /messages/{id}/file)
                const message: Message = {
                    id: String(data.id),
                    conversationId: String(data.chatId),
                    senderId: String(data.senderId),
                    senderName: "",
                    text: "",
                    time: String(data.timestamp),
                    attachment: {
                        id: String(data.id),
                        filename: String(data.filename),
                        contentType: String(data.contentType),
                        size: Number(data.size),
                    },
                };
                this.handlers.onChatEvent({ type: "message", message });
                return;
            }
            case "INVITATION":
                this.handlers.onChatEvent({ type: "invitation" });
                return;
            case "KICK":
                // Server gửi KICK cho cả nhóm và riêng người bị xóa (user:{memberId})
                this.handlers.onChatEvent({
                    type: "membership",
                    conversationId: String(data.chatId),
                    kicked: String(data.memberId) === currentUserId(),
                });
                return;
            case "JOIN":
            case "LEAVE":
            case "ADD":
            case "SET_ROLE":
                this.handlers.onChatEvent({
                    type: "membership",
                    conversationId: String(data.chatId),
                    kicked: false,
                });
                return;
            case "ERROR":
                this.handlers.onError({
                    code: "ERROR",
                    message: "Có lỗi từ máy chủ chat. Vui lòng thử lại.",
                });
                return;
            default:
                return;
        }
    }
}