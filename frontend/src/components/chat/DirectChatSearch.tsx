"use client";

import { useEffect, useId, useState, type FormEvent, type KeyboardEvent } from "react";
import { buttonStyle, FIELD } from "@/components/ui/styles";
import { chatApi, errorMessage } from "@/lib/chat/api";
import type { UserSuggestion } from "@/lib/chat/types";

const DEBOUNCE_MS = 300;

type DirectChatSearchProps = {
    busy: boolean;
    // Được gọi khi người dùng chọn một gợi ý hoặc bấm "Mở" với từ khóa khớp
    onPick: (user: UserSuggestion) => void;
};

/** Ô tìm người để nhắn riêng: gợi ý ngay khi gõ (debounce), chọn một gợi ý là mở chat. */
export function DirectChatSearch({ busy, onPick }: DirectChatSearchProps) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState<UserSuggestion[]>([]);
    const [searching, setSearching] = useState(false);
    const [open, setOpen] = useState(false);
    const [active, setActive] = useState(-1);
    const [error, setError] = useState<string | null>(null);
    const listId = useId();

    const keyword = query.trim();
    // Kết quả chỉ hiện khi khớp với từ khóa đang gõ
    const shown = keyword ? results : [];
    const showList = open && keyword.length > 0 && (shown.length > 0 || searching);

    useEffect(() => {
        if (!keyword) return;
        let cancelled = false;
        const timer = window.setTimeout(() => {
            chatApi
                .searchUsers(keyword)
                .then((list) => {
                    if (cancelled) return;
                    setResults(list);
                    setActive(-1);
                    setError(null);
                })
                .catch((err) => {
                    if (!cancelled) setError(errorMessage(err));
                })
                .finally(() => {
                    if (!cancelled) setSearching(false);
                });
        }, DEBOUNCE_MS);
        return () => {
            cancelled = true;
            window.clearTimeout(timer);
        };
    }, [keyword]);

    function onChange(value: string) {
        setQuery(value);
        setOpen(true);
        setActive(-1);
        setError(null);
        setSearching(value.trim().length > 0);
        if (!value.trim()) setResults([]);
    }

    function pick(user: UserSuggestion) {
        setQuery(user.name || user.username);
        setOpen(false);
        setActive(-1);
        onPick(user);
    }

    function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
        if (e.key === "ArrowDown" || e.key === "ArrowUp") {
            if (shown.length === 0) return;
            e.preventDefault();
            setOpen(true);
            setActive((i) => {
                const step = e.key === "ArrowDown" ? 1 : -1;
                return (i + step + shown.length) % shown.length;
            });
        } else if (e.key === "Escape") {
            setOpen(false);
        }
    }

    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        if (busy || !keyword) return;
        if (showList && active >= 0 && shown[active]) {
            pick(shown[active]);
            return;
        }
        // Chưa chọn gợi ý: tìm ngay theo từ khóa, ưu tiên tên đăng nhập/mã khớp đúng
        setSearching(true);
        try {
            const list = await chatApi.searchUsers(keyword);
            const exact = list.find((u) => u.username.toLowerCase() === keyword.toLowerCase());
            const target = exact ?? list[0];
            if (!target) {
                setError("Không tìm thấy người dùng này.");
                return;
            }
            pick(target);
        } catch (err) {
            setError(errorMessage(err));
        } finally {
            setSearching(false);
        }
    }

    return (
        <form onSubmit={submit} className="flex flex-col gap-1.5 px-4 pb-3">
            <div className="flex gap-2">
                <div className="relative min-w-0 flex-1">
                    <input
                        role="combobox"
                        value={query}
                        onChange={(e) => onChange(e.target.value)}
                        onKeyDown={onKeyDown}
                        onFocus={() => setOpen(true)}
                        onBlur={() => setOpen(false)}
                        aria-expanded={showList}
                        aria-controls={listId}
                        aria-autocomplete="list"
                        aria-activedescendant={showList && active >= 0 ? `${listId}-${active}` : undefined}
                        placeholder="Tìm theo tên hoặc mã người dùng"
                        aria-label="Tìm người để nhắn riêng"
                        autoComplete="off"
                        autoCapitalize="none"
                        spellCheck={false}
                        disabled={busy}
                        className={FIELD}
                    />
                    {showList ? (
                        <ul
                            id={listId}
                            role="listbox"
                            aria-label="Gợi ý người dùng"
                            className="absolute left-0 right-0 top-full z-20 mt-1 max-h-72 overflow-y-auto rounded-md border border-line bg-surface py-1 shadow-md"
                        >
                            {shown.map((u, index) => (
                                <li
                                    key={u.id}
                                    id={`${listId}-${index}`}
                                    role="option"
                                    aria-selected={index === active}
                                    onMouseDown={(e) => e.preventDefault()}
                                    onClick={() => pick(u)}
                                    className={`flex cursor-pointer flex-col px-3 py-2 text-sm text-ink ${
                                        index === active ? "bg-tint" : "hover:bg-paper"
                                    }`}
                                >
                                    <span className="truncate font-medium">{u.name || u.username}</span>
                                    <span className="truncate text-xs text-muted">@{u.username}</span>
                                </li>
                            ))}
                            {shown.length === 0 ? (
                                <li className="px-3 py-2 text-xs text-muted">Đang tìm...</li>
                            ) : null}
                        </ul>
                    ) : null}
                </div>
                <button
                    type="submit"
                    disabled={busy || !keyword}
                    className={buttonStyle("primary", "md")}
                >
                    Mở
                </button>
            </div>
            {error ? <p className="text-xs text-danger">{error}</p> : null}
        </form>
    );
}
