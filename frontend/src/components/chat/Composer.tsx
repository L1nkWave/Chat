"use client";

import { useRef, useState, type ChangeEvent } from "react";
import { ACCEPT, validateFile } from "@/lib/chat/files";

const MAX_LENGTH = 2000;

type ComposerProps = {
    // Trả về true nếu tin đã được gửi đi.
    onSend: (text: string, file: File | null) => Promise<boolean>;
};

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

export function Composer({ onSend }: ComposerProps) {
    const [draft, setDraft] = useState("");
    const [file, setFile] = useState<File | null>(null);
    const [fileError, setFileError] = useState<string | null>(null);
    const [sending, setSending] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);

    const text = draft.trim();
    const canSend = !sending && (text.length > 0 || file !== null);

    function pickFile(e: ChangeEvent<HTMLInputElement>) {
        const picked = e.target.files?.[0] ?? null;
        e.target.value = ""; // cho phép chọn lại cùng một tệp
        if (!picked) return;
        const error = validateFile(picked);
        setFileError(error);
        setFile(error ? null : picked);
    }

    async function submit() {
        if (!canSend) return;
        setSending(true);
        const ok = await onSend(text, file);
        setSending(false);
        if (ok) {
            setDraft("");
            setFile(null);
            setFileError(null);
        }
    }

    return (
        <div className="border-t border-line bg-surface">
            {file || fileError ? (
                <div className="flex items-center gap-3 px-4 pt-3 text-sm sm:px-8">
                    {file ? (
                        <>
                            <span className="flex min-w-0 items-center gap-2 rounded-md border border-line bg-paper px-3 py-1.5 text-ink">
                                <PaperclipIcon size={15} />
                                <span className="truncate">{file.name}</span>
                            </span>
                            <button
                                type="button"
                                onClick={() => setFile(null)}
                                className="text-xs text-muted underline-offset-4 hover:text-ink hover:underline"
                            >
                                Bỏ tệp
                            </button>
                        </>
                    ) : (
                        <span className="text-xs text-danger">{fileError}</span>
                    )}
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
                    accept={ACCEPT}
                    onChange={pickFile}
                    className="sr-only"
                    tabIndex={-1}
                    aria-hidden="true"
                />
                <button
                    type="button"
                    onClick={() => inputRef.current?.click()}
                    aria-label="Đính kèm tệp"
                    className="grid h-11 w-11 shrink-0 place-items-center rounded-md border border-line bg-surface text-muted transition-colors hover:bg-tint hover:text-ink"
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
                    className="max-h-40 min-h-11 flex-1 resize-none rounded-md border border-line bg-paper px-3.5 py-2.5 text-[15px] text-ink outline-none transition-colors placeholder:text-muted focus:border-primary"
                />
                <button
                    type="submit"
                    disabled={!canSend}
                    aria-label={sending ? "Đang gửi" : "Gửi tin nhắn"}
                    className="flex h-11 items-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-ink transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-40"
                >
                    <span className="hidden sm:inline">{sending ? "Đang gửi..." : "Gửi"}</span>
                    <SendIcon />
                </button>
            </form>
        </div>
    );
}