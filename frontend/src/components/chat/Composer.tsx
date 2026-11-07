"use client";

import { useRef, useState, type ChangeEvent } from "react";
import { ACCEPT, validateFile } from "@/lib/chat/files";

const MAX_LENGTH = 2000;

type ComposerProps = {
    // Trả về true nếu tin đã được gửi đi.
    onSend: (text: string, file: File | null) => Promise<boolean>;
};

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
                <div className="flex items-center gap-2 px-4 pt-3 text-sm">
                    {file ? (
                        <>
              <span className="min-w-0 truncate rounded-lg bg-paper px-3 py-1.5">
                📎 {file.name}
              </span>
                            <button
                                type="button"
                                onClick={() => setFile(null)}
                                className="text-xs text-muted underline-offset-2 hover:underline"
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
                className="flex items-end gap-2 px-4 py-3"
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
                    className="grid h-11 w-11 shrink-0 place-items-center rounded-xl border border-line text-lg transition-colors hover:bg-paper"
                >
                    +
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
                    className="max-h-40 min-h-11 flex-1 resize-none rounded-xl border border-line bg-paper px-3 py-2.5 text-[15px] outline-none focus-visible:ring-2 focus-visible:ring-sky"
                />
                <button
                    type="submit"
                    disabled={!canSend}
                    className="h-11 rounded-xl bg-primary px-5 text-sm font-medium text-primary-ink transition-opacity disabled:opacity-40"
                >
                    {sending ? "Đang gửi..." : "Gửi"}
                </button>
            </form>
        </div>
    );
}