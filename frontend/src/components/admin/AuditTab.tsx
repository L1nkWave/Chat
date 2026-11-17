"use client";

import { useEffect, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { searchAudit } from "@/lib/admin/audit-api";
import type { AuditPage } from "@/lib/admin/audit-api";
import { formatChatTime } from "@/lib/chat/time";
import { BTN_GHOST, INPUT, LABEL } from "./shared";

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;

const ACTION_OPTIONS: { value: string; label: string }[] = [
    { value: "", label: "Tất cả hành động" },
    { value: "USER_LOCK", label: "Khóa tài khoản" },
    { value: "USER_UNLOCK", label: "Mở khóa tài khoản" },
    { value: "ROLE_CREATE", label: "Tạo vai trò" },
    { value: "ROLE_GRANT", label: "Cấp vai trò" },
    { value: "ROLE_REVOKE", label: "Thu hồi vai trò" },
];

const ACTION_LABEL: Record<string, string> = Object.fromEntries(
    ACTION_OPTIONS.filter((item) => item.value).map((item) => [item.value, item.label]),
);

const DIGITS = /^\d*$/;

export function AuditTab() {
    const [action, setAction] = useState("");
    const [actorId, setActorId] = useState("");
    const [targetId, setTargetId] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [page, setPage] = useState(0);

    const [result, setResult] = useState<AuditPage | null>(null);
    const [loading, setLoading] = useState(true);
    const [notice, setNotice] = useState<string | null>(null);

    useEffect(() => {
        if (!DIGITS.test(actorId) || !DIGITS.test(targetId)) {
            setNotice("Mã người thực hiện và mã đối tượng chỉ gồm chữ số.");
            setLoading(false);
            return;
        }
        if (from && to && from > to) {
            setNotice("Ngày bắt đầu không được sau ngày kết thúc.");
            setLoading(false);
            return;
        }

        let alive = true;
        const timer = window.setTimeout(() => {
            setLoading(true);
            setNotice(null);
            searchAudit({ action, actorId, targetId, from, to, page, size: PAGE_SIZE })
                .then((data) => {
                    if (alive) setResult(data);
                })
                .catch((err: unknown) => {
                    if (alive) setNotice(adminErrorMessage(err));
                })
                .finally(() => {
                    if (alive) setLoading(false);
                });
        }, SEARCH_DELAY_MS);
        return () => {
            alive = false;
            window.clearTimeout(timer);
        };
    }, [action, actorId, targetId, from, to, page]);

    function changeFilter(setter: (value: string) => void, value: string) {
        setter(value);
        setPage(0);
    }

    function clearFilters() {
        setAction("");
        setActorId("");
        setTargetId("");
        setFrom("");
        setTo("");
        setPage(0);
    }

    const totalPages = result ? Math.max(1, Math.ceil(result.total / PAGE_SIZE)) : 1;
    const hasFilter = Boolean(action || actorId || targetId || from || to);

    return (
        <section aria-label="Nhật ký hệ thống" className="flex flex-col gap-4">
            <div className="flex flex-col gap-3">
                <h2 className="font-serif text-lg font-semibold">Nhật ký hệ thống</h2>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                    <div className="flex flex-col gap-1">
                        <label htmlFor="audit-action" className={LABEL}>
                            Hành động
                        </label>
                        <select
                            id="audit-action"
                            value={action}
                            onChange={(event) => changeFilter(setAction, event.target.value)}
                            className={INPUT}
                        >
                            {ACTION_OPTIONS.map((item) => (
                                <option key={item.value} value={item.value}>
                                    {item.label}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="flex flex-col gap-1">
                        <label htmlFor="audit-actor" className={LABEL}>
                            Người thực hiện (mã)
                        </label>
                        <input
                            id="audit-actor"
                            inputMode="numeric"
                            value={actorId}
                            onChange={(event) => changeFilter(setActorId, event.target.value.trim())}
                            className={INPUT}
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <label htmlFor="audit-target" className={LABEL}>
                            Đối tượng (mã người dùng)
                        </label>
                        <input
                            id="audit-target"
                            inputMode="numeric"
                            value={targetId}
                            onChange={(event) => changeFilter(setTargetId, event.target.value.trim())}
                            className={INPUT}
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <label htmlFor="audit-from" className={LABEL}>
                            Từ ngày
                        </label>
                        <input
                            id="audit-from"
                            type="date"
                            value={from}
                            onChange={(event) => changeFilter(setFrom, event.target.value)}
                            className={INPUT}
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <label htmlFor="audit-to" className={LABEL}>
                            Đến ngày
                        </label>
                        <input
                            id="audit-to"
                            type="date"
                            value={to}
                            onChange={(event) => changeFilter(setTo, event.target.value)}
                            className={INPUT}
                        />
                    </div>
                </div>
                {hasFilter ? (
                    <div>
                        <button type="button" onClick={clearFilters} className={BTN_GHOST}>
                            Xóa bộ lọc
                        </button>
                    </div>
                ) : null}
            </div>

            {notice ? <Notice text={notice} tone="error" onClose={() => setNotice(null)} /> : null}

            <p className="text-xs text-muted">
                {result ? `${result.total} bản ghi` : " "}
                {loading ? " · Đang tải..." : ""}
            </p>

            {result && result.items.length === 0 && !loading ? (
                <p className="text-sm text-muted">Không có bản ghi nào phù hợp.</p>
            ) : null}

            <ul className="flex flex-col divide-y divide-line overflow-hidden rounded-lg border border-line bg-surface">
                {result?.items.map((entry) => (
                    <li key={entry.id} className="flex flex-col gap-1 px-4 py-3 text-sm">
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                            <span>{formatChatTime(entry.createdAt)}</span>
                            <span>Người thực hiện #{entry.actorId}</span>
                            {entry.ip ? <span>IP {entry.ip}</span> : null}
                        </div>
                        <p className="break-words">
                            <span className="font-semibold text-primary">
                                {ACTION_LABEL[entry.action] ?? entry.action}
                            </span>
                            <span className="ml-2 font-mono text-[12px] text-muted">{entry.action}</span>
                            {entry.targetType ? (
                                <span className="text-muted">
                                    {" · "}
                                    {entry.targetType} {entry.targetId ?? ""}
                                </span>
                            ) : null}
                        </p>
                        {entry.detail ? <p className="break-words text-muted">{entry.detail}</p> : null}
                    </li>
                ))}
            </ul>

            <div className="flex items-center justify-between gap-3">
                <button
                    type="button"
                    disabled={page === 0 || loading}
                    onClick={() => setPage((value) => Math.max(0, value - 1))}
                    className={BTN_GHOST}
                >
                    Trang trước
                </button>
                <span className="text-sm text-muted">
                    Trang {page + 1} / {totalPages}
                </span>
                <button
                    type="button"
                    disabled={!result || page + 1 >= totalPages || loading}
                    onClick={() => setPage((value) => value + 1)}
                    className={BTN_GHOST}
                >
                    Trang sau
                </button>
            </div>
        </section>
    );
}