"use client";

import { useEffect, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import {
    adminErrorMessage,
    getUsersByIds,
    listOpenReports,
    resolveReport,
} from "@/lib/admin/api";
import type { ModerationReport, ResolveDecision, UserInfo } from "@/lib/admin/api";
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

// Tự làm mới danh sách mỗi 30 giây để không phải F5
const REFRESH_MS = 30_000;

type Feedback = { text: string; tone: "info" | "error" };

export function ReportsTab() {
    const [reports, setReports] = useState<ModerationReport[]>([]);
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
        listOpenReports(0, 20)
            .then(async (list) => {
                if (!alive) return;
                setReports(list);
                const ids = list.flatMap((item) => [item.authorId, item.reporterId]);
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
    }, [reloadKey]);

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
            setReports((prev) => prev.filter((item) => item.id !== report.id));
            setNotice({ text: DECISION_DONE[decision], tone: "info" });
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
                    <h2 className="font-serif text-lg font-semibold">Báo cáo đang chờ xử lý</h2>
                    <p className="text-xs text-muted">
                        Tự cập nhật mỗi 30 giây. Hiển thị tối đa 20 báo cáo cũ nhất.
                    </p>
                </div>
                <button type="button" onClick={() => setReloadKey((key) => key + 1)} className={BTN_GHOST}>
                    Làm mới
                </button>
            </div>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
            {loading ? <p className="text-sm text-muted">Đang tải...</p> : null}
            {!loading && reports.length === 0 ? (
                <p className="text-sm text-muted">Không có báo cáo nào đang chờ.</p>
            ) : null}

            <ul className="flex flex-col gap-3">
                {reports.map((report) => (
                    <li key={report.id} className={CARD}>
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                            <span className="font-semibold text-primary">{CATEGORY_LABEL[report.category]}</span>
                            <span>Tác giả: {who(report.authorId)}</span>
                            <span>Người báo cáo: {who(report.reporterId)}</span>
                            <span>{formatChatTime(report.reportedAt)}</span>
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
                    </li>
                ))}
            </ul>
        </section>
    );
}