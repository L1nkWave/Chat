"use client";

import { useEffect, useLayoutEffect, useRef } from "react";
import { formatChatTime } from "@/lib/chat/time";
import type { Message } from "@/lib/chat/types";
import { AttachmentView } from "./AttachmentView";

type MessageListProps = {
    messages: Message[];
    currentUserId: string;
    isGroup: boolean;
};

// Khoảng cách (px) tính là "đang ở cuối"
const NEAR_BOTTOM = 120;

export function MessageList({ messages, currentUserId, isGroup }: MessageListProps) {
    const scrollRef = useRef<HTMLDivElement>(null);
    const contentRef = useRef<HTMLDivElement>(null);
    // Đang ở gần cuối thì tự cuộn khi có tin mới hoặc khi ảnh tải xong
    const pinned = useRef(true);

    function scrollToBottom() {
        const el = scrollRef.current;
        if (el) el.scrollTop = el.scrollHeight;
    }

    function handleScroll() {
        const el = scrollRef.current;
        if (!el) return;
        pinned.current = el.scrollHeight - el.scrollTop - el.clientHeight < NEAR_BOTTOM;
    }

    // Khi có tin mới: tin của mình thì luôn xuống cuối, tin người khác chỉ xuống khi đang ở cuối
    useLayoutEffect(() => {
        const last = messages[messages.length - 1];
        if (last && last.senderId === currentUserId) pinned.current = true;
        if (pinned.current) scrollToBottom();
    }, [messages, currentUserId]);

    // Ảnh tải xong hoặc nội dung đổi kích thước sau khi render: giữ đáy nếu đang ở cuối
    useEffect(() => {
        const content = contentRef.current;
        if (!content || typeof ResizeObserver === "undefined") return;
        const observer = new ResizeObserver(() => {
            if (pinned.current) scrollToBottom();
        });
        observer.observe(content);
        return () => observer.disconnect();
    }, []);

    return (
        <div
            ref={scrollRef}
            onScroll={handleScroll}
            className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-8"
        >
            <div ref={contentRef} className="space-y-4">
                {messages.length === 0 ? (
                    <p className="py-10 text-center text-sm text-muted">Chưa có tin nhắn nào.</p>
                ) : null}
                {messages.map((m) => {
                    const mine = m.senderId === currentUserId;
                    return (
                        <div key={m.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
                            <div
                                className={`max-w-[min(34rem,80%)] rounded-md px-4 py-2.5 ${
                                    mine ? "bg-tint text-ink" : "border border-line bg-surface text-ink"
                                }`}
                            >
                                {isGroup && !mine ? (
                                    <p className="mb-1 text-xs font-semibold text-primary">{m.senderName}</p>
                                ) : null}
                                {m.attachment ? <AttachmentView attachment={m.attachment} mine={mine} /> : null}
                                {/* Nội dung là văn bản thuần, không dùng dangerouslySetInnerHTML. */}
                                {m.text ? (
                                    <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed">
                                        {m.text}
                                    </p>
                                ) : null}
                                <p className="mt-1 text-right text-[11px] text-muted">{formatChatTime(m.time)}</p>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}