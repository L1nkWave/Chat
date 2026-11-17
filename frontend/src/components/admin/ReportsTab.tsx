"use client";

import { useEffect, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import {
    adminErrorMessage,
    getUsersByIds,
    resolveReport,
    searchReports,
} from "@/lib/admin/api";
import type {
    ModerationReport,
    ReportCategory,
    ReportFilter,
    ResolveDecision,
    UserInfo,
} from "@/lib/admin/api";
import { formatChatTime } from "@/lib/chat/time";
import { BTN_GHOST, BTN_PRIMARY, CARD, CATEGORY_LABEL, INPUT, LABEL } from "./shared";

const DECISIONS: { value: ResolveDecision; label: string }[] = [
    { value: "DISMISS", label: "Bỏ qua" },
    { value: "WARN", label: "Cảnh báo" },
    { value: "HIDE", label: "Gỡ nội dung và cảnh báo" },
];

const DECISION_DONE: Record<ResolveDecision, string> = {
    DISMISS: "Đã bỏ qua báo cáo.",
    WARN: "Đã gửi cảnh báo cho tác giả.",
    HIDE: "Đã gỡ nội dung và gửi cảnh báo cho tác giả.",
};

const DECISION_LABEL: Record<ResolveDecision, string> = {
    DISMISS: "Bỏ qua",
    WARN: "Cảnh báo",
    HIDE: "Gỡ nội dung",
};

const STATUS_OPTIONS: { value: ReportFilter["status"]; label: string }[] = [
    { value: "OPEN", label: "Đang chờ" },
    { value: "RESOLVED", label: "Đã xử lý" },
    { value: "ALL", label: "Tất cả" },
];

const CATEGORY_OPTIONS: ReportCategory[] = ["HARASSMENT", "PROFANITY", "SPAM", "INAPPROPRIATE", "OTHER"];

const EMPTY_FILTER: ReportFilter = {
    status: "OPEN",
    category: "",
    authorId: null,
    reporterId: null,
    from: "",
    to: "",
    q: "",
};

const PAGE_SIZE = 20;

// Tự làm mới mỗi 30 giây để không phải F5
const REFRESH_MS = 30_000;

type Feedback = { text: string; tone: "info" | "error" };

export function ReportsTab() {
    // draft: giá trị đang gõ; applied: bộ lọc đã áp dụng cho danh sách
    const [draft, setDraft] = useState<ReportFilter>(EMPTY_FILTER);
    const [applied, setApplied] = useState<ReportFilter>(EMPTY_FILTER);
    const [page, setPage] = useState(0);

    const [reports, setReports] = useState<ModerationReport[]>([]);
    const [total, setTotal] = useState(0);
    const [people, setPeople] = useState<Record<number, UserInfo>>({});
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState<string | null>(null);
    const [notes, setNotes] = useState<Record<string, string>>({});
    const [notice, setNotice] = useState<Feedback | null>(null);
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        const timer = window.setInterval(() => setReloadKey((key) => key + 1), REFRESH_MS);
        return () => window.clearInterval(timer);
    }, []);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        searchReports(applied, page, PAGE_SIZE)
            .then(async (result) => {
                if (!alive) return;
                setReports(result.items);
                setTotal(result.total);
                const ids = result.items.flatMap((item) => [item.authorId, item.reporterId]);
                const found = await getUsersByIds(ids).catch(() => [] as UserInfo[]);
                if (alive) {
                    setPeople(Object.fromEntries(found.map((person) => [person.id, person])));
                }
            })
            .catch((err: unknown) => {
                if (alive) setNotice({ text: adminErrorMessage(err), tone: "error" });
            })
            .finally(() => {
                if (alive) setLoading(false);
            });
        return () => {
            alive = false;
        };
    }, [applied, page, reloadKey]);

    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

    function applyFilter() {
        setNotice(null);
        setApplied(draft);
        setPage(0);
    }

    function clearFilter() {
        setNotice(null);
        setDraft(EMPTY_FILTER);
        setApplied(EMPTY_FILTER);
        setPage(0);
    }

    function who(id: number): string {
        const person = people[id];
        return person ? `${person.name} (@${person.username})` : `Mã #${id}`;
    }

    async function resolve(report: ModerationReport, decision: ResolveDecision) {
        if (
            decision === "HIDE" &&
            !window.confirm("Gỡ nội dung tin này với mọi thành viên và gửi cảnh báo cho tác giả?")
        ) {
            return;
        }
        const note = (notes[report.id] ?? "").trim();
        setBusyId(report.id);
        setNotice(null);
        try {
            await resolveReport(report.id, decision, note || undefined);
            setNotice({ text: DECISION_DONE[decision], tone: "info" });
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusyId(null);
        }
    }

    return (
        <section aria-label="Báo cáo tin nhắn" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-col">
                    <h2 className="font-serif text-lg font-semibold">Báo cáo tin nhắn</h2>
                    <p className="text-xs text-muted">Tự cập nhật mỗi 30 giây. Tìm thấy {total} báo cáo.</p>
                </div>
                <button type="button" onClick={() => setReloadKey((key) => key + 1)} className={BTN_GHOST}>
                    Làm mới
                </button>
            </div>

            <form
                onSubmit={(event) => {
                    event.preventDefault();
                    applyFilter();
                }}
                className={CARD}
            >
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                    <div className="flex flex-col gap-1">
                        <label htmlFor="rf-status" className={LABEL}>Trạng thái</label>
                        <select
                            id="rf-status"
                            value={draft.status}
                            onChange={(e) => setDraft({ ...draft, status: e.target.value as ReportFilter["status"] })}
                            className={INPUT}
                        >
                            {STATUS_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                        </select>
                    </div>

                    <div className="flex flex-col gap-1">
                        <label htmlFor="rf-category" className={LABEL}>Loại vi phạm</label>
                        <select
                            id="rf-category"
                            value={draft.category}
                            onChange={(e) => setDraft({ ...draft, category: e.target.value as ReportCategory | "" })}
                            className={INPUT}
                        >
                            <option value="">Tất cả</option>
                            {CATEGORY_OPTIONS.map((cat) => (
                                <option key={cat} value={cat}>{CATEGORY_LABEL[cat]}</option>
                            ))}
                        </select>
                    </div>

                    <div className="flex flex-col gap-1">
                        <label htmlFor="rf-author" className={LABEL}>Mã tác giả</label>
                        <input
                            id="rf-author"
                            type="number"
                            min={1}
                            value={draft.authorId ?? ""}
                            onChange={(e) =>
                                setDraft({ ...draft, authorId: e.target.value === "" ? null : Number(e.target.value) })
                            }
                            className={INPUT}
                        />
                    </div>

                    <div className="flex flex-col gap-1">
                        <label htmlFor="rf-reporter" className={LABEL}>Mã người báo cáo</label>
                        <input
                            id="rf-reporter"
                            type="number"
                            min={1}
                            value={draft.reporterId ?? ""}
                            onChange={(e) =>
                                setDraft({ ...draft, reporterId: e.target.value === "" ? null : Number(e.target.value) })
                            }
                            className={INPUT}
                        />
                    </div>

                    <div className="flex flex-col gap-1">
                        <label htmlFor="rf-from" className={LABEL}>Từ ngày</label>
                        <input
                            id="rf-from"
                            type="date"
                            value={draft.from}
                            onChange={(e) => setDraft({ ...draft, from: e.target.value })}
                            className={INPUT}
                        />
                    </div>

                    <div className="flex flex-col gap-1">
                        <label htmlFor="rf-to" className={LABEL}>Đến ngày</label>
                        <input
                            id="rf-to"
                            type="date"
                            value={draft.to}
                            onChange={(e) => setDraft({ ...draft, to: e.target.value })}
                            className={INPUT}
                        />
                    </div>

                    <div className="flex flex-col gap-1 sm:col-span-2">
                        <label htmlFor="rf-q" className={LABEL}>Tìm trong nội dung tin</label>
                        <input
                            id="rf-q"
                            value={draft.q}
                            maxLength={100}
                            onChange={(e) => setDraft({ ...draft, q: e.target.value })}
                            className={INPUT}
                        />
                    </div>
                </div>

                <div className="mt-3 flex flex-wrap justify-end gap-2">
                    <button type="button" onClick={clearFilter} className={BTN_GHOST}>
                        Xóa bộ lọc
                    </button>
                    <button type="submit" className={BTN_PRIMARY}>
                        Áp dụng
                    </button>
                </div>
            </form>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
            {loading ? <p className="text-sm text-muted">Đang tải...</p> : null}
            {!loading && reports.length === 0 ? (
                <p className="text-sm text-muted">Không có báo cáo nào khớp bộ lọc.</p>
            ) : null}

            <ul className="flex flex-col gap-3">
                {reports.map((report) => {
                    const open = report.status === "OPEN";
                    return (
                        <li key={report.id} className={CARD}>
                            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                                <span className="font-semibold text-primary">{CATEGORY_LABEL[report.category]}</span>
                                <span>Tác giả: {who(report.authorId)}</span>
                                <span>Người báo cáo: {who(report.reporterId)}</span>
                                <span>{formatChatTime(report.reportedAt)}</span>
                                {open ? null : (
                                    <span className="font-semibold">
                                        Đã xử lý: {report.decision ? DECISION_LABEL[report.decision] : "—"}
                                        {report.resolvedAt ? ` · ${formatChatTime(report.resolvedAt)}` : ""}
                                    </span>
                                )}
                            </div>

                            <blockquote className="whitespace-pre-wrap break-words rounded-md bg-paper p-3 text-sm leading-relaxed">
                                {report.messageText ?? "Tin là tệp hoặc đã bị xóa trước khi báo cáo."}
                            </blockquote>

                            {report.reason ? (
                                <p className="text-sm">
                                    <span className="text-muted">Lý do người báo cáo: </span>
                                    {report.reason}
                                </p>
                            ) : null}

                            {!open && report.resolutionNote ? (
                                <p className="text-sm">
                                    <span className="text-muted">Ghi chú xử lý: </span>
                                    {report.resolutionNote}
                                </p>
                            ) : null}

                            {open ? (
                                <>
                                    <label htmlFor={`note-${report.id}`} className={LABEL}>
                                        Lời nhắn gửi kèm cảnh báo (không bắt buộc)
                                    </label>
                                    <input
                                        id={`note-${report.id}`}
                                        value={notes[report.id] ?? ""}
                                        onChange={(event) =>
                                            setNotes((prev) => ({ ...prev, [report.id]: event.target.value }))
                                        }
                                        maxLength={300}
                                        className={INPUT}
                                    />

                                    <div className="flex flex-wrap gap-2">
                                        {DECISIONS.map((item) => (
                                            <button
                                                key={item.value}
                                                type="button"
                                                disabled={busyId === report.id}
                                                onClick={() => resolve(report, item.value)}
                                                className={item.value === "HIDE" ? BTN_PRIMARY : BTN_GHOST}
                                            >
                                                {item.label}
                                            </button>
                                        ))}
                                    </div>
                                </>
                            ) : null}
                        </li>
                    );
                })}
            </ul>

            {total > PAGE_SIZE ? (
                <div className="flex items-center justify-between gap-3 text-sm">
                    <button
                        type="button"
                        disabled={page === 0 || loading}
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        className={BTN_GHOST}
                    >
                        Trang trước
                    </button>
                    <span className="text-muted">Trang {page + 1} / {totalPages}</span>
                    <button
                        type="button"
                        disabled={page + 1 >= totalPages || loading}
                        onClick={() => setPage((p) => p + 1)}
                        className={BTN_GHOST}
                    >
                        Trang sau
                    </button>
                </div>
            ) : null}
        </section>
    );
}