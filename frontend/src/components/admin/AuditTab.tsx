"use client";

import { useEffect, useState } from "react";

import { EntityPicker } from "./EntityPicker";
import type { PickerOption } from "./EntityPicker";
import { actionLabel, AUDIT_ACTION_OPTIONS } from "./labels";
import { searchPeople } from "./pickers";
import { buttonStyle, EMPTY, FIELD, HELP, LABEL, SECTION_TITLE, SELECT, TABLE_WRAP, TD } from "@/components/ui/styles";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage, getUsersByIds } from "@/lib/admin/api";
import { searchAudit } from "@/lib/admin/audit-api";
import type { AuditPage } from "@/lib/admin/audit-api";
import { formatChatTime } from "@/lib/chat/time";

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;
const DIGITS = /^\d*$/;

export function AuditTab() {
    const [action, setAction] = useState("");
    const [actor, setActor] = useState<PickerOption | null>(null);
    const [targetId, setTargetId] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [page, setPage] = useState(0);

    const [result, setResult] = useState<AuditPage | null>(null);
    const [loading, setLoading] = useState(true);
    const [notice, setNotice] = useState<string | null>(null);
    /** Tên tài khoản theo mã, để hiện bên cạnh mã người thực hiện và đối tượng */
    const [names, setNames] = useState<Record<number, string>>({});

    const actorId = actor ? String(actor.id) : "";

    useEffect(() => {
        if (!DIGITS.test(targetId)) {
            setNotice("Mã đối tượng chỉ gồm chữ số.");
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

    // Lấy tên cho các mã có trong trang hiện tại
    useEffect(() => {
        if (!result) return;
        const ids = new Set<number>();
        for (const entry of result.items) {
            ids.add(entry.actorId);
            if (entry.targetType === "USER" && entry.targetId && DIGITS.test(entry.targetId) && entry.targetId !== "") {
                ids.add(Number(entry.targetId));
            }
        }
        const missing = [...ids].filter((id) => !(id in names));
        if (missing.length === 0) return;
        let alive = true;
        getUsersByIds(missing)
            .then((users) => {
                if (!alive) return;
                setNames((prev) => {
                    const next = { ...prev };
                    for (const user of users) next[user.id] = `${user.name} (@${user.username})`;
                    return next;
                });
            })
            .catch(() => {
                // Không lấy được tên thì vẫn hiện mã; không làm gián đoạn màn hình
            });
        return () => {
            alive = false;
        };
    }, [result, names]);

    function changeFilter<T>(setter: (value: T) => void, value: T) {
        setter(value);
        setPage(0);
    }

    function clearFilters() {
        setAction("");
        setActor(null);
        setTargetId("");
        setFrom("");
        setTo("");
        setPage(0);
    }

    const totalPages = result ? Math.max(1, Math.ceil(result.total / PAGE_SIZE)) : 1;
    const hasFilter = Boolean(action || actor || targetId || from || to);

    return (
        <section aria-label="Nhật ký hệ thống" className="flex flex-col gap-4">
            <div className="flex flex-col gap-3">
                <h2 className={SECTION_TITLE}>Nhật ký hệ thống</h2>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
                    <div className="flex flex-col gap-1.5 lg:col-span-2">
                        <label htmlFor="audit-action" className={LABEL}>
                            Hành động
                        </label>
                        <select
                            id="audit-action"
                            value={action}
                            onChange={(event) => changeFilter(setAction, event.target.value)}
                            className={SELECT}
                        >
                            {AUDIT_ACTION_OPTIONS.map((item) => (
                                <option key={item.value} value={item.value}>
                                    {item.label}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="flex flex-col gap-1.5 lg:col-span-2">
                        <EntityPicker
                            id="audit-actor"
                            label="Người thực hiện (tìm theo tên)"
                            placeholder="Nhập tên hoặc tên đăng nhập"
                            search={searchPeople}
                            value={actor}
                            onChange={(option) => changeFilter(setActor, option)}
                        />
                    </div>
                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="audit-target" className={LABEL}>
                            Đối tượng (mã người dùng)
                        </label>
                        <input
                            id="audit-target"
                            inputMode="numeric"
                            value={targetId}
                            onChange={(event) => changeFilter(setTargetId, event.target.value.trim())}
                            className={FIELD}
                        />
                    </div>
                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="audit-from" className={LABEL}>
                            Từ ngày
                        </label>
                        <input
                            id="audit-from"
                            type="date"
                            value={from}
                            onChange={(event) => changeFilter(setFrom, event.target.value)}
                            className={FIELD}
                        />
                    </div>
                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="audit-to" className={LABEL}>
                            Đến ngày
                        </label>
                        <input
                            id="audit-to"
                            type="date"
                            value={to}
                            onChange={(event) => changeFilter(setTo, event.target.value)}
                            className={FIELD}
                        />
                    </div>
                </div>
                {hasFilter ? (
                    <div>
                        <button type="button" onClick={clearFilters} className={buttonStyle("ghost", "md")}>
                            Xóa bộ lọc
                        </button>
                    </div>
                ) : null}
            </div>

            {notice ? <Notice text={notice} tone="error" onClose={() => setNotice(null)} /> : null}

            <p className={HELP}>
                {result ? `${result.total} bản ghi` : " "}
                {loading ? " · Đang tải..." : ""}
            </p>

            {result && result.items.length === 0 && !loading ? (
                <p className={EMPTY}>Không có bản ghi nào phù hợp.</p>
            ) : null}

            <ul className={TABLE_WRAP}>
                {result?.items.map((entry) => (
                    <li key={entry.id} className={`${TD} flex flex-col gap-1 text-sm`}>
                        <div className={`flex flex-wrap items-center gap-x-3 gap-y-1 ${HELP}`}>
                            <span>{formatChatTime(entry.createdAt)}</span>
                            <span>
                                Người thực hiện: {names[entry.actorId] ?? `mã #${entry.actorId}`}
                            </span>
                            {entry.ip ? <span>IP {entry.ip}</span> : null}
                        </div>
                        <p className="break-words">
                            <span className="font-semibold text-ink">{actionLabel(entry.action)}</span>
                            {entry.targetType ? (
                                <span className="text-muted">
                                    {" · "}
                                    {entry.targetType === "USER" && entry.targetId && names[Number(entry.targetId)]
                                        ? `Người dùng: ${names[Number(entry.targetId)]}`
                                        : `${entry.targetType} ${entry.targetId ?? ""}`}
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
                    className={buttonStyle("secondary", "md")}
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
                    className={buttonStyle("secondary", "md")}
                >
                    Trang sau
                </button>
            </div>
        </section>
    );
}