"use client";

import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { formatChatTime } from "@/lib/chat/time";
import type { Message } from "@/lib/chat/types";
import { AttachmentView } from "./AttachmentView";

const REPORT_CATEGORIES = [
    { value: "HARASSMENT", label: "Quấy rối, xúc phạm" },
    { value: "PROFANITY", label: "Từ ngữ thô tục" },
    { value: "SPAM", label: "Spam, quảng cáo" },
    { value: "INAPPROPRIATE", label: "Nội dung không phù hợp" },
    { value: "OTHER", label: "Khác" },
];

type MessageListProps = {
    messages: Message[];
    currentUserId: string;
    isGroup: boolean;
    hasOlder: boolean;
    loadingOlder: boolean;
    onLoadOlder: () => void;
    onEdit: (messageId: string, text: string) => void;
    onRemove: (messageId: string) => void;
    onReport: (messageId: string, category: string, reason?: string) => void;
};

// Khoảng cách (px) tính là "đang ở cuối"
const NEAR_BOTTOM = 120;

export function MessageList({
                                messages,
                                currentUserId,
                                isGroup,
                                hasOlder,
                                loadingOlder,
                                onLoadOlder,
                                onEdit,
                                onRemove,
                                onReport,
                            }: MessageListProps) {
    const [editingId, setEditingId] = useState<string | null>(null);
    const [draft, setDraft] = useState("");
    const [reportingId, setReportingId] = useState<string | null>(null);
    const [reportCategory, setReportCategory] = useState("");
    const [reportReason, setReportReason] = useState("");
    const scrollRef = useRef<HTMLDivElement>(null);
    const contentRef = useRef<HTMLDivElement>(null);
    // Đang ở gần cuối thì tự cuộn khi có tin mới hoặc khi ảnh tải xong
    const pinned = useRef(true);
    // Trạng thái lần render trước, để nhận biết tin cũ vừa được thêm lên đầu
    const previous = useRef<{ first?: string; last?: string; height: number }>({ height: 0 });

    function scrollToBottom() {
        const el = scrollRef.current;
        if (el) el.scrollTop = el.scrollHeight;
    }

    function handleScroll() {
        const el = scrollRef.current;
        if (!el) return;
        pinned.current = el.scrollHeight - el.scrollTop - el.clientHeight < NEAR_BOTTOM;
    }

    const firstId = messages[0]?.id;
    const lastId = messages[messages.length - 1]?.id;

    useLayoutEffect(() => {
        const el = scrollRef.current;
        if (!el) return;
        const prev = previous.current;
        const prepended = prev.first !== undefined && firstId !== prev.first && lastId === prev.last;

        if (prepended) {
            // Tin cũ được thêm lên trên: giữ nguyên vị trí người dùng đang xem
            el.scrollTop += el.scrollHeight - prev.height;
        } else {
            // Tin mới: tin của mình thì luôn xuống cuối, tin người khác chỉ khi đang ở cuối
            const last = messages[messages.length - 1];
            if (last && last.senderId === currentUserId) pinned.current = true;
            if (pinned.current) scrollToBottom();
        }

        previous.current = { first: firstId, last: lastId, height: el.scrollHeight };
    }, [messages, currentUserId, firstId, lastId]);

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
                {messages.length > 0 && hasOlder ? (
                    <div className="flex justify-center">
                        <button
                            type="button"
                            onClick={onLoadOlder}
                            disabled={loadingOlder}
                            className="h-8 rounded-md border border-line bg-surface px-3 text-xs text-muted transition-colors hover:bg-tint hover:text-ink disabled:opacity-50"
                        >
                            {loadingOlder ? "Đang tải..." : "Tải tin cũ hơn"}
                        </button>
                    </div>
                ) : null}

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
                                {m.removedAt ? (
                                    <p className="text-[15px] italic text-muted">
                                        Tin nhắn đã bị xóa lúc {formatChatTime(m.removedAt)}
                                    </p>
                                ) : null}
                                {m.text && !m.removedAt && editingId === m.id ? (
                                    <form
                                        onSubmit={(e) => {
                                            e.preventDefault();
                                            const text = draft.trim();
                                            if (text && text !== m.text) onEdit(m.id, text);
                                            setEditingId(null);
                                        }}
                                        className="flex items-center gap-2"
                                    >
                                        <input
                                            autoFocus
                                            value={draft}
                                            onChange={(e) => setDraft(e.target.value)}
                                            maxLength={512}
                                            className="h-9 min-w-0 flex-1 rounded-md border border-line bg-surface px-2 text-sm text-ink"
                                        />
                                        <button type="submit" className="text-xs text-primary hover:underline">
                                            Lưu
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => setEditingId(null)}
                                            className="text-xs text-muted hover:underline"
                                        >
                                            Hủy
                                        </button>
                                    </form>
                                ) : m.text && !m.removedAt ? (
                                    <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed">
                                        {m.text}
                                    </p>
                                ) : null}
                                <p className="mt-1 text-right text-[11px] text-muted">
                                    {m.edited && !m.removedAt
                                        ? `Đã sửa lúc ${formatChatTime(m.editedAt ?? m.time)}`
                                        : formatChatTime(m.time)}
                                </p>
                                {!mine && !m.removedAt && reportingId !== m.id ? (
                                    <div className="mt-1 flex justify-start text-xs">
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setReportingId(m.id);
                                                setReportCategory("");
                                                setReportReason("");
                                            }}
                                            className="text-muted transition-colors hover:text-ink"
                                        >
                                            Báo cáo
                                        </button>
                                    </div>
                                ) : null}
                                {reportingId === m.id ? (
                                    <form
                                        onSubmit={(e) => {
                                            e.preventDefault();
                                            if (!reportCategory) return;
                                            onReport(m.id, reportCategory, reportReason.trim() || undefined);
                                            setReportingId(null);
                                        }}
                                        className="mt-2 space-y-2 rounded-md border border-line bg-surface p-2 text-xs"
                                    >
                                        <div className="flex flex-wrap gap-1.5">
                                            {REPORT_CATEGORIES.map((c) => (
                                                <button
                                                    key={c.value}
                                                    type="button"
                                                    onClick={() => setReportCategory(c.value)}
                                                    className={`rounded-full border px-2.5 py-1 transition-colors ${
                                                        reportCategory === c.value
                                                            ? "border-primary bg-primary text-white"
                                                            : "border-line text-muted hover:text-ink"
                                                    }`}
                                                >
                                                    {c.label}
                                                </button>
                                            ))}
                                        </div>
                                        <input
                                            value={reportReason}
                                            onChange={(e) => setReportReason(e.target.value)}
                                            maxLength={300}
                                            placeholder="Mô tả thêm (không bắt buộc)"
                                            className="h-8 w-full rounded border border-line bg-canvas px-2 text-ink"
                                        />
                                        <div className="flex justify-end gap-3">
                                            <button
                                                type="button"
                                                onClick={() => setReportingId(null)}
                                                className="text-muted hover:underline"
                                            >
                                                Hủy
                                            </button>
                                            <button
                                                type="submit"
                                                disabled={!reportCategory}
                                                className="text-primary hover:underline disabled:opacity-40"
                                            >
                                                Gửi báo cáo
                                            </button>
                                        </div>
                                    </form>
                                ) : null}
                                {mine && m.text && !m.removedAt && !m.attachment && editingId !== m.id ? (
                                    <div className="mt-1 flex justify-end gap-3 text-xs">
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setDraft(m.text);
                                                setEditingId(m.id);
                                            }}
                                            className="text-muted transition-colors hover:text-ink"
                                        >
                                            Sửa
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                if (window.confirm("Xóa tin nhắn này?")) onRemove(m.id);
                                            }}
                                            className="text-muted transition-colors hover:text-ink"
                                        >
                                            Xóa
                                        </button>
                                    </div>
                                ) : null}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}