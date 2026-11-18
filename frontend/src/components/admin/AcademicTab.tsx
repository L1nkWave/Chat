"use client";

import { useId, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { importAcademicFile, saveAcademicTerm } from "@/lib/admin/academic-api";
import type { CreatedAccount, ImportKind, ImportReport } from "@/lib/admin/academic-api";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";
import { CatalogManager } from "./CatalogManager";
import { RecordsTable } from "./RecordsTable";
import { ManualEntryForm } from "./ManualEntryForm";
import type { RecordSpec } from "./RecordsTable";

const MAX_SHOWN_ERRORS = 50;

type Feedback = { text: string; tone: "info" | "error" };

export type CardSpec = {
    kind: ImportKind;
    title: string;
    columns: string;
    note?: string;
};

export const FACULTY_CARD: CardSpec = {
    kind: "faculties",
    title: "Khoa",
    columns: "Mã khoa, Tên khoa",
    note: "Bước 1 của luồng nhập: khoa trước, rồi đến lớp. Nhập lại file cũ sẽ cập nhật tên khoa đã có.",
};

export const CLASS_CARD: CardSpec = {
    kind: "classes",
    title: "Lớp",
    columns: "Mã lớp, Tên lớp, Mã khoa, Niên khóa (không bắt buộc)",
    note: "Bước 2: mã khoa phải đã có trong mục Khoa.",
};

export const SUBJECT_CARD: CardSpec = {
    kind: "subjects",
    title: "Môn học",
    columns: "Mã MH, Tên môn học",
    note: "Bước 3: danh mục môn. Môn đã có trong TKB hoặc lịch thi vẫn nhập lại được, dòng cũ sẽ được cập nhật tên.",
};

export const LECTURER_CARD: CardSpec = {
    kind: "lecturers",
    title: "Giảng viên",
    columns: "Mã GV, Họ tên",
    note: "Mã GV là tên đăng nhập. Mật khẩu mặc định là Dhsg@123 và bắt buộc đổi ở lần đăng nhập đầu. Họ tên phải khớp cột GV trên thời khóa biểu.",
};

export const STUDENT_CARD: CardSpec = {
    kind: "students",
    title: "Sinh viên",
    columns: "MSSV, Họ tên, Lớp, CCCD (không bắt buộc)",
    note: "Lớp trong file phải đã có ở mục Khoa & lớp. Tạo tài khoản mới với mật khẩu tạm. Nếu tài khoản đã có thì chỉ cập nhật họ tên, lớp và CCCD. CCCD được mã hóa khi lưu.",
};

const TIMETABLE_CARD: CardSpec = {
    kind: "timetable",
    title: "Thời khóa biểu",
    columns: "Mã MH, Môn, Nhóm, Phòng, Thứ, Tiết, Số tiết, GV, Lớp, Ngày, Giờ bắt đầu",
    note: "Mỗi dòng là một buổi học. Cột GV là họ tên giảng viên. Phòng có phần mô tả sau dấu \"-\" thì chỉ lấy mã phòng.",
};

const EXAM_CARD: CardSpec = {
    kind: "exams",
    title: "Lịch thi",
    columns: "STT, Mã MH, Tên môn học, Sĩ số, Ngày thi, Giờ bắt đầu, Phòng thi, Cơ sở",
    note: "Đúng định dạng file lịch thi của trường. Các cột STT và cột khác không dùng đến sẽ bị bỏ qua.",
};

const DUTY_CARD: CardSpec = {
    kind: "duties",
    title: "Lịch gác thi",
    columns: "Mã GV, Mã MH, Ngày, Giờ thi, Phòng, Cơ sở",
    note: "Mã GV là tên đăng nhập của giảng viên.",
};

const SUBJECT_RECORDS: RecordSpec = {
    kind: "subjects",
    title: "Môn học",
    canCreate: true,
    fields: [
        { name: "code", label: "Mã MH", type: "text", editable: false },
        { name: "name", label: "Tên môn học", type: "text", editable: true },
    ],
};

const TIMETABLE_RECORDS: RecordSpec = {
    kind: "timetable",
    title: "Buổi học",
    fields: [
        { name: "class_code", label: "Lớp", type: "text", editable: false },
        { name: "subject_code", label: "Mã MH", type: "text", editable: false },
        { name: "subject_name", label: "Môn", type: "text", editable: true },
        { name: "group_code", label: "Nhóm", type: "text", editable: false },
        { name: "room", label: "Phòng", type: "text", editable: true },
        { name: "weekday", label: "Thứ", type: "number", editable: false },
        { name: "start_period", label: "Tiết", type: "number", editable: false },
        { name: "period_count", label: "Số tiết", type: "number", editable: false },
        { name: "lecturer_code", label: "Mã GV", type: "text", editable: true },
        { name: "lecturer_name", label: "GV", type: "text", editable: true },
        { name: "session_date", label: "Ngày", type: "date", editable: true },
        { name: "start_time", label: "Giờ", type: "time", editable: true },
    ],
};

const EXAM_RECORDS: RecordSpec = {
    kind: "exams",
    title: "Lịch thi",
    fields: [
        { name: "subject_code", label: "Mã MH", type: "text", editable: false },
        { name: "subject_name", label: "Môn", type: "text", editable: true },
        { name: "candidate_count", label: "Sĩ số", type: "number", editable: true },
        { name: "exam_date", label: "Ngày thi", type: "date", editable: true },
        { name: "exam_time", label: "Giờ", type: "time", editable: true },
        { name: "room", label: "Phòng", type: "text", editable: true },
        { name: "campus", label: "Cơ sở", type: "text", editable: true },
    ],
};

const DUTY_RECORDS: RecordSpec = {
    kind: "duties",
    title: "Lịch gác thi",
    fields: [
        { name: "lecturer_code", label: "Mã GV", type: "text", editable: true },
        { name: "subject_code", label: "Mã MH", type: "text", editable: true },
        { name: "exam_date", label: "Ngày", type: "date", editable: true },
        { name: "exam_time", label: "Giờ", type: "time", editable: true },
        { name: "room", label: "Phòng", type: "text", editable: true },
        { name: "campus", label: "Cơ sở", type: "text", editable: true },
    ],
};

const TERM_RECORDS: RecordSpec = {
    kind: "terms",
    title: "Học kỳ",
    fields: [
        { name: "code", label: "Mã kỳ", type: "text", editable: false },
        { name: "name", label: "Tên học kỳ", type: "text", editable: true },
        { name: "start_date", label: "Bắt đầu", type: "date", editable: true },
        { name: "end_date", label: "Kết thúc", type: "date", editable: true },
    ],
};

type SubTab = "structure" | "subjects" | "schedule" | "terms";

const SUB_TABS: { id: SubTab; label: string; hint: string }[] = [
    { id: "structure", label: "Khoa & lớp", hint: "Danh mục khoa và lớp. Nhập trước sinh viên và lịch học." },
    { id: "subjects", label: "Môn học", hint: "Danh mục môn học. Nhập trước thời khóa biểu và lịch thi." },
    { id: "schedule", label: "Lịch học & thi", hint: "Thời khóa biểu, lịch thi và lịch gác thi. Sinh viên và giảng viên nhập ở tab Người dùng." },
    { id: "terms", label: "Học kỳ", hint: "Khai báo học kỳ. Khoảng ngày của hai học kỳ không được trùng nhau." },
];

/** Trang Học vụ của quản trị. Các mục xếp theo thứ tự nhập: khoa -> lớp -> môn -> sinh viên, giảng viên -> lịch. */
export function AcademicTab() {
    const [tab, setTab] = useState<SubTab>("structure");
    const current = SUB_TABS.find((t) => t.id === tab) ?? SUB_TABS[0];

    return (
        <section aria-label="Dữ liệu học vụ" className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <h2 className="font-serif text-lg font-semibold">Dữ liệu học vụ</h2>
                <p className="text-sm text-muted">
                    Kiểm tra file trước khi nhập. Nếu file còn dòng lỗi thì hệ thống không ghi dòng nào; sửa file rồi kiểm tra lại.
                </p>
            </div>

            <div role="tablist" aria-label="Mục học vụ" className="flex flex-wrap gap-2">
                {SUB_TABS.map((t) => (
                    <button
                        key={t.id}
                        type="button"
                        role="tab"
                        aria-selected={tab === t.id}
                        onClick={() => setTab(t.id)}
                        className={tab === t.id ? BTN_PRIMARY : BTN_GHOST}
                    >
                        {t.label}
                    </button>
                ))}
            </div>
            <p className="text-xs text-muted">{current.hint}</p>

            {tab === "structure" ? (
                <div className="flex flex-col gap-4">
                    <div className="grid gap-4 lg:grid-cols-2">
                        <ImportCard spec={FACULTY_CARD} />
                        <ImportCard spec={CLASS_CARD} />
                    </div>
                    <CatalogManager />
                </div>
            ) : null}

            {tab === "subjects" ? (
                <div className="flex flex-col gap-4">
                    <div className="grid gap-4 lg:grid-cols-2">
                        <ImportCard spec={SUBJECT_CARD} />
                    </div>
                    <RecordsTable spec={SUBJECT_RECORDS} />
                </div>
            ) : null}

            {tab === "schedule" ? (
                <div className="flex flex-col gap-4">
                    <div className="grid gap-4 lg:grid-cols-3">
                        <ImportCard spec={TIMETABLE_CARD} />
                        <ImportCard spec={EXAM_CARD} />
                        <ImportCard spec={DUTY_CARD} />
                    </div>
                    <ManualEntryForm
                        kind="timetable"
                        title="Buổi học"
                        hint="Một dòng là một buổi học. GV là họ tên giảng viên, phải khớp tài khoản. Môn phải có ở mục Môn học."
                        fields={[
                            { name: "Mã MH" },
                            { name: "Môn", optional: true },
                            { name: "Nhóm", optional: true },
                            { name: "Phòng", placeholder: "Ví dụ: C.E603" },
                            { name: "Thứ", placeholder: "2 đến 7, hoặc CN" },
                            { name: "Tiết", placeholder: "1 đến 14" },
                            { name: "Số tiết", placeholder: "Ví dụ: 3" },
                            { name: "GV", optional: true, placeholder: "Họ tên giảng viên" },
                            { name: "Mã GV", optional: true },
                            { name: "Lớp", placeholder: "Ví dụ: DCT1222" },
                            { name: "Ngày", placeholder: "dd/mm/yyyy" },
                            { name: "Giờ bắt đầu", placeholder: "HH:mm" },
                        ]}
                    />
                    <ManualEntryForm
                        kind="exams"
                        title="Lịch thi"
                        hint="Một dòng là một ca thi. Mã MH phải có ở mục Môn học."
                        fields={[
                            { name: "Mã MH" },
                            { name: "Tên môn học", optional: true },
                            { name: "Sĩ số", optional: true },
                            { name: "Ngày thi", placeholder: "dd/mm/yyyy" },
                            { name: "Giờ bắt đầu", placeholder: "HH:mm" },
                            { name: "Phòng thi", placeholder: "Ví dụ: C.E603" },
                            { name: "Cơ sở", optional: true },
                        ]}
                    />
                    <ManualEntryForm
                        kind="duties"
                        title="Lịch gác thi"
                        hint="Mã GV là tên đăng nhập của giảng viên."
                        fields={[
                            { name: "Mã GV" },
                            { name: "Mã MH", optional: true },
                            { name: "Ngày", placeholder: "dd/mm/yyyy" },
                            { name: "Giờ thi", placeholder: "HH:mm" },
                            { name: "Phòng" },
                            { name: "Cơ sở", optional: true },
                        ]}
                    />
                    <RecordsTable spec={TIMETABLE_RECORDS} />
                    <RecordsTable spec={EXAM_RECORDS} />
                    <RecordsTable spec={DUTY_RECORDS} />
                </div>
            ) : null}

            {tab === "terms" ? (
                <div className="flex flex-col gap-4">
                    <TermCard />
                    <RecordsTable spec={TERM_RECORDS} />
                </div>
            ) : null}
        </section>
    );
}

/** Thẻ nhập Excel. Dùng chung cho tab Học vụ và tab Người dùng. */
export function ImportCard({ spec }: { spec: CardSpec }) {
    const inputId = useId();
    const [file, setFile] = useState<File | null>(null);
    const [report, setReport] = useState<ImportReport | null>(null);
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    // Chỉ cho nhập khi đã kiểm tra đúng file này và không còn lỗi
    const canImport = file !== null && report !== null && report.dryRun && report.errors.length === 0 && report.valid > 0 && !busy;

    function pick(next: File | null) {
        setFile(next);
        setReport(null);
        setNotice(null);
    }

    async function run(dryRun: boolean) {
        if (!file) return;
        if (!dryRun && !window.confirm(`Nhập ${file.name} vào mục ${spec.title.toLowerCase()}?`)) return;
        setBusy(true);
        setNotice(null);
        try {
            const result = await importAcademicFile(spec.kind, file, dryRun);
            setReport(result);
            if (!dryRun) {
                setNotice(
                    result.written > 0
                        ? { text: successText(spec, result), tone: "info" }
                        : { text: "Chưa ghi dòng nào vì file còn lỗi. Sửa file rồi kiểm tra lại.", tone: "error" },
                );
            }
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <section className={CARD} aria-label={`Nhập ${spec.title}`}>
            <div className="flex flex-col gap-1">
                <h3 className="text-base font-semibold">{spec.title}</h3>
                <p className="text-xs text-muted">Cột trong file: {spec.columns}</p>
                {spec.note ? <p className="text-xs text-muted">{spec.note}</p> : null}
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor={inputId} className={LABEL}>
                    File .xlsx
                </label>
                <input
                    id={inputId}
                    type="file"
                    accept=".xlsx"
                    disabled={busy}
                    onChange={(event) => pick(event.target.files?.[0] ?? null)}
                    className="text-sm"
                />
            </div>

            <div className="flex flex-wrap gap-2">
                <button type="button" disabled={!file || busy} onClick={() => run(true)} className={BTN_GHOST}>
                    Kiểm tra file
                </button>
                <button type="button" disabled={!canImport} onClick={() => run(false)} className={BTN_PRIMARY}>
                    Nhập vào hệ thống
                </button>
            </div>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
            {report ? <ReportView report={report} /> : null}
            {report && report.createdAccounts.length > 0 ? <AccountsView accounts={report.createdAccounts} /> : null}
        </section>
    );
}

/** Luôn nói rõ số dòng thêm mới và số dòng cập nhật, để nhập lại file cũ không bị hiểu nhầm là không ghi */
function successText(spec: CardSpec, result: ImportReport): string {
    const base = `Đã xử lý ${result.written} dòng vào mục ${spec.title.toLowerCase()}: thêm mới ${result.inserted}, cập nhật ${result.updated}.`;
    if (spec.kind === "students") {
        return `${base} Tạo ${result.createdAccounts.length} tài khoản sinh viên mới.`;
    }
    if (spec.kind === "lecturers") {
        return `${base} Tạo ${result.createdAccounts.length} tài khoản giảng viên mới.`;
    }
    return base;
}

function ReportView({ report }: { report: ImportReport }) {
    const shown = report.errors.slice(0, MAX_SHOWN_ERRORS);
    return (
        <div className="flex flex-col gap-2 text-sm">
            <p>
                {report.dryRun ? "Kết quả kiểm tra" : "Kết quả nhập"}: {report.total} dòng, hợp lệ {report.valid}
                {report.dryRun ? "" : `, đã ghi ${report.written} (thêm mới ${report.inserted}, cập nhật ${report.updated})`}.
            </p>
            {report.errors.length > 0 ? (
                <div className="flex flex-col gap-1">
                    <p className="font-medium text-primary">
                        Còn {report.errors.length} lỗi. Sửa các dòng dưới đây rồi kiểm tra lại.
                    </p>
                    <ul className="flex max-h-60 flex-col gap-1 overflow-auto rounded-md border border-line bg-paper p-2 text-xs">
                        {shown.map((item, index) => (
                            <li key={`${item.row}-${index}`}>
                                Dòng {item.row}: {item.message}
                            </li>
                        ))}
                    </ul>
                    {report.errors.length > MAX_SHOWN_ERRORS ? (
                        <p className="text-xs text-muted">Chỉ hiện {MAX_SHOWN_ERRORS} lỗi đầu tiên.</p>
                    ) : null}
                </div>
            ) : report.dryRun ? (
                <p className="text-muted">File hợp lệ. Bấm "Nhập vào hệ thống" để ghi dữ liệu.</p>
            ) : null}
        </div>
    );
}

function AccountsView({ accounts }: { accounts: CreatedAccount[] }) {
    const [copied, setCopied] = useState(false);

    async function copyAll() {
        const text = accounts.map((a) => `${a.username}\t${a.name}\t${a.temporaryPassword}`).join("\n");
        try {
            await navigator.clipboard.writeText(text);
            setCopied(true);
        } catch {
            setCopied(false);
        }
    }

    return (
        <div className="flex flex-col gap-2 rounded-md border border-line bg-tint p-3">
            <span className={LABEL}>Tài khoản mới ({accounts.length})</span>
            <p className="text-xs text-muted">
                Mật khẩu tạm chỉ hiển thị lúc này. Gửi cho người dùng qua kênh an toàn; họ phải đổi mật khẩu khi đăng nhập.
            </p>
            <div className="max-h-64 overflow-auto">
                <table className="w-full text-left text-sm">
                    <thead className="text-xs text-muted">
                    <tr>
                        <th className="py-1 pr-2 font-medium">Tên đăng nhập</th>
                        <th className="py-1 pr-2 font-medium">Họ tên</th>
                        <th className="py-1 font-medium">Mật khẩu tạm</th>
                    </tr>
                    </thead>
                    <tbody>
                    {accounts.map((account) => (
                        <tr key={account.username} className="border-t border-line">
                            <td className="py-1 pr-2 font-mono">{account.username}</td>
                            <td className="py-1 pr-2">{account.name}</td>
                            <td className="py-1 font-mono">{account.temporaryPassword}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            <div>
                <button type="button" onClick={copyAll} className={BTN_GHOST}>
                    {copied ? "Đã sao chép" : "Sao chép tất cả"}
                </button>
            </div>
        </div>
    );
}

function TermCard() {
    const codeId = useId();
    const nameId = useId();
    const startId = useId();
    const endId = useId();
    const [code, setCode] = useState("");
    const [name, setName] = useState("");
    const [start, setStart] = useState("");
    const [end, setEnd] = useState("");
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    async function save() {
        if (!code.trim() || !name.trim() || !start || !end) {
            setNotice({ text: "Nhập đủ mã kỳ, tên học kỳ và hai ngày.", tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await saveAcademicTerm({ code: code.trim(), name: name.trim(), startDate: start, endDate: end });
            setNotice({ text: `Đã lưu học kỳ ${code.trim()}.`, tone: "info" });
            setCode("");
            setName("");
            setStart("");
            setEnd("");
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <section className={CARD} aria-label="Học kỳ">
            <div className="flex flex-col gap-1">
                <h3 className="text-base font-semibold">Thêm học kỳ</h3>
                <p className="text-xs text-muted">
                    Mã kỳ đã có thì cập nhật học kỳ đó. Khoảng ngày không được trùng với học kỳ khác có mã khác.
                    Có thể nhập nhiều học kỳ bằng file Excel với các cột: Mã kỳ, Tên học kỳ, Ngày bắt đầu, Ngày kết thúc.
                </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor={codeId} className={LABEL}>Mã kỳ</label>
                    <input id={codeId} value={code} onChange={(e) => setCode(e.target.value)} placeholder="Ví dụ: HK1-2026" className={INPUT} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={nameId} className={LABEL}>Tên học kỳ</label>
                    <input id={nameId} value={name} onChange={(e) => setName(e.target.value)} placeholder="Học kỳ 1 năm học 2026-2027" className={INPUT} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={startId} className={LABEL}>Ngày bắt đầu</label>
                    <input id={startId} type="date" value={start} onChange={(e) => setStart(e.target.value)} className={INPUT} />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor={endId} className={LABEL}>Ngày kết thúc</label>
                    <input id={endId} type="date" value={end} onChange={(e) => setEnd(e.target.value)} className={INPUT} />
                </div>
            </div>
            <div>
                <button type="button" disabled={busy} onClick={save} className={BTN_PRIMARY}>
                    Lưu học kỳ
                </button>
            </div>
            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
        </section>
    );
}