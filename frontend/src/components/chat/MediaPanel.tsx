"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { buttonStyle, EMPTY, LINK, SECTION_TITLE } from "@/components/ui/styles";
import { chatApi, errorMessage } from "@/lib/chat/api";
import { fetchAttachmentBlob, formatSize } from "@/lib/chat/files";
import { formatChatTime, formatRemovedAt } from "@/lib/chat/time";
import type { MediaItem } from "@/lib/chat/types";

const PAGE_SIZE = 30;

type MediaKind = "IMAGE" | "FILE";

/**
 * Tải danh sách ảnh hoặc tệp theo trang. Trang sau nối vào sau.
 * Khi version đổi (có tin file mới hoặc tin bị xóa), tải lại từ trang đầu.
 */
function usePagedMedia(chatId: string, kind: MediaKind, version: number) {
    const [items, setItems] = useState<MediaItem[]>([]);
    const [hasMore, setHasMore] = useState(true);
    const [loaded, setLoaded] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const busy = useRef(false);
    const offset = useRef(0);
    const more = useRef(true);
    // Tăng mỗi lần tải lại, để kết quả của yêu cầu cũ không ghi đè dữ liệu mới
    const epoch = useRef(0);

    const fetchPage = useCallback(
        async (reset: boolean) => {
            if (busy.current || (!reset && !more.current)) return;
            const current = epoch.current;
            const from = reset ? 0 : offset.current;
            busy.current = true;
            setLoading(true);
            setError(null);
            try {
                const page = await chatApi.listMedia(chatId, kind, from, PAGE_SIZE);
                if (current !== epoch.current) return;
                offset.current = from + page.length;
                more.current = page.length === PAGE_SIZE;
                setItems((prev) => (reset ? page : [...prev, ...page]));
                setHasMore(more.current);
            } catch (err) {
                if (current === epoch.current) setError(errorMessage(err));
            } finally {
                if (current === epoch.current) {
                    busy.current = false;
                    setLoading(false);
                    setLoaded(true);
                }
            }
        },
        [chatId, kind],
    );

    // Lần đầu và mỗi khi version đổi: bắt đầu lại từ trang đầu
    useEffect(() => {
        epoch.current += 1;
        busy.current = false;
        offset.current = 0;
        more.current = true;
        void fetchPage(true);
    }, [fetchPage, version]);

    const loadMore = useCallback(() => void fetchPage(false), [fetchPage]);

    return { items, hasMore, loaded, loading, error, loadMore };
}

type MoreFooterProps = {
    hasMore: boolean;
    loading: boolean;
    error: string | null;
    count: number;
    onMore: () => void;
};

/** Khi cuộn tới cuối thì tải trang tiếp. Có nút dự phòng nếu trình duyệt không hỗ trợ hoặc có lỗi. */
function MoreFooter({ hasMore, loading, error, count, onMore }: MoreFooterProps) {
    const sentinel = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        const el = sentinel.current;
        // Có lỗi thì không tự thử lại, tránh lặp liên tục; người dùng bấm "Thử lại"
        if (!el || !hasMore || loading || error || typeof IntersectionObserver === "undefined") return;
        const observer = new IntersectionObserver(
            (entries) => {
                if (entries.some((entry) => entry.isIntersecting)) onMore();
            },
            { rootMargin: "120px" },
        );
        observer.observe(el);
        return () => observer.disconnect();
    }, [hasMore, loading, error, onMore, count]);

    return (
        <div className="flex flex-col items-center gap-2">
            {error ? <p className="text-xs text-danger">{error}</p> : null}
            {hasMore ? (
                <>
                    <div ref={sentinel} aria-hidden="true" className="h-px w-full" />
                    <button
                        type="button"
                        onClick={onMore}
                        disabled={loading}
                        className={buttonStyle("secondary", "sm")}
                    >
                        {loading ? "Đang tải..." : error ? "Thử lại" : "Tải thêm"}
                    </button>
                </>
            ) : null}
        </div>
    );
}

type RemoveProps = {
    /** Mục này có được xóa bởi người đang đăng nhập không */
    canRemove: (item: MediaItem) => boolean;
    onRemove: (item: MediaItem) => void;
};

/** Ô ảnh: tải nội dung bằng token rồi dùng blob URL, thu hồi khi rời khỏi màn hình */
function ImageThumb({ item, canRemove, onRemove }: { item: MediaItem } & RemoveProps) {
    const removedAt = item.removedAt;
    const [url, setUrl] = useState<string | null>(null);
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        if (removedAt) return;
        let cancelled = false;
        let objectUrl: string | null = null;
        fetchAttachmentBlob(item.messageId)
            .then((blob) => {
                if (cancelled) return;
                objectUrl = URL.createObjectURL(blob);
                setUrl(objectUrl);
            })
            .catch(() => {
                if (!cancelled) setFailed(true);
            });
        return () => {
            cancelled = true;
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [item.messageId, removedAt]);

    if (removedAt) {
        return (
            <div className="grid h-full place-items-center rounded-md border border-line bg-surface p-2 text-center text-xs leading-snug text-muted">
                Tệp đã bị xóa lúc {formatRemovedAt(removedAt)}
            </div>
        );
    }

    if (!url) {
        return (
            <div className="grid h-full place-items-center rounded-md border border-line bg-surface text-xs text-muted">
                {failed ? "Không tải được" : "Đang tải..."}
            </div>
        );
    }

    return (
        <div className="flex h-full w-full flex-col gap-1">
            <div className="relative min-h-0 flex-1">
            <button
                type="button"
                onClick={() => window.open(url, "_blank", "noopener,noreferrer")}
                aria-label={`Mở ảnh ${item.filename}`}
                className="block h-full w-full overflow-hidden rounded-md bg-paper"
            >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={url} alt={item.filename} className="h-full w-full object-cover" />
            </button>
            </div>
            {canRemove(item) ? (
                <div className="flex justify-end">
                    <button
                        type="button"
                        onClick={() => onRemove(item)}
                        aria-label={`Xóa ảnh ${item.filename}`}
                        className="text-xs text-muted underline-offset-4 transition-colors hover:text-danger hover:underline"
                    >
                        Xóa
                    </button>
                </div>
            ) : null}
        </div>
    );
}

function ImageGrid({ chatId, version, canRemove, onRemove }: { chatId: string; version: number } & RemoveProps) {
    const { items, hasMore, loaded, loading, error, loadMore } = usePagedMedia(chatId, "IMAGE", version);

    return (
        <div className="flex flex-col gap-3">
            {!loaded ? <p className="text-sm text-muted">Đang tải...</p> : null}
            {loaded && items.length === 0 ? <p className={EMPTY}>Chưa có ảnh nào.</p> : null}
            {items.length > 0 ? (
                <ul className="grid grid-cols-3 gap-2">
                    {items.map((item) => (
                        <li key={item.messageId} className="aspect-square min-w-0">
                            <ImageThumb item={item} canRemove={canRemove} onRemove={onRemove} />
                        </li>
                    ))}
                </ul>
            ) : null}
            <MoreFooter
                hasMore={hasMore}
                loading={loading}
                error={error}
                count={items.length}
                onMore={loadMore}
            />
        </div>
    );
}

function FileList({ chatId, version, canRemove, onRemove }: { chatId: string; version: number } & RemoveProps) {
    const { items, hasMore, loaded, loading, error, loadMore } = usePagedMedia(chatId, "FILE", version);
    const [downloadError, setDownloadError] = useState<string | null>(null);

    async function download(item: MediaItem) {
        setDownloadError(null);
        try {
            const blob = await fetchAttachmentBlob(item.messageId);
            const objectUrl = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = objectUrl;
            link.download = item.filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.setTimeout(() => URL.revokeObjectURL(objectUrl), 10_000);
        } catch {
            setDownloadError("Không tải được tệp.");
        }
    }

    return (
        <div className="flex flex-col gap-3">
            {!loaded ? <p className="text-sm text-muted">Đang tải...</p> : null}
            {loaded && items.length === 0 ? <p className={EMPTY}>Chưa có tệp nào.</p> : null}
            {items.length > 0 ? (
                <ul className="flex flex-col gap-2">
                    {items.map((item) => {
                        const removedAt = item.removedAt;
                        return (
                            <li
                                key={item.messageId}
                                className="flex min-w-0 flex-col gap-1 rounded-md border border-line bg-surface px-3 py-2.5"
                            >
                                <span className="min-w-0 flex-1">
                                    <span className="block truncate text-sm font-medium text-ink">{item.filename}</span>
                                    <span className="block truncate text-xs tabular-nums text-muted">
                                        {removedAt
                                            ? `Tệp đã bị xóa lúc ${formatRemovedAt(removedAt)}`
                                            : `${formatSize(item.size)} · ${formatChatTime(item.createdAt)}`}
                                    </span>
                                </span>
                                {removedAt ? null : (
                                    <div className="flex justify-end gap-3">
                                        <button
                                            type="button"
                                            onClick={() => void download(item)}
                                            className={`${LINK} text-xs`}
                                        >
                                            Tải về
                                        </button>
                                        {canRemove(item) ? (
                                            <button
                                                type="button"
                                                onClick={() => onRemove(item)}
                                                aria-label={`Xóa tệp ${item.filename}`}
                                                className="text-xs text-muted underline-offset-4 transition-colors hover:text-danger hover:underline"
                                            >
                                                Xóa
                                            </button>
                                        ) : null}
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            ) : null}
            {downloadError ? <p className="text-xs text-danger">{downloadError}</p> : null}
            <MoreFooter
                hasMore={hasMore}
                loading={loading}
                error={error}
                count={items.length}
                onMore={loadMore}
            />
        </div>
    );
}

type MediaPanelProps = {
    chatId: string;
    /** Tăng mỗi khi có tin file mới hoặc tin bị xóa trong đoạn chat, để danh sách tự tải lại */
    version: number;
    currentUserId: string;
    /** Admin nhóm được xóa mọi tệp trong nhóm */
    canModerate: boolean;
    /** Xóa tin qua WebSocket; server phát sự kiện tới mọi thành viên */
    onRemove: (messageId: string) => void;
};

/** Tab "Ảnh & file" của panel thông tin: lưới ảnh và danh sách tệp, tải thêm khi cuộn. */
export function MediaPanel({ chatId, version, currentUserId, canModerate, onRemove }: MediaPanelProps) {
    const canRemove = useCallback(
        (item: MediaItem) =>
            !item.removedAt && (canModerate || String(item.authorId) === currentUserId),
        [canModerate, currentUserId],
    );

    const remove = useCallback(
        (item: MediaItem) => {
            if (!window.confirm(`Xóa "${item.filename}"? Tệp sẽ bị xóa với mọi thành viên trong đoạn chat.`)) return;
            onRemove(item.messageId);
        },
        [onRemove],
    );

    return (
        <div className="flex flex-col gap-6 p-4">
            <section aria-label="Ảnh" className="flex flex-col gap-2">
                <h3 className={SECTION_TITLE}>Ảnh</h3>
                <ImageGrid chatId={chatId} version={version} canRemove={canRemove} onRemove={remove} />
            </section>
            <section aria-label="Tệp" className="flex flex-col gap-2">
                <h3 className={SECTION_TITLE}>Tệp</h3>
                <FileList chatId={chatId} version={version} canRemove={canRemove} onRemove={remove} />
            </section>
        </div>
    );
}
