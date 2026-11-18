"use client";

import { useId, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { importAcademicFile, saveAcademicTerm } from "@/lib/admin/academic-api";
import type { CreatedAccount, ImportKind, ImportReport } from "@/lib/admin/academic-api";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";

const MAX_SHOWN_ERRORS = 50;

type Feedback = { text: string; tone: "info" | "error" };

type CardSpec = {
    kind: ImportKind;
    title: string;
    columns: string;
    note?: string;
};

const IMPORT_CARDS: CardSpec[] = [
    {
        kind: "students",
        title: "Sinh viên",
        columns: "MSSV, Họ tên, Lớp, CCCD (không bắt buộc)",
        note: "Tạo tài khoản mới với mật khẩu tạm. Nếu tài khoản đã có thì chỉ cập nhật họ tên, lớp và CCCD. CCCD được mã hóa khi lưu.",
    },
    {
        kind: "timetable",
        title: "Thời khóa biểu",
        columns: "Mã MH, Môn, Nhóm, Phòng, Thứ, Tiết, Số tiết, GV, Lớp, Ngày, Giờ bắt đầu",
        note: "Mỗi dòng là một buổi học. Cột GV là họ tên giảng viên, phải khớp đúng họ tên trong tài khoản giảng viên. Phòng có phần mô tả sau dấu \"-\" thì chỉ lấy mã phòng.",
    },
    {
        kind: "exams",
        title: "Lịch thi",
        columns: "STT, Mã MH, Tên môn học, Sĩ số, Ngày thi, Giờ bắt đầu, Phòng thi, Cơ sở",
        note: "Đúng định dạng file lịch thi của trường. Các cột STT và cột khác không dùng đến sẽ bị bỏ qua.",
    },
    {
        kind: "duties",
        title: "Lịch gác thi",
        columns: "Mã GV, Mã MH, Ngày, Giờ thi, Phòng, Cơ sở",
        note: "Mã GV là tên đăng nhập của giảng viên.",
    },
];

/** Trang Học vụ của quản trị: nhập Excel theo từng loại và nhập học kỳ bằng form. */
export function AcademicTab() {
    return (
        <section aria-label="Dữ liệu học vụ" className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <h2 className="font-serif text-lg font-semibold">Dữ liệu học vụ</h2>
                <p className="text-sm text-muted">
                    Kiểm tra file trước khi nhập. Nếu file còn dòng lỗi thì hệ thống không ghi dòng nào; sửa file rồi kiểm tra lại.
                </p>
            </div>
            <div className="grid gap-4 lg:grid-cols-2">
                {IMPORT_CARDS.map((spec) => (
                    <ImportCard key={spec.kind} spec={spec} />
                ))}
                <TermCard />
            </div>
        </section>
    );
}

function ImportCard({ spec }: { spec: CardSpec }) {
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

/** Nói rõ số dòng mới và số dòng cập nhật, để nhập lại file cũ không bị hiểu nhầm là không ghi */
function successText(spec: CardSpec, result: ImportReport): string {
    if (spec.kind !== "students") {
        return `Đã ghi ${result.written} dòng vào mục ${spec.title.toLowerCase()}.`;
    }
    const created = result.createdAccounts.length;
    const updated = result.written - created;
    return `Đã xử lý ${result.written} sinh viên: tạo mới ${created} tài khoản, cập nhật ${updated} hồ sơ đã có.`;
}

function ReportView({ report }: { report: ImportReport }) {
    const shown = report.errors.slice(0, MAX_SHOWN_ERRORS);
    return (
        <div className="flex flex-col gap-2 text-sm">
            <p>
                {report.dryRun ? "Kết quả kiểm tra" : "Kết quả nhập"}: {report.total} dòng, hợp lệ {report.valid}
                {report.dryRun ? "" : `, đã ghi ${report.written}`}.
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
                Mật khẩu tạm chỉ hiển thị lúc này. Gửi cho sinh viên qua kênh an toàn; họ phải đổi mật khẩu khi đăng nhập.
            </p>
            <div className="max-h-64 overflow-auto">
                <table className="w-full text-left text-sm">
                    <thead className="text-xs text-muted">
                    <tr>
                        <th className="py-1 pr-2 font-medium">MSSV</th>
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
                <h3 className="text-base font-semibold">Học kỳ</h3>
                <p className="text-xs text-muted">Nhập tay từng học kỳ. Nếu có nhiều học kỳ, dùng file Excel với các cột: Mã kỳ, Tên học kỳ, Ngày bắt đầu, Ngày kết thúc.</p>
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