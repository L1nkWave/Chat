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
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                        src={previewUrl}
                        alt={attachment.filename}
                        className="max-h-72 max-w-full rounded-lg object-contain"
                    />
                ) : (
                    <div className="grid h-24 w-48 place-items-center rounded-lg bg-paper text-xs text-muted">
                        {error ?? "Đang tải ảnh..."}
                    </div>
                )}
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