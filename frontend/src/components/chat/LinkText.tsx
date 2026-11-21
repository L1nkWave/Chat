"use client";

import { useEffect, useId, useMemo, useState } from "react";
import { buttonStyle, LINK, SECTION_TITLE } from "@/components/ui/styles";
import { splitLinks } from "@/lib/chat/links";

type LinkTextProps = {
    text: string;
};

/**
 * Hiển thị văn bản tin nhắn. Liên kết http(s) là chữ có gạch chân; bấm vào mới mở hộp xác nhận.
 * Không dùng thẻ <a href> trong tin nhắn và không xem trước liên kết.
 */
export function LinkText({ text }: LinkTextProps) {
    const [pendingUrl, setPendingUrl] = useState<string | null>(null);
    const parts = useMemo(() => splitLinks(text), [text]);

    return (
        <>
            {parts.map((part, index) =>
                part.url ? (
                    <button
                        key={index}
                        type="button"
                        onClick={() => setPendingUrl(part.url)}
                        className={`${LINK} break-all text-left underline`}
                    >
                        {part.text}
                    </button>
                ) : (
                    <span key={index}>{part.text}</span>
                ),
            )}
            {pendingUrl ? <LinkConfirm url={pendingUrl} onClose={() => setPendingUrl(null)} /> : null}
        </>
    );
}

type LinkConfirmProps = {
    url: string;
    onClose: () => void;
};

function LinkConfirm({ url, onClose }: LinkConfirmProps) {
    const titleId = useId();
    const host = new URL(url).hostname;

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === "Escape") onClose();
        };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [onClose]);

    return (
        <div
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            onClick={onClose}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        >
            <div
                onClick={(e) => e.stopPropagation()}
                className="flex w-full max-w-md flex-col gap-4 rounded-lg border border-line bg-surface p-5 text-ink shadow-md"
            >
                <h3 id={titleId} className={SECTION_TITLE}>
                    Mở liên kết bên ngoài?
                </h3>
                <div className="flex flex-col gap-1.5 text-sm">
                    <p className="text-muted">
                        Tên miền: <span className="font-medium text-ink break-all">{host}</span>
                    </p>
                    <p className="break-all rounded-md border border-line bg-paper p-2 font-mono text-xs leading-relaxed">
                        {url}
                    </p>
                </div>
                <p className="text-xs text-muted">Chỉ mở liên kết khi bạn tin tưởng nguồn gửi.</p>
                <div className="flex justify-end gap-2">
                    <button type="button" onClick={onClose} className={buttonStyle("secondary")}>
                        Hủy
                    </button>
                    <a
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer nofollow"
                        onClick={() => window.setTimeout(onClose, 0)}
                        className={buttonStyle("primary")}
                    >
                        Mở liên kết
                    </a>
                </div>
            </div>
        </div>
    );
}
