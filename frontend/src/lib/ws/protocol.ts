import type { Message } from "@/lib/chat/types";

export const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:9090/ws";

export type Frame = { path: string; payload: unknown };

export type SessionPayload = { userId: string; name: string };

// Sự kiện từ server trên route /chat.
export type ChatEvent =
    | { type: "message"; message: Message }
    | { type: "invitation" }
    | { type: "membership"; conversationId: string; kicked: boolean };

export type ErrorPayload = { code: string; message: string };

// Khung tin: dòng "path=/route", một dòng trống, rồi JSON.
export function encodeFrame(path: string, payload: unknown): string {
    return `path=${path}\n\n${JSON.stringify(payload)}`;
}

export function decodeFrame(raw: string): Frame | null {
    const text = raw.replace(/\r\n/g, "\n");
    const idx = text.indexOf("\n\n");
    if (idx < 0) return null;
    const head = text.slice(0, idx).trim();
    if (!head.startsWith("path=")) return null;
    try {
        return { path: head.slice(5).trim(), payload: JSON.parse(text.slice(idx + 2)) };
    } catch {
        return null;
    }
}