"use client";

import { useId, useState, type FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { addManualEntry } from "@/lib/admin/academic-api";
import type { ManualKind } from "@/lib/admin/academic-api";
import { BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";
import { HELP, SECTION_TITLE } from "@/components/ui/styles";

export type ManualField = {
    /** Đúng tên cột trong file import */
    name: string;
    placeholder?: string;
    optional?: boolean;
};

type Feedback = { text: string; tone: "info" | "error" };

/** Form thêm một dòng. Dữ liệu đi qua đúng luồng import nên kiểm tra và chống trùng giống hệt file. */
export function ManualEntryForm({ kind, title, hint, fields }: {
    kind: ManualKind;
    title: string;
    hint: string;
    fields: ManualField[];
}) {
    const baseId = useId();
    const [values, setValues] = useState<Record<string, string>>({});
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    async function submit(event: FormEvent) {
        event.preventDefault();
        const missing = fields.filter((f) => !f.optional && !(values[f.name] ?? "").trim());
        if (missing.length > 0) {
            setNotice({ text: `Nhập: ${missing.map((f) => f.name).join(", ")}.`, tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await addManualEntry(kind, values);
            setNotice({ text: `Đã thêm 1 dòng vào mục ${title.toLowerCase()}.`, tone: "info" });
            setValues({});
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <section className={CARD} aria-label={`Thêm ${title} bằng form`}>
            <div className="flex flex-col gap-1">
                <h3 className={SECTION_TITLE}>Thêm {title.toLowerCase()} bằng form</h3>
                <p className={HELP}>{hint}</p>
            </div>
            <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2">
                {fields.map((field, index) => {
                    const id = `${baseId}-${index}`;
                    return (
                        <div key={field.name} className="flex flex-col gap-1">
                            <label htmlFor={id} className={LABEL}>
                                {field.name}{field.optional ? " (không bắt buộc)" : ""}
                            </label>
                            <input
                                id={id}
                                value={values[field.name] ?? ""}
                                placeholder={field.placeholder}
                                onChange={(e) => setValues({ ...values, [field.name]: e.target.value })}
                                className={INPUT}
                            />
                        </div>
                    );
                })}
                <div className="sm:col-span-2">
                    <button type="submit" disabled={busy} className={BTN_PRIMARY}>Thêm dòng</button>
                </div>
            </form>
            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
        </section>
    );
}