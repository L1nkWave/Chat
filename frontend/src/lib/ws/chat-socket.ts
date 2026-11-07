import { ApiError, apiFetch } from "@/lib/api/client";
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

    send(conversationId: string, text: string, attachmentId?: string): boolean {
        if (!this.isOpen() || !this.ws) return false;
        this.ws.send(
            encodeFrame("/chat", {
                type: "send",
                conversationId,
                text,
                attachmentId: attachmentId ?? null,
            }),
        );
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
        const frame = decodeFrame(raw);
        if (!frame) return;

        if (frame.path === "/session") {
            this.handlers.onSession(frame.payload as SessionPayload);
        } else if (frame.path === "/chat") {
            this.handlers.onChatEvent(frame.payload as ChatEvent);
        } else if (frame.path === "/error") {
            this.handlers.onError(frame.payload as ErrorPayload);
        }
    }
}