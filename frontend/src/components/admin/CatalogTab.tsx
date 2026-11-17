"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { createClass, createFaculty, listClasses, listFaculties } from "@/lib/admin/catalog-api";
import type { Faculty, SchoolClass } from "@/lib/admin/catalog-api";
import { BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";
import { EntityPicker } from "./EntityPicker";
import type { PickerOption } from "./EntityPicker";
import { searchFaculties, searchPeople } from "./pickers";

const FACULTY_CODE = /^[A-Za-z0-9_]{2,16}$/;
const CLASS_CODE = /^[A-Za-z0-9_.-]{2,32}$/;
const COHORT = /^(K\d{2})?$/i;
const SEARCH_DELAY_MS = 250;

type Feedback = { text: string; tone: "info" | "error" };

/** Chỉ hiển thị cho ROOT. Server vẫn kiểm tra lại quyền ở mọi thao tác. */
export function CatalogTab() {
    const [faculties, setFaculties] = useState<Faculty[]>([]);
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [facultyQ, setFacultyQ] = useState("");
    const [classQ, setClassQ] = useState("");
    const [reloadKey, setReloadKey] = useState(0);
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    const [fCode, setFCode] = useState("");
    const [fName, setFName] = useState("");

    const [cCode, setCCode] = useState("");
    const [cName, setCName] = useState("");
    const [cFaculty, setCFaculty] = useState<PickerOption | null>(null);
    const [cCohort, setCCohort] = useState("");
    const [cAdvisor, setCAdvisor] = useState<PickerOption | null>(null);

    useEffect(() => {
        let alive = true;
        const timer = window.setTimeout(() => {
            listFaculties(facultyQ.trim())
                .then((data) => {
                    if (alive) setFaculties(data);
                })
                .catch((err: unknown) => {
                    if (alive) setNotice({ text: adminErrorMessage(err), tone: "error" });
                });
        }, SEARCH_DELAY_MS);
        return () => {
            alive = false;
            window.clearTimeout(timer);
        };
    }, [facultyQ, reloadKey]);

    useEffect(() => {
        let alive = true;
        const timer = window.setTimeout(() => {
            listClasses(classQ.trim(), null)
                .then((data) => {
                    if (alive) setClasses(data);
                })
                .catch((err: unknown) => {
                    if (alive) setNotice({ text: adminErrorMessage(err), tone: "error" });
                });
        }, SEARCH_DELAY_MS);
        return () => {
            alive = false;
            window.clearTimeout(timer);
        };
    }, [classQ, reloadKey]);

    async function submitFaculty(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const code = fCode.trim();
        const name = fName.trim();
        if (!FACULTY_CODE.test(code)) {
            setNotice({ text: "Mã khoa gồm chữ, số hoặc gạch dưới, từ 2 đến 16 ký tự.", tone: "error" });
            return;
        }
        if (!name) {
            setNotice({ text: "Nhập tên khoa.", tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await createFaculty(code.toUpperCase(), name);
            setFCode("");
            setFName("");
            setNotice({ text: `Đã thêm khoa ${name}.`, tone: "info" });
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function submitClass(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const code = cCode.trim();
        const name = cName.trim();
        if (!CLASS_CODE.test(code)) {
            setNotice({ text: "Mã lớp gồm chữ, số, dấu chấm, gạch ngang hoặc gạch dưới, từ 2 đến 32 ký tự.", tone: "error" });
            return;
        }
        if (!name) {
            setNotice({ text: "Nhập tên lớp.", tone: "error" });
            return;
        }
        if (!cFaculty) {
            setNotice({ text: "Chọn khoa quản lý lớp.", tone: "error" });
            return;
        }
        if (!COHORT.test(cCohort.trim())) {
            setNotice({ text: "Niên khóa có dạng K23, hoặc để trống.", tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await createClass({
                code: code.toUpperCase(),
                name,
                facultyId: cFaculty.id,
                cohort: cCohort.trim().toUpperCase(),
                advisorId: cAdvisor ? cAdvisor.id : null,
            });
            setCCode("");
            setCName("");
            setCCohort("");
            setCAdvisor(null);
            setNotice({ text: `Đã thêm lớp ${name}.`, tone: "info" });
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <section aria-label="Khoa và lớp" className="flex flex-col gap-6">
            <h2 className="font-serif text-lg font-semibold">Khoa và lớp</h2>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <div className="grid gap-4 lg:grid-cols-2">
                <form onSubmit={submitFaculty} className={CARD}>
                    <h3 className="text-sm font-semibold">Thêm khoa</h3>
                    <label htmlFor="faculty-code" className={LABEL}>Mã khoa</label>
                    <input id="faculty-code" value={fCode} onChange={(e) => setFCode(e.target.value)} placeholder="Ví dụ: CNTT" className={INPUT} />
                    <label htmlFor="faculty-name" className={LABEL}>Tên khoa</label>
                    <input id="faculty-name" value={fName} onChange={(e) => setFName(e.target.value)} className={INPUT} />
                    <button type="submit" disabled={busy} className={`${BTN_PRIMARY} self-start`}>Thêm khoa</button>
                </form>

                <form onSubmit={submitClass} className={CARD}>
                    <h3 className="text-sm font-semibold">Thêm lớp</h3>
                    <label htmlFor="class-code" className={LABEL}>Mã lớp</label>
                    <input id="class-code" value={cCode} onChange={(e) => setCCode(e.target.value)} placeholder="Ví dụ: DH23TIN01" className={INPUT} />
                    <label htmlFor="class-name" className={LABEL}>Tên lớp</label>
                    <input id="class-name" value={cName} onChange={(e) => setCName(e.target.value)} className={INPUT} />
                    <EntityPicker
                        id="class-faculty"
                        label="Khoa"
                        placeholder="Nhập tên hoặc mã khoa"
                        search={searchFaculties}
                        value={cFaculty}
                        onChange={setCFaculty}
                    />
                    <label htmlFor="class-cohort" className={LABEL}>Niên khóa (không bắt buộc)</label>
                    <input id="class-cohort" value={cCohort} onChange={(e) => setCCohort(e.target.value)} placeholder="Ví dụ: K23" className={INPUT} />
                    <EntityPicker
                        id="class-advisor"
                        label="Cố vấn học tập (không bắt buộc)"
                        placeholder="Nhập tên hoặc tên đăng nhập"
                        search={searchPeople}
                        value={cAdvisor}
                        onChange={setCAdvisor}
                    />
                    <button type="submit" disabled={busy} className={`${BTN_PRIMARY} self-start`}>Thêm lớp</button>
                </form>
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
                <div className="flex flex-col gap-3">
                    <h3 className="text-sm font-semibold">Danh sách khoa</h3>
                    <input
                        aria-label="Tìm khoa"
                        value={facultyQ}
                        onChange={(e) => setFacultyQ(e.target.value)}
                        placeholder="Tìm theo tên hoặc mã khoa"
                        className={INPUT}
                    />
                    {faculties.length === 0 ? <p className="text-sm text-muted">Chưa có khoa nào.</p> : null}
                    <ul className="flex flex-col divide-y divide-line overflow-hidden rounded-lg border border-line bg-surface">
                        {faculties.map((faculty) => (
                            <li key={faculty.id} className="px-4 py-2 text-sm">
                                <span className="font-medium">{faculty.name}</span>
                                <span className="ml-2 text-xs text-muted">{faculty.code} · #{faculty.id}</span>
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="flex flex-col gap-3">
                    <h3 className="text-sm font-semibold">Danh sách lớp</h3>
                    <input
                        aria-label="Tìm lớp"
                        value={classQ}
                        onChange={(e) => setClassQ(e.target.value)}
                        placeholder="Tìm theo tên hoặc mã lớp"
                        className={INPUT}
                    />
                    {classes.length === 0 ? <p className="text-sm text-muted">Chưa có lớp nào.</p> : null}
                    <ul className="flex flex-col divide-y divide-line overflow-hidden rounded-lg border border-line bg-surface">
                        {classes.map((item) => (
                            <li key={item.id} className="flex flex-col px-4 py-2 text-sm">
                                <span className="font-medium">{item.name}</span>
                                <span className="text-xs text-muted">
                                    {item.code} · {item.facultyName}
                                    {item.cohort ? ` · ${item.cohort}` : ""}
                                    {item.advisorId ? ` · Cố vấn #${item.advisorId}` : ""}
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>
            </div>
        </section>
    );
}