"use client";

import { useEffect, useState } from "react";
import {
    fetchAttachmentBlob,
    formatSize,
    isImage,
} from "@/lib/chat/files";
import type { Attachment } from "@/lib/chat/types";

type AttachmentViewProps = {
    attachment: Attachment;
    mine: boolean;
};

export function AttachmentView({ attachment, mine }: AttachmentViewProps) {
    const image = isImage(attachment.contentType);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [zoomed, setZoomed] = useState(false);

    useEffect(() => {
        if (!image) return;
        let cancelled = false;
        let objectUrl: string | null = null;

        fetchAttachmentBlob(attachment.id)
            .then((blob) => {
                if (cancelled) return;
                objectUrl = URL.createObjectURL(blob);
                setPreviewUrl(objectUrl);
            })
            .catch(() => {
                if (!cancelled) setError("Không tải được ảnh.");
            });

        return () => {
            cancelled = true;
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [attachment.id, image]);

    // Đóng ảnh phóng to bằng phím Esc
    useEffect(() => {
        if (!zoomed) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === "Escape") setZoomed(false);
        };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [zoomed]);

    async function download() {
        setError(null);
        try {
            const blob = await fetchAttachmentBlob(attachment.id);
            const objectUrl = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = objectUrl;
            link.download = attachment.filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(objectUrl), 10_000);
        } catch {
            setError("Không tải được tệp.");
        }
    }

    if (image) {
        return (
            <div className="mb-1">
                {previewUrl ? (
                    <button
                        type="button"
                        onClick={() => setZoomed(true)}
                        aria-label={`Xem ảnh ${attachment.filename}`}
                        className="block max-w-full"
                    >
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                            src={previewUrl}
                            alt={attachment.filename}
                            className="max-h-72 max-w-full rounded-lg object-contain"
                        />
                    </button>
                ) : (
                    <div className="grid h-24 w-48 place-items-center rounded-lg bg-paper text-xs text-muted">
                        {error ?? "Đang tải ảnh..."}
                    </div>
                )}

                {zoomed && previewUrl ? (
                    <div
                        role="dialog"
                        aria-modal="true"
                        aria-label={attachment.filename}
                        onClick={() => setZoomed(false)}
                        className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-black/85 p-4"
                    >
                        <div className="absolute right-4 top-4 flex gap-2">
                            <button
                                type="button"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    void download();
                                }}
                                className="rounded-lg bg-white/90 px-3 py-1.5 text-sm text-black hover:bg-white"
                            >
                                Tải về
                            </button>
                            <button
                                type="button"
                                onClick={() => setZoomed(false)}
                                aria-label="Đóng xem ảnh"
                                className="rounded-lg bg-white/90 px-3 py-1.5 text-sm text-black hover:bg-white"
                            >
                                Đóng
                            </button>
                        </div>
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                            src={previewUrl}
                            alt={attachment.filename}
                            onClick={(e) => e.stopPropagation()}
                            className="max-h-full max-w-full rounded-lg object-contain"
                        />
                        <p className="text-sm text-white/80">{attachment.filename}</p>
                    </div>
                ) : null}
            </div>
        );
    }

    return (
        <div className="mb-1">
            <button
                type="button"
                onClick={download}
                className={`flex max-w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition-opacity hover:opacity-90 ${
                    mine ? "bg-primary-ink/10" : "bg-paper"
                }`}
            >
                <span className="min-w-0">
                    <span className="block truncate font-medium">{attachment.filename}</span>
                    <span className={`block text-xs ${mine ? "text-primary-ink/70" : "text-muted"}`}>
                        {formatSize(attachment.size)} · Nhấn để tải về
                    </span>
                </span>
            </button>
            {error ? <p className="mt-1 text-xs text-danger">{error}</p> : null}
        </div>
    );
}