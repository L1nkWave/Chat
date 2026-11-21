"use client";

import { useEffect, useId, useState, type FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { createSubject, deleteRecord, listRecords, updateRecord } from "@/lib/admin/academic-api";
import type { RecordKind, RecordPage, RecordRow } from "@/lib/admin/academic-api";
import { buttonStyle, CARD, FIELD, HELP, LABEL, SECTION_TITLE, TABLE, TABLE_WRAP, TD, TH } from "@/components/ui/styles";

type Feedback = { text: string; tone: "info" | "error" };

export type FieldSpec = {
    name: string;
    label: string;
    type: "text" | "date" | "time" | "number";
    editable: boolean;
};

export type RecordSpec = {
    kind: RecordKind;
    title: string;
    /** Khi bật, hiện form thêm mới (chỉ môn học) */
    canCreate?: boolean;
    fields: FieldSpec[];
};

const PAGE_SIZE = 20;

/** Bảng tìm kiếm, sửa, xóa cho một tab phụ của Học vụ */
export function RecordsTable({ spec }: { spec: RecordSpec }) {
    const searchId = useId();
    const [input, setInput] = useState("");
    const [query, setQuery] = useState("");
    const [page, setPage] = useState(0);
    const [version, setVersion] = useState(0);
    const [data, setData] = useState<RecordPage | null>(null);
    const [editingKey, setEditingKey] = useState<string | null>(null);
    const [draft, setDraft] = useState<Record<string, string>>({});
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    useEffect(() => {
        let active = true;
        listRecords(spec.kind, query, page, PAGE_SIZE)
            .then((result) => {
                if (active) setData(result);
            })
            .catch((err) => {
                if (active) setNotice({ text: adminErrorMessage(err), tone: "error" });
            });
        return () => {
            active = false;
        };
    }, [spec.kind, query, page, version]);

    const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

    function search(event: FormEvent) {
        event.preventDefault();
        setEditingKey(null);
        setPage(0);
        setQuery(input.trim());
    }

    function startEdit(row: RecordRow) {
        const next: Record<string, string> = {};
        for (const field of spec.fields) {
            if (field.editable) {
                next[field.name] = row[field.name] == null ? "" : String(row[field.name]);
            }
        }
        setEditingKey(String(row.key));
        setDraft(next);
        setNotice(null);
    }

    async function save(key: string) {
        setBusy(true);
        try {
            await updateRecord(spec.kind, key, draft);
            setNotice({ text: "Đã lưu thay đổi.", tone: "info" });
            setEditingKey(null);
            setVersion((v) => v + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function remove(row: RecordRow) {
        if (!window.confirm(`Xóa ${spec.title.toLowerCase()} ${String(row.key)}?`)) return;
        try {
            await deleteRecord(spec.kind, String(row.key));
            setNotice({ text: "Đã xóa.", tone: "info" });
            if (editingKey === String(row.key)) setEditingKey(null);
            setVersion((v) => v + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        }
    }

    return (
        <section className={CARD} aria-label={`Danh sách ${spec.title}`}>
            <div className="flex flex-wrap items-end justify-between gap-2">
                <h4 className={SECTION_TITLE}>
                    {spec.title} {data ? `(${data.total})` : ""}
                </h4>
                <form onSubmit={search} role="search" className="flex items-end gap-2">
                    <div className="flex flex-col gap-1">
                        <label htmlFor={searchId} className={LABEL}>Tìm kiếm</label>
                        <input
                            id={searchId}
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            placeholder="Nhập từ khóa"
                            className={FIELD}
                        />
                    </div>
                    <button type="submit" className={buttonStyle("secondary")}>Tìm</button>
                </form>
            </div>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <div className={`${TABLE_WRAP} max-h-96 overflow-y-auto`}>
                <table className={TABLE}>
                    <thead>
                    <tr>
                        {spec.fields.map((field) => (
                            <th key={field.name} className={TH}>{field.label}</th>
                        ))}
                        <th className={TH}><span className="sr-only">Thao tác</span></th>
                    </tr>
                    </thead>
                    <tbody>
                    {(data?.items ?? []).map((row) => {
                        const key = String(row.key);
                        const editing = editingKey === key;
                        if (editing) {
                            return (
                                <tr key={key} className="bg-tint">
                                    <td colSpan={spec.fields.length + 1} className="py-2">
                                        <div className="grid gap-2 sm:grid-cols-3">
                                            {spec.fields.map((field) => (
                                                <div key={field.name} className="flex flex-col gap-1">
                                                    <label className={LABEL} htmlFor={`${key}-${field.name}`}>{field.label}</label>
                                                    {field.editable ? (
                                                        <input
                                                            id={`${key}-${field.name}`}
                                                            type={field.type === "number" ? "number" : field.type === "date" ? "date" : field.type === "time" ? "time" : "text"}
                                                            value={draft[field.name] ?? ""}
                                                            onChange={(e) => setDraft({ ...draft, [field.name]: e.target.value })}
                                                            className={FIELD}
                                                        />
                                                    ) : (
                                                        <span className="text-sm text-muted">{row[field.name] == null ? "" : String(row[field.name])}</span>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                        <div className="mt-2 flex gap-2">
                                            <button type="button" disabled={busy} onClick={() => save(key)} className={buttonStyle("primary")}>
                                                Lưu
                                            </button>
                                            <button type="button" onClick={() => setEditingKey(null)} className={buttonStyle("secondary")}>
                                                Hủy
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            );
                        }
                        return (
                            <tr key={key}>
                                {spec.fields.map((field) => (
                                    <td key={field.name} className={TD}>
                                        {row[field.name] == null || row[field.name] === "" ? "-" : String(row[field.name])}
                                    </td>
                                ))}
                                <td className={`${TD} whitespace-nowrap`}>
                                    <button type="button" onClick={() => startEdit(row)} className={buttonStyle("secondary", "sm")}>Sửa</button>{" "}
                                    <button type="button" onClick={() => remove(row)} className={buttonStyle("danger", "sm")}>Xóa</button>
                                </td>
                            </tr>
                        );
                    })}
                    {data && data.items.length === 0 ? (
                        <tr>
                            <td colSpan={spec.fields.length + 1} className="py-3 text-center text-muted">
                                Không có bản ghi nào{query ? " khớp từ khóa" : ""}.
                            </td>
                        </tr>
                    ) : null}
                    </tbody>
                </table>
            </div>

            <div className="flex items-center justify-between gap-2 text-sm text-muted">
                <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)} className={buttonStyle("secondary")}>
                    Trang trước
                </button>
                <span>Trang {page + 1} / {totalPages}</span>
                <button
                    type="button"
                    disabled={!data || page + 1 >= totalPages}
                    onClick={() => setPage((p) => p + 1)}
                    className={buttonStyle("secondary")}
                >
                    Trang sau
                </button>
            </div>

            {spec.canCreate ? <CreateSubjectForm onCreated={() => setVersion((v) => v + 1)} /> : null}
        </section>
    );
}

function CreateSubjectForm({ onCreated }: { onCreated: () => void }) {
    const codeId = useId();
    const nameId = useId();
    const [code, setCode] = useState("");
    const [name, setName] = useState("");
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    async function submit(event: FormEvent) {
        event.preventDefault();
        if (!code.trim() || !name.trim()) {
            setNotice({ text: "Nhập mã môn và tên môn.", tone: "error" });
            return;
        }
        setBusy(true);
        try {
            await createSubject(code.trim(), name.trim());
            setNotice({ text: `Đã thêm môn ${code.trim()}.`, tone: "info" });
            setCode("");
            setName("");
            onCreated();
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <form onSubmit={submit} className="flex flex-col gap-3 border-t border-line pt-4">
            <span className={HELP}>Thêm môn học thủ công</span>
            <div className="grid gap-2 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor={codeId} className={LABEL}>Mã MH</label>
                    <input id={codeId} value={code} onChange={(e) => setCode(e.target.value)} className={FIELD} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={nameId} className={LABEL}>Tên môn học</label>
                    <input id={nameId} value={name} onChange={(e) => setName(e.target.value)} className={FIELD} />
                </div>
            </div>
            <div>
                <button type="submit" disabled={busy} className={buttonStyle("primary")}>Thêm môn</button>
            </div>
            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
        </form>
    );
}