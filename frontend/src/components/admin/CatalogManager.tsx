"use client";

import { useEffect, useId, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { notifyAcademicChanged, onAcademicChanged } from "@/lib/admin/refresh";
import {
    deleteClass,
    deleteFaculty,
    listClasses,
    listFaculties,
    listLecturers,
    saveClass,
    saveFaculty,
} from "@/lib/admin/academic-api";
import type { ClassRow, FacultyRow, LecturerOption } from "@/lib/admin/academic-api";
import { buttonStyle, CARD, FIELD, HELP, LABEL, SECTION_TITLE, SELECT, TABLE, TABLE_WRAP, TD, TH } from "@/components/ui/styles";

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
        return onAcademicChanged(() => void reload());
    }, []);

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <h3 className={SECTION_TITLE}>Quản lý khoa và lớp</h3>
                <p className={HELP}>
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
                        className={FIELD}
                    />
                </div>
                <button type="submit" className={buttonStyle("secondary")}>Tìm</button>
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
            <h4 className={SECTION_TITLE}>Khoa ({faculties.length})</h4>

            <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor={codeId} className={LABEL}>Mã khoa</label>
                    <input id={codeId} value={code} onChange={(e) => setCode(e.target.value)} placeholder="Ví dụ: CNTT" className={FIELD} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={nameId} className={LABEL}>Tên khoa</label>
                    <input id={nameId} value={name} onChange={(e) => setName(e.target.value)} placeholder="Công nghệ thông tin" className={FIELD} />
                </div>
            </div>
            <div className="flex flex-wrap gap-2">
                <button type="button" disabled={busy} onClick={submit} className={buttonStyle("primary")}>
                    {editing ? "Lưu thay đổi" : "Thêm khoa"}
                </button>
                {editing ? (
                    <button type="button" onClick={reset} className={buttonStyle("secondary")}>Hủy sửa</button>
                ) : null}
            </div>

            <div className={`${TABLE_WRAP} max-h-80 overflow-y-auto`}>
                <table className={TABLE}>
                    <thead>
                    <tr>
                        <th className={TH}>Mã</th>
                        <th className={TH}>Tên</th>
                        <th className={`${TH} text-right`}>Số lớp</th>
                        <th className={TH}><span className="sr-only">Thao tác</span></th>
                    </tr>
                    </thead>
                    <tbody>
                    {faculties.map((row) => (
                        <tr key={row.id}>
                            <td className={`${TD} font-mono`}>{row.code}</td>
                            <td className={TD}>{row.name}</td>
                            <td className={`${TD} text-right`}>{row.classCount}</td>
                            <td className={`${TD} whitespace-nowrap`}>
                                <button type="button" onClick={() => edit(row)} className={buttonStyle("secondary", "sm")}>Sửa</button>{" "}
                                <button type="button" onClick={() => remove(row)} className={buttonStyle("danger", "sm")}>Xóa</button>
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
    const advisorId = useId();
    const [editing, setEditing] = useState<ClassRow | null>(null);
    const [code, setCode] = useState("");
    const [name, setName] = useState("");
    const [faculty, setFaculty] = useState("");
    const [cohort, setCohort] = useState("");
    const [advisor, setAdvisor] = useState("");
    const [lecturers, setLecturers] = useState<LecturerOption[]>([]);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        listLecturers()
            .then(setLecturers)
            .catch((err) => onNotice({ text: adminErrorMessage(err), tone: "error" }));
    }, []);

    function reset() {
        setEditing(null);
        setCode("");
        setName("");
        setFaculty("");
        setCohort("");
        setAdvisor("");
    }

    function edit(row: ClassRow) {
        setEditing(row);
        setCode(row.code);
        setName(row.name);
        setFaculty(String(row.facultyId));
        setCohort(row.cohort ?? "");
        setAdvisor(row.advisorId === null ? "" : String(row.advisorId));
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
                advisorId: advisor ? Number(advisor) : null,
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
            <h4 className={SECTION_TITLE}>Lớp ({classes.length})</h4>

            {faculties.length === 0 ? (
                <p className={HELP}>Chưa có khoa nào. Thêm khoa trước khi thêm lớp.</p>
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
                        className={FIELD}
                    />
                    {editing ? <span className={HELP}>Mã lớp không đổi sau khi tạo.</span> : null}
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={nameId} className={LABEL}>Tên lớp</label>
                    <input id={nameId} value={name} onChange={(e) => setName(e.target.value)} placeholder="Khoa học máy tính K22" className={FIELD} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={facultyId} className={LABEL}>Khoa</label>
                    <select id={facultyId} value={faculty} onChange={(e) => setFaculty(e.target.value)} className={SELECT}>
                        <option value="">-- Chọn khoa --</option>
                        {faculties.map((f) => (
                            <option key={f.id} value={f.id}>{f.code} - {f.name}</option>
                        ))}
                    </select>
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={cohortId} className={LABEL}>Niên khóa</label>
                    <input id={cohortId} value={cohort} onChange={(e) => setCohort(e.target.value)} placeholder="Ví dụ: K22" className={FIELD} />
                </div>
                <div className="flex flex-col gap-1 sm:col-span-2">
                    <label htmlFor={advisorId} className={LABEL}>Giảng viên chủ nhiệm</label>
                    <select id={advisorId} value={advisor} onChange={(e) => setAdvisor(e.target.value)} className={SELECT}>
                        <option value="">-- Chưa gán --</option>
                        {lecturers.map((l) => (
                            <option key={l.id} value={l.id}>{l.username} - {l.name}</option>
                        ))}
                    </select>
                    <span className={HELP}>Mỗi lớp có tối đa một giảng viên chủ nhiệm.</span>
                </div>
            </div>
            <div className="flex flex-wrap gap-2">
                <button type="button" disabled={busy} onClick={submit} className={buttonStyle("primary")}>
                    {editing ? "Lưu thay đổi" : "Thêm lớp"}
                </button>
                {editing ? (
                    <button type="button" onClick={reset} className={buttonStyle("secondary")}>Hủy sửa</button>
                ) : null}
            </div>

            <div className={`${TABLE_WRAP} max-h-80 overflow-y-auto`}>
                <table className={TABLE}>
                    <thead>
                    <tr>
                        <th className={TH}>Mã</th>
                        <th className={TH}>Tên</th>
                        <th className={TH}>Khoa</th>
                        <th className={TH}>GVCN</th>
                        <th className={`${TH} text-right`}>SV</th>
                        <th className={TH}><span className="sr-only">Thao tác</span></th>
                    </tr>
                    </thead>
                    <tbody>
                    {classes.map((row) => (
                        <tr key={row.id}>
                            <td className={`${TD} font-mono`}>{row.code}</td>
                            <td className={TD}>{row.name}</td>
                            <td className={`${TD} font-mono`}>{row.facultyCode}</td>
                            <td className={TD}>{row.advisorName ?? "—"}</td>
                            <td className={`${TD} text-right`}>{row.studentCount}</td>
                            <td className={`${TD} whitespace-nowrap`}>
                                <button type="button" onClick={() => edit(row)} className={buttonStyle("secondary", "sm")}>Sửa</button>{" "}
                                <button type="button" onClick={() => remove(row)} className={buttonStyle("danger", "sm")}>Xóa</button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}