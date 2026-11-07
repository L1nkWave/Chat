"use client";

import { useEffect, useRef } from "react";
import type { Message } from "@/lib/chat/types";
import { AttachmentView } from "./AttachmentView";

type MessageListProps = {
    messages: Message[];
    currentUserId: string;
    isGroup: boolean;
};

export function MessageList({ messages, currentUserId, isGroup }: MessageListProps) {
    const endRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        endRef.current?.scrollIntoView({ block: "end" });
    }, [messages.length]);

    if (messages.length === 0) {
        return (
            <div className="flex min-h-0 flex-1 items-center justify-center text-sm text-muted">
                Chưa có tin nhắn nào.
            </div>
        );
    }

    return (
        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-4">
            {messages.map((m) => {
                const mine = m.senderId === currentUserId;
                return (
                    <div key={m.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
                        <div
                            className={`max-w-[75%] rounded-2xl px-4 py-2 ${
                                mine
                                    ? "rounded-br-sm bg-primary text-primary-ink"
                                    : "rounded-bl-sm border border-line bg-surface"
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
                            <p
                                className={`mt-1 text-right text-[11px] ${
                                    mine ? "text-primary-ink/70" : "text-muted"
                                }`}
                            >
                                {m.time}
                            </p>
                        </div>
                    </div>
                );
            })}
            <div ref={endRef} />
        </div>
    );
}