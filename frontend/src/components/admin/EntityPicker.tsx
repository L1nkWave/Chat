"use client";

import { useEffect, useState } from "react";

import { INPUT, LABEL } from "./shared";
import { buttonStyle, HELP } from "@/components/ui/styles";

export type PickerOption = {
    id: number;
    label: string;
    hint?: string;
};

type EntityPickerProps = {
    id: string;
    label: string;
    placeholder?: string;
    /** Phải là hàm có tham chiếu ổn định (khai báo ngoài component) */
    search: (query: string) => Promise<PickerOption[]>;
    value: PickerOption | null;
    onChange: (option: PickerOption | null) => void;
};

/** Ô chọn theo tên: gõ để tìm, bấm để chọn, bấm "Bỏ chọn" để đổi. */
export function EntityPicker({ id, label, placeholder, search, value, onChange }: EntityPickerProps) {
    const [text, setText] = useState("");
    const [options, setOptions] = useState<PickerOption[]>([]);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!open) return;
        let alive = true;
        const timer = window.setTimeout(() => {
            setLoading(true);
            search(text.trim())
                .then((items) => {
                    if (alive) setOptions(items);
                })
                .catch(() => {
                    if (alive) setOptions([]);
                })
                .finally(() => {
                    if (alive) setLoading(false);
                });
        }, 250);
        return () => {
            alive = false;
            window.clearTimeout(timer);
        };
    }, [text, open, search]);

    if (value) {
        return (
            <div className="flex flex-col gap-1">
                <span className={LABEL}>{label}</span>
                <div className="flex h-10 items-center justify-between gap-2 rounded-md border border-line bg-paper px-3 text-sm text-ink">
                    <span className="min-w-0 truncate">
                        {value.label}
                        {value.hint ? <span className="text-muted"> · {value.hint}</span> : null}
                    </span>
                    <button
                        type="button"
                        onClick={() => onChange(null)}
                        className={buttonStyle("ghost", "sm")}
                    >
                        Bỏ chọn
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-1">
            <label htmlFor={id} className={LABEL}>
                {label}
            </label>
            <input
                id={id}
                value={text}
                placeholder={placeholder}
                autoComplete="off"
                onFocus={() => setOpen(true)}
                onBlur={() => window.setTimeout(() => setOpen(false), 150)}
                onChange={(event) => {
                    setText(event.target.value);
                    setOpen(true);
                }}
                className={INPUT}
            />
            {open ? (
                <ul role="listbox" aria-label={label} className="flex flex-col divide-y divide-line overflow-hidden rounded-md border border-line bg-surface">
                    {loading ? <li className="px-3 py-2 text-sm text-muted">Đang tìm...</li> : null}
                    {!loading && options.length === 0 ? (
                        <li className="px-3 py-2 text-sm text-muted">Không có kết quả.</li>
                    ) : null}
                    {!loading
                        ? options.map((item) => (
                            <li key={item.id}>
                                <button
                                    type="button"
                                    onMouseDown={(event) => event.preventDefault()}
                                    onClick={() => {
                                        onChange(item);
                                        setText("");
                                        setOpen(false);
                                    }}
                                    className="flex w-full flex-col items-start px-3 py-2 text-left text-sm transition-colors hover:bg-tint"
                                >
                                    <span>{item.label}</span>
                                    {item.hint ? <span className={HELP}>{item.hint}</span> : null}
                                </button>
                            </li>
                        ))
                        : null}
                </ul>
            ) : null}
        </div>
    );
}