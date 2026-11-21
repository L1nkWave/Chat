"use client";

import { useEffect, useState } from "react";
import {
    fetchAttachmentBlob,
    formatSize,
    isImage,
} from "@/lib/chat/files";
import { buttonStyle } from "@/components/ui/styles";
import type { Attachment } from "@/lib/chat/types";

type AttachmentViewProps = {
    attachment: Attachment;
    mine: boolean;
};

export function AttachmentView({ attachment }: AttachmentViewProps) {
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
                            className="max-h-72 max-w-full rounded-md object-contain"
                        />
                    </button>
                ) : (
                    <div className="grid h-24 w-48 place-items-center rounded-md border border-line bg-surface text-xs text-muted">
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
                                className={buttonStyle("secondary", "sm")}
                            >
                                Tải về
                            </button>
                            <button
                                type="button"
                                onClick={() => setZoomed(false)}
                                aria-label="Đóng xem ảnh"
                                className={buttonStyle("secondary", "sm")}
                            >
                                Đóng
                            </button>
                        </div>
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                            src={previewUrl}
                            alt={attachment.filename}
                            onClick={(e) => e.stopPropagation()}
                            className="max-h-full max-w-full rounded-md object-contain"
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
                className="flex max-w-full items-center gap-3 rounded-md border border-line bg-surface px-3 py-2.5 text-left text-sm text-ink transition-colors hover:bg-tint"
            >
                <span className="min-w-0">
                    <span className="block truncate font-medium">{attachment.filename}</span>
                    <span className="block text-xs tabular-nums text-muted">
                        {formatSize(attachment.size)} · Nhấn để tải về
                    </span>
                </span>
            </button>
            {error ? <p className="mt-1 text-xs text-danger">{error}</p> : null}
        </div>
    );
}