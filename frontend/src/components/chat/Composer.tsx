"use client";

import { useRef, useState, type ChangeEvent } from "react";
import { ACCEPT, formatSize, validateFile } from "@/lib/chat/files";
import { buttonStyle } from "@/components/ui/styles";

const MAX_LENGTH = 2000;

type ComposerProps = {
    // Gửi một tệp thành một tin nhắn. Trả về thông báo lỗi, hoặc null nếu đã gửi.
    onSendFile: (file: File) => Promise<string | null>;
    // Gửi phần chữ. Trả về true nếu đã gửi.
    onSendText: (text: string) => Promise<boolean>;
};

// Tệp đang chờ gửi. Tệp gửi lỗi giữ lại kèm thông báo để người dùng thử lại.
type PendingFile = { file: File; error?: string };

function PaperclipIcon({ size = 18 }: { size?: number }) {
    return (
        <svg
            viewBox="0 0 24 24"
            width={size}
            height={size}
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48" />
        </svg>
    );
}

function SendIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            width={18}
            height={18}
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            <path d="M5 12h14M13 6l6 6-6 6" />
        </svg>
    );
}

export function Composer({ onSendFile, onSendText }: ComposerProps) {
    const [draft, setDraft] = useState("");
    const [pending, setPending] = useState<PendingFile[]>([]);
    const [pickErrors, setPickErrors] = useState<string[]>([]);
    const [sending, setSending] = useState(false);
    // Tệp đang gửi và tiến độ (số tệp đã xong / tổng số)
    const [current, setCurrent] = useState<File | null>(null);
    const [progress, setProgress] = useState({ done: 0, total: 0 });
    const inputRef = useRef<HTMLInputElement>(null);

    const text = draft.trim();
    const canSend = !sending && (text.length > 0 || pending.length > 0);

    function pickFiles(e: ChangeEvent<HTMLInputElement>) {
        const picked: File[] = Array.from(e.target.files ?? []);
        e.target.value = ""; // cho phép chọn lại cùng các tệp
        if (picked.length === 0) return;

        const accepted: PendingFile[] = [];
        const errors: string[] = [];
        for (const file of picked) {
            const error = validateFile(file);
            if (error) errors.push(`${file.name}: ${error}`);
            else accepted.push({ file });
        }
        setPickErrors(errors);
        setPending((prev) => [...prev, ...accepted]);
    }

    function removePending(index: number) {
        setPending((prev) => prev.filter((_, i) => i !== index));
    }

    // Gửi lần lượt từng tệp. Tệp lỗi không chặn các tệp còn lại; sau đó mới gửi phần chữ.
    async function submit() {
        if (!canSend) return;
        setSending(true);
        setPickErrors([]);
        // Gửi lại cả các tệp lỗi trước đó; xóa thông báo lỗi cũ trước khi gửi
        const queue = pending.map((p) => p.file);
        setPending((prev) => prev.map(({ file }) => ({ file })));
        setProgress({ done: 0, total: queue.length });

        let done = 0;
        for (const file of queue) {
            setCurrent(file);
            const error = await onSendFile(file);
            done += 1;
            setProgress({ done, total: queue.length });
            setPending((prev) =>
                error === null
                    ? prev.filter((p) => p.file !== file)
                    : prev.map((p) => (p.file === file ? { ...p, error } : p)),
            );
        }
        setCurrent(null);

        if (text) {
            const ok = await onSendText(text);
            if (ok) setDraft("");
        }
        setProgress({ done: 0, total: 0 });
        setSending(false);
    }

    return (
        <div className="border-t border-line bg-surface">
            {pickErrors.length > 0 ? (
                <ul className="flex flex-col gap-1 px-4 pt-3 text-xs text-danger sm:px-8">
                    {pickErrors.map((message) => (
                        <li key={message}>{message}</li>
                    ))}
                </ul>
            ) : null}

            {pending.length > 0 ? (
                <div className="flex flex-col gap-2 px-4 pt-3 text-sm sm:px-8">
                    {sending && progress.total > 0 ? (
                        <p className="text-xs text-muted" role="status">
                            Đang gửi tệp {Math.min(progress.done + 1, progress.total)}/{progress.total}
                        </p>
                    ) : null}
                    <ul aria-label="Tệp chờ gửi" className="flex max-h-32 flex-col gap-1.5 overflow-y-auto">
                        {pending.map((item, index) => (
                            <li
                                key={`${index}-${item.file.name}`}
                                className="flex min-w-0 items-center gap-3 rounded-md border border-line bg-paper px-3 py-1.5 text-ink"
                            >
                                <PaperclipIcon size={15} />
                                <span className="min-w-0 flex-1">
                                    <span className="block truncate">{item.file.name}</span>
                                    {item.error ? (
                                        <span className="block truncate text-xs text-danger">
                                            Không gửi được: {item.error}
                                        </span>
                                    ) : (
                                        <span className="block text-xs text-muted">
                                            {current === item.file ? "Đang gửi..." : formatSize(item.file.size)}
                                        </span>
                                    )}
                                </span>
                                <button
                                    type="button"
                                    onClick={() => removePending(index)}
                                    disabled={sending}
                                    aria-label={`Bỏ tệp ${item.file.name}`}
                                    className="shrink-0 text-xs font-medium text-danger underline-offset-4 hover:underline disabled:opacity-40"
                                >
                                    Bỏ
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>
            ) : null}

            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    void submit();
                }}
                className="flex items-end gap-2 px-4 py-3 sm:px-8"
            >
                <input
                    ref={inputRef}
                    type="file"
                    multiple
                    accept={ACCEPT}
                    onChange={pickFiles}
                    className="sr-only"
                    tabIndex={-1}
                    aria-hidden="true"
                />
                <button
                    type="button"
                    onClick={() => inputRef.current?.click()}
                    disabled={sending}
                    aria-label="Đính kèm tệp"
                    className={buttonStyle("secondary", "sm", "w-8 px-0!")}
                >
                    <PaperclipIcon />
                </button>
                <textarea
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    onKeyDown={(e) => {
                        // Enter gửi tin, Shift+Enter xuống dòng, bỏ qua khi đang gõ IME tiếng Việt.
                        if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
                            e.preventDefault();
                            void submit();
                        }
                    }}
                    rows={1}
                    maxLength={MAX_LENGTH}
                    placeholder="Nhập tin nhắn"
                    aria-label="Nhập tin nhắn"
                    className="max-h-40 min-h-10 flex-1 resize-none rounded-md border border-line bg-surface px-3 py-2 text-[15px] leading-6 text-ink outline-none transition-colors placeholder:text-muted focus:border-primary focus-visible:ring-2 focus-visible:ring-primary/25"
                />
                <button
                    type="submit"
                    disabled={!canSend}
                    aria-label={sending ? "Đang gửi" : "Gửi tin nhắn"}
                    className={buttonStyle("primary", "md")}
                >
                    <span className="hidden sm:inline">{sending ? "Đang gửi..." : "Gửi"}</span>
                    <SendIcon />
                </button>
            </form>
        </div>
    );
}
