"use client";

import { useEffect, useId, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import {
    deleteClass,
    deleteFaculty,
    listClasses,
    listFaculties,
    saveClass,
    saveFaculty,
} from "@/lib/admin/academic-api";
import type { ClassRow, FacultyRow } from "@/lib/admin/academic-api";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";

type Feedback = { text: string; tone: "info" | "error" };

/** Thêm, sửa, xóa khoa và lớp bằng form. Dùng chung bảng với import Excel. */
export function CatalogManager() {
    const [faculties, setFaculties] = useState<FacultyRow[]>([]);
    const [classes, setClasses] = useState<ClassRow[]>([]);
    const [notice, setNotice] = useState<Feedback | null>(null);
    const [filterInput, setFilterInput] = useState("");
    const [filter, setFilter] = useState("");

    const needle = filter.trim().toLowerCase();
    const shownFaculties = needle
        ? faculties.filter((f) => `${f.code} ${f.name}`.toLowerCase().includes(needle))
        : faculties;
    const shownClasses = needle
        ? classes.filter((c) => `${c.code} ${c.name} ${c.facultyCode} ${c.cohort ?? ""}`.toLowerCase().includes(needle))
        : classes;

    async function reload() {
        try {
            const [f, c] = await Promise.all([listFaculties(), listClasses()]);
            setFaculties(f);
            setClasses(c);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        }
    }

    useEffect(() => {
        void reload();
    }, []);

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <h3 className="text-base font-semibold">Quản lý khoa và lớp</h3>
                <p className="text-xs text-muted">
                    Thêm hoặc sửa từng khoa, lớp bằng form. Không xóa được khoa còn lớp, hoặc lớp còn sinh viên và thời khóa biểu.
                    Không đổi mã lớp đã có sinh viên.
                </p>
            </div>
            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
            <form
                role="search"
                className="flex flex-wrap items-end gap-2"
                onSubmit={(event) => {
                    event.preventDefault();
                    setFilter(filterInput);
                }}
            >
                <div className="flex flex-col gap-1">
                    <label htmlFor="catalog-search" className={LABEL}>Tìm khoa hoặc lớp</label>
                    <input
                        id="catalog-search"
                        value={filterInput}
                        onChange={(event) => setFilterInput(event.target.value)}
                        placeholder="Mã, tên, niên khóa"
                        className={INPUT}
                    />
                </div>
                <button type="submit" className={BTN_GHOST}>Tìm</button>
            </form>
            <div className="grid gap-4 lg:grid-cols-2">
                <FacultySection
                    faculties={shownFaculties}
                    onChanged={reload}
                    onNotice={setNotice}
                />
                <ClassSection
                    classes={shownClasses}
                    faculties={faculties}
                    onChanged={reload}
                    onNotice={setNotice}
                />
            </div>
        </div>
    );
}

type SectionProps = {
    onChanged: () => Promise<void>;
    onNotice: (n: Feedback) => void;
};

function FacultySection({ faculties, onChanged, onNotice }: SectionProps & { faculties: FacultyRow[] }) {
    const codeId = useId();
    const nameId = useId();
    const [editing, setEditing] = useState<FacultyRow | null>(null);
    const [code, setCode] = useState("");
    const [name, setName] = useState("");
    const [busy, setBusy] = useState(false);

    function reset() {
        setEditing(null);
        setCode("");
        setName("");
    }

    function edit(row: FacultyRow) {
        setEditing(row);
        setCode(row.code);
        setName(row.name);
    }

    async function submit() {
        if (!code.trim() || !name.trim()) {
            onNotice({ text: "Nhập mã khoa và tên khoa.", tone: "error" });
            return;
        }
        setBusy(true);
        try {
            await saveFaculty(editing?.id ?? null, { code: code.trim(), name: name.trim() });
            onNotice({ text: editing ? `Đã cập nhật khoa ${code.trim()}.` : `Đã thêm khoa ${code.trim()}.`, tone: "info" });
            reset();
            await onChanged();
        } catch (err) {
            onNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function remove(row: FacultyRow) {
        if (!window.confirm(`Xóa khoa ${row.code} - ${row.name}?`)) return;
        try {
            await deleteFaculty(row.id);
            onNotice({ text: `Đã xóa khoa ${row.code}.`, tone: "info" });
            if (editing?.id === row.id) reset();
            await onChanged();
        } catch (err) {
            onNotice({ text: adminErrorMessage(err), tone: "error" });
        }
    }

    return (
        <section className={CARD} aria-label="Danh mục khoa">
            <h4 className="text-sm font-semibold">Khoa ({faculties.length})</h4>

            <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor={codeId} className={LABEL}>Mã khoa</label>
                    <input id={codeId} value={code} onChange={(e) => setCode(e.target.value)} placeholder="Ví dụ: CNTT" className={INPUT} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={nameId} className={LABEL}>Tên khoa</label>
                    <input id={nameId} value={name} onChange={(e) => setName(e.target.value)} placeholder="Công nghệ thông tin" className={INPUT} />
                </div>
            </div>
            <div className="flex flex-wrap gap-2">
                <button type="button" disabled={busy} onClick={submit} className={BTN_PRIMARY}>
                    {editing ? "Lưu thay đổi" : "Thêm khoa"}
                </button>
                {editing ? (
                    <button type="button" onClick={reset} className={BTN_GHOST}>Hủy sửa</button>
                ) : null}
            </div>

            <div className="max-h-80 overflow-auto">
                <table className="w-full text-left text-sm">
                    <thead className="text-xs text-muted">
                    <tr>
                        <th className="py-1 pr-2 font-medium">Mã</th>
                        <th className="py-1 pr-2 font-medium">Tên</th>
                        <th className="py-1 pr-2 font-medium">Số lớp</th>
                        <th className="py-1 font-medium"><span className="sr-only">Thao tác</span></th>
                    </tr>
                    </thead>
                    <tbody>
                    {faculties.map((row) => (
                        <tr key={row.id} className="border-t border-line">
                            <td className="py-1 pr-2 font-mono">{row.code}</td>
                            <td className="py-1 pr-2">{row.name}</td>
                            <td className="py-1 pr-2">{row.classCount}</td>
                            <td className="py-1 whitespace-nowrap">
                                <button type="button" onClick={() => edit(row)} className={BTN_GHOST}>Sửa</button>{" "}
                                <button type="button" onClick={() => remove(row)} className={BTN_GHOST}>Xóa</button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}

function ClassSection({
                          classes,
                          faculties,
                          onChanged,
                          onNotice,
                      }: SectionProps & { classes: ClassRow[]; faculties: FacultyRow[] }) {
    const codeId = useId();
    const nameId = useId();
    const facultyId = useId();
    const cohortId = useId();
    const [editing, setEditing] = useState<ClassRow | null>(null);
    const [code, setCode] = useState("");
    const [name, setName] = useState("");
    const [faculty, setFaculty] = useState("");
    const [cohort, setCohort] = useState("");
    const [busy, setBusy] = useState(false);

    function reset() {
        setEditing(null);
        setCode("");
        setName("");
        setFaculty("");
        setCohort("");
    }

    function edit(row: ClassRow) {
        setEditing(row);
        setCode(row.code);
        setName(row.name);
        setFaculty(String(row.facultyId));
        setCohort(row.cohort ?? "");
    }

    async function submit() {
        if (!code.trim() || !name.trim() || !faculty) {
            onNotice({ text: "Nhập mã lớp, tên lớp và chọn khoa.", tone: "error" });
            return;
        }
        setBusy(true);
        try {
            await saveClass(editing?.id ?? null, {
                code: code.trim(),
                name: name.trim(),
                facultyId: Number(faculty),
                cohort: cohort.trim() || null,
            });
            onNotice({ text: editing ? `Đã cập nhật lớp ${code.trim()}.` : `Đã thêm lớp ${code.trim()}.`, tone: "info" });
            reset();
            await onChanged();
        } catch (err) {
            onNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function remove(row: ClassRow) {
        if (!window.confirm(`Xóa lớp ${row.code} - ${row.name}?`)) return;
        try {
            await deleteClass(row.id);
            onNotice({ text: `Đã xóa lớp ${row.code}.`, tone: "info" });
            if (editing?.id === row.id) reset();
            await onChanged();
        } catch (err) {
            onNotice({ text: adminErrorMessage(err), tone: "error" });
        }
    }

    return (
        <section className={CARD} aria-label="Danh mục lớp">
            <h4 className="text-sm font-semibold">Lớp ({classes.length})</h4>

            {faculties.length === 0 ? (
                <p className="text-xs text-muted">Chưa có khoa nào. Thêm khoa trước khi thêm lớp.</p>
            ) : null}

            <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor={codeId} className={LABEL}>Mã lớp</label>
                    <input
                        id={codeId}
                        value={code}
                        disabled={editing !== null}
                        onChange={(e) => setCode(e.target.value)}
                        placeholder="Ví dụ: DCT1222"
                        className={INPUT}
                    />
                    {editing ? <span className="text-xs text-muted">Mã lớp không đổi sau khi tạo.</span> : null}
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={nameId} className={LABEL}>Tên lớp</label>
                    <input id={nameId} value={name} onChange={(e) => setName(e.target.value)} placeholder="Khoa học máy tính K22" className={INPUT} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={facultyId} className={LABEL}>Khoa</label>
                    <select id={facultyId} value={faculty} onChange={(e) => setFaculty(e.target.value)} className={INPUT}>
                        <option value="">-- Chọn khoa --</option>
                        {faculties.map((f) => (
                            <option key={f.id} value={f.id}>{f.code} - {f.name}</option>
                        ))}
                    </select>
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={cohortId} className={LABEL}>Niên khóa</label>
                    <input id={cohortId} value={cohort} onChange={(e) => setCohort(e.target.value)} placeholder="Ví dụ: K22" className={INPUT} />
                </div>
            </div>
            <div className="flex flex-wrap gap-2">
                <button type="button" disabled={busy} onClick={submit} className={BTN_PRIMARY}>
                    {editing ? "Lưu thay đổi" : "Thêm lớp"}
                </button>
                {editing ? (
                    <button type="button" onClick={reset} className={BTN_GHOST}>Hủy sửa</button>
                ) : null}
            </div>

            <div className="max-h-80 overflow-auto">
                <table className="w-full text-left text-sm">
                    <thead className="text-xs text-muted">
                    <tr>
                        <th className="py-1 pr-2 font-medium">Mã</th>
                        <th className="py-1 pr-2 font-medium">Tên</th>
                        <th className="py-1 pr-2 font-medium">Khoa</th>
                        <th className="py-1 pr-2 font-medium">SV</th>
                        <th className="py-1 font-medium"><span className="sr-only">Thao tác</span></th>
                    </tr>
                    </thead>
                    <tbody>
                    {classes.map((row) => (
                        <tr key={row.id} className="border-t border-line">
                            <td className="py-1 pr-2 font-mono">{row.code}</td>
                            <td className="py-1 pr-2">{row.name}</td>
                            <td className="py-1 pr-2 font-mono">{row.facultyCode}</td>
                            <td className="py-1 pr-2">{row.studentCount}</td>
                            <td className="py-1 whitespace-nowrap">
                                <button type="button" onClick={() => edit(row)} className={BTN_GHOST}>Sửa</button>{" "}
                                <button type="button" onClick={() => remove(row)} className={BTN_GHOST}>Xóa</button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}