"use client";

import { useEffect, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage, setUserLocked } from "@/lib/admin/api";
import {
    getUserDetail,
    resetUserPassword,
    searchUsers,
    setUserGlobalRole,
    updateUserProfile,
} from "@/lib/admin/users-api";
import type { UserDetail, UserPage, UserStatusFilter } from "@/lib/admin/users-api";
import { formatChatTime } from "@/lib/chat/time";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, LABEL, PERMISSION_OPTIONS } from "./shared";

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;

const ROLE_OPTIONS: { value: string; label: string }[] = [
    { value: "", label: "Tất cả vai trò" },
    { value: "ROLE_STUDENT", label: "Sinh viên" },
    { value: "ROLE_LECTURER", label: "Giảng viên" },
    { value: "ROLE_ADMIN", label: "Quản trị viên" },
    { value: "ROLE_ROOT", label: "Quản trị cao nhất" },
];

const ROLE_LABEL: Record<string, string> = Object.fromEntries(
    ROLE_OPTIONS.filter((item) => item.value).map((item) => [item.value, item.label]),
);

const PERMISSION_LABEL: Record<string, string> = Object.fromEntries(
    PERMISSION_OPTIONS.map((item) => [item.code, item.label]),
);

const AUDIT_LABEL: Record<string, string> = {
    USER_LOCK: "Khóa tài khoản",
    USER_UNLOCK: "Mở khóa tài khoản",
    ROLE_GRANT: "Cấp vai trò",
    ROLE_REVOKE: "Thu hồi vai trò",
    USER_EDIT: "Sửa thông tin",
    PASSWORD_RESET: "Đặt lại mật khẩu",
    ROLE_SET: "Đổi vai trò toàn trường",
};

const SCOPE_LABEL: Record<"SCHOOL" | "FACULTY" | "CLASS", string> = {
    SCHOOL: "Toàn trường",
    FACULTY: "Khoa",
    CLASS: "Lớp",
};

const GLOBAL_ROLE_OPTIONS: { value: string; label: string }[] = [
    { value: "ROLE_STUDENT", label: "Sinh viên" },
    { value: "ROLE_LECTURER", label: "Giảng viên" },
    { value: "ROLE_ADMIN", label: "Quản trị viên" },
];

type Feedback = { text: string; tone: "info" | "error" };

/** isRoot chỉ để hiện mục đổi vai trò; máy chủ vẫn kiểm tra lại. */
export function UsersTab({ isRoot }: { isRoot: boolean }) {
    const [q, setQ] = useState("");
    const [role, setRole] = useState("");
    const [status, setStatus] = useState<UserStatusFilter>("ALL");
    const [page, setPage] = useState(0);

    const [result, setResult] = useState<UserPage | null>(null);
    const [loading, setLoading] = useState(true);
    const [reloadKey, setReloadKey] = useState(0);

    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [detail, setDetail] = useState<UserDetail | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);

    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    const [editing, setEditing] = useState(false);
    const [editName, setEditName] = useState("");
    const [editBio, setEditBio] = useState("");
    const [roleChoice, setRoleChoice] = useState("");
    const [tempPassword, setTempPassword] = useState<string | null>(null);
    const [copied, setCopied] = useState(false);

    // Tìm sau khi người dùng ngừng gõ một lúc
    useEffect(() => {
        let alive = true;
        const timer = window.setTimeout(() => {
            setLoading(true);
            searchUsers({ q: q.trim(), role, status, page, size: PAGE_SIZE })
                .then((data) => {
                    if (alive) setResult(data);
                })
                .catch((err: unknown) => {
                    if (alive) setNotice({ text: adminErrorMessage(err), tone: "error" });
                })
                .finally(() => {
                    if (alive) setLoading(false);
                });
        }, SEARCH_DELAY_MS);
        return () => {
            alive = false;
            window.clearTimeout(timer);
        };
    }, [q, role, status, page, reloadKey]);

    useEffect(() => {
        if (selectedId === null) return;
        let alive = true;
        setDetailLoading(true);
        getUserDetail(selectedId)
            .then((data) => {
                if (alive) setDetail(data);
            })
            .catch((err: unknown) => {
                if (alive) {
                    setDetail(null);
                    setNotice({ text: adminErrorMessage(err), tone: "error" });
                }
            })
            .finally(() => {
                if (alive) setDetailLoading(false);
            });
        return () => {
            alive = false;
        };
    }, [selectedId, reloadKey]);

    function changeFilter<T>(setter: (value: T) => void, value: T) {
        setter(value);
        setPage(0);
    }

    async function toggleLock(locked: boolean) {
        if (!detail) return;
        const verb = locked ? "khóa" : "mở khóa";
        if (!window.confirm(`Bạn chắc chắn muốn ${verb} tài khoản ${detail.username}?`)) return;
        setBusy(true);
        setNotice(null);
        try {
            await setUserLocked(detail.id, locked);
            setNotice({
                text: locked
                    ? `Đã khóa ${detail.username}. WebSocket đang mở đã bị ngắt; phiên HTTP còn hiệu lực tối đa 5 phút.`
                    : `Đã mở khóa ${detail.username}.`,
                tone: "info",
            });
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    function selectUser(id: number) {
        setSelectedId(id);
        setEditing(false);
        setTempPassword(null);
        setCopied(false);
        setRoleChoice("");
        setNotice(null);
    }

    function startEdit() {
        if (!detail) return;
        setEditName(detail.name);
        setEditBio(detail.bio ?? "");
        setEditing(true);
    }

    async function saveProfile() {
        if (!detail) return;
        if (!editName.trim()) {
            setNotice({ text: "Họ tên không được để trống.", tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await updateUserProfile(detail.id, editName.trim(), editBio.trim());
            setEditing(false);
            setNotice({ text: `Đã cập nhật thông tin của ${detail.username}.`, tone: "info" });
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function resetPassword() {
        if (!detail) return;
        const message =
            `Đặt lại mật khẩu của ${detail.username}? Mọi phiên đăng nhập hiện có sẽ bị đăng xuất ` +
            "và người dùng phải đổi mật khẩu khi đăng nhập lại.";
        if (!window.confirm(message)) return;
        setBusy(true);
        setNotice(null);
        setTempPassword(null);
        try {
            const data = await resetUserPassword(detail.id);
            setTempPassword(data.temporaryPassword);
            setCopied(false);
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function copyTempPassword() {
        if (!tempPassword) return;
        try {
            await navigator.clipboard.writeText(tempPassword);
            setCopied(true);
        } catch {
            setNotice({ text: "Không sao chép được. Hãy chọn và sao chép thủ công.", tone: "error" });
        }
    }

    async function saveGlobalRole() {
        if (!detail || !roleChoice) return;
        const label = ROLE_LABEL[roleChoice] ?? roleChoice;
        const message =
            `Đặt vai trò toàn trường của ${detail.username} thành ${label}? ` +
            "Người dùng sẽ bị đăng xuất và nhận quyền mới khi đăng nhập lại.";
        if (!window.confirm(message)) return;
        setBusy(true);
        setNotice(null);
        try {
            await setUserGlobalRole(detail.id, roleChoice);
            setNotice({ text: `Đã đổi vai trò của ${detail.username} thành ${label}.`, tone: "info" });
            setRoleChoice("");
            setReloadKey((key) => key + 1);
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    const currentGlobalRole =
        detail?.roles.find((item) => GLOBAL_ROLE_OPTIONS.some((option) => option.value === item)) ?? "";

    const totalPages = result ? Math.max(1, Math.ceil(result.total / PAGE_SIZE)) : 1;

    return (
        <section aria-label="Quản lý người dùng" className="flex flex-col gap-4">
            <div className="flex flex-col gap-3">
                <h2 className="font-serif text-lg font-semibold">Người dùng</h2>
                <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_200px_180px]">
                    <div className="flex flex-col gap-1">
                        <label htmlFor="user-search" className={LABEL}>
                            Tìm theo tên hoặc tên đăng nhập
                        </label>
                        <input
                            id="user-search"
                            value={q}
                            onChange={(event) => changeFilter(setQ, event.target.value)}
                            placeholder="Ví dụ: Nguyễn Văn A hoặc sv001"
                            className={INPUT}
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <label htmlFor="user-role" className={LABEL}>
                            Vai trò
                        </label>
                        <select
                            id="user-role"
                            value={role}
                            onChange={(event) => changeFilter(setRole, event.target.value)}
                            className={INPUT}
                        >
                            {ROLE_OPTIONS.map((item) => (
                                <option key={item.value} value={item.value}>
                                    {item.label}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="flex flex-col gap-1">
                        <label htmlFor="user-status" className={LABEL}>
                            Trạng thái
                        </label>
                        <select
                            id="user-status"
                            value={status}
                            onChange={(event) => changeFilter(setStatus, event.target.value as UserStatusFilter)}
                            className={INPUT}
                        >
                            <option value="ALL">Tất cả</option>
                            <option value="ACTIVE">Đang hoạt động</option>
                            <option value="LOCKED">Đang bị khóa</option>
                        </select>
                    </div>
                </div>
            </div>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <div className="flex flex-col gap-4 lg:flex-row lg:items-start">
                <div className="flex min-w-0 flex-1 flex-col gap-3">
                    <p className="text-xs text-muted">
                        {result ? `${result.total} người phù hợp` : " "}
                        {loading ? " · Đang tải..." : ""}
                    </p>

                    {result && result.items.length === 0 && !loading ? (
                        <p className="text-sm text-muted">Không tìm thấy người dùng nào.</p>
                    ) : null}

                    <ul className="flex flex-col gap-2">
                        {result?.items.map((user) => {
                            const active = selectedId === user.id;
                            return (
                                <li key={user.id}>
                                    <button
                                        type="button"
                                        onClick={() => selectUser(user.id)}
                                        aria-current={active ? "true" : undefined}
                                        className={`flex w-full flex-wrap items-center justify-between gap-2 rounded-lg border p-3 text-left transition-colors ${
                                            active ? "border-primary bg-tint" : "border-line bg-surface hover:bg-tint"
                                        }`}
                                    >
                                        <span className="flex min-w-0 flex-col">
                                            <span className="truncate font-medium">
                                                {user.name}
                                                {user.locked ? (
                                                    <span className="ml-2 text-xs font-semibold text-primary">Đang khóa</span>
                                                ) : null}
                                            </span>
                                            <span className="truncate text-xs text-muted">@{user.username}</span>
                                        </span>
                                        <span className="flex flex-wrap gap-1 text-xs text-muted">
                                            {user.roles
                                                .filter((item) => item !== "ROLE_USER")
                                                .map((item) => (
                                                    <span key={item} className="rounded-full border border-line px-2 py-0.5">
                                                        {ROLE_LABEL[item] ?? item}
                                                    </span>
                                                ))}
                                        </span>
                                    </button>
                                </li>
                            );
                        })}
                    </ul>

                    <div className="flex items-center justify-between gap-2 pt-1">
                        <button
                            type="button"
                            disabled={page === 0 || loading}
                            onClick={() => setPage((value) => Math.max(0, value - 1))}
                            className={BTN_GHOST}
                        >
                            Trang trước
                        </button>
                        <span className="text-xs text-muted">
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
                </div>

                <aside className="w-full lg:w-[380px] lg:shrink-0">
                    {selectedId === null ? (
                        <div className={CARD}>
                            <p className="text-sm text-muted">Chọn một người dùng để xem hồ sơ.</p>
                        </div>
                    ) : detailLoading && !detail ? (
                        <div className={CARD}>
                            <p className="text-sm text-muted">Đang tải hồ sơ...</p>
                        </div>
                    ) : detail ? (
                        <div className={CARD}>
                            <div className="flex flex-col">
                                <h3 className="text-base font-semibold">{detail.name}</h3>
                                <p className="text-xs text-muted">@{detail.username} · Mã #{detail.id}</p>
                            </div>

                            <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm">
                                <dt className="text-muted">Trạng thái</dt>
                                <dd>{detail.locked ? "Đang bị khóa" : "Đang hoạt động"}</dd>
                                <dt className="text-muted">Tạo lúc</dt>
                                <dd>{formatChatTime(detail.createdAt)}</dd>
                                <dt className="text-muted">Truy cập gần nhất</dt>
                                <dd>{formatChatTime(detail.lastSeen)}</dd>
                                <dt className="text-muted">Xác thực hai lớp</dt>
                                <dd>
                                    {detail.twoFactorEnabled === null
                                        ? "Không xác định"
                                        : detail.twoFactorEnabled
                                            ? "Đã bật"
                                            : "Chưa bật"}
                                </dd>
                            </dl>

                            {detail.bio ? <p className="text-sm leading-relaxed">{detail.bio}</p> : null}

                            <div className="flex flex-col gap-1">
                                <span className={LABEL}>Vai trò toàn trường</span>
                                {detail.roles.filter((item) => item !== "ROLE_USER").length > 0 ? (
                                    <p className="text-sm">
                                        {detail.roles
                                            .filter((item) => item !== "ROLE_USER")
                                            .map((item) => ROLE_LABEL[item] ?? item)
                                            .join(", ")}
                                    </p>
                                ) : (
                                    <p className="text-sm text-muted">Không có vai trò toàn trường.</p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className={LABEL}>Vai trò theo phạm vi</span>
                                {detail.scopedRoles.length > 0 ? (
                                    detail.scopedRoles.map((grant) => (
                                        <p key={grant.grantId} className="text-sm">
                                            {ROLE_LABEL[grant.role] ?? grant.role} ·{" "}
                                            {SCOPE_LABEL[grant.scopeType]}
                                            {grant.scopeId ? ` #${grant.scopeId}` : ""}
                                        </p>
                                    ))
                                ) : (
                                    <p className="text-sm text-muted">Không có.</p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className={LABEL}>Quyền thực tế (toàn trường)</span>
                                {detail.schoolPermissions.length > 0 ? (
                                    <p className="text-sm">
                                        {detail.schoolPermissions.map((code) => PERMISSION_LABEL[code] ?? code).join(", ")}
                                    </p>
                                ) : (
                                    <p className="text-sm text-muted">Không có quyền nào.</p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className={LABEL}>Hoạt động gần đây</span>
                                {detail.recentAudit.length > 0 ? (
                                    <ul className="flex flex-col divide-y divide-line text-sm">
                                        {detail.recentAudit.map((entry) => (
                                            <li key={entry.id} className="flex flex-col py-2">
                                                <span>{AUDIT_LABEL[entry.action] ?? entry.action}</span>
                                                <span className="text-xs text-muted">
                                                    {formatChatTime(entry.createdAt)} · bởi #{entry.actorId}
                                                </span>
                                            </li>
                                        ))}
                                    </ul>
                                ) : (
                                    <p className="text-sm text-muted">Chưa có hoạt động nào.</p>
                                )}
                            </div>

                            {editing ? (
                                <div className="flex flex-col gap-3 border-t border-line pt-3">
                                    <div className="flex flex-col gap-1">
                                        <label htmlFor="edit-name" className={LABEL}>
                                            Họ tên
                                        </label>
                                        <input
                                            id="edit-name"
                                            value={editName}
                                            maxLength={64}
                                            onChange={(event) => setEditName(event.target.value)}
                                            className={INPUT}
                                        />
                                    </div>
                                    <div className="flex flex-col gap-1">
                                        <label htmlFor="edit-bio" className={LABEL}>
                                            Tiểu sử
                                        </label>
                                        <textarea
                                            id="edit-bio"
                                            value={editBio}
                                            maxLength={255}
                                            rows={3}
                                            onChange={(event) => setEditBio(event.target.value)}
                                            className="min-w-0 w-full rounded-md border border-line bg-paper px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-primary"
                                        />
                                    </div>
                                    <div className="flex flex-wrap gap-2">
                                        <button type="button" disabled={busy} onClick={saveProfile} className={BTN_PRIMARY}>
                                            Lưu thông tin
                                        </button>
                                        <button type="button" disabled={busy} onClick={() => setEditing(false)} className={BTN_GHOST}>
                                            Hủy
                                        </button>
                                    </div>
                                </div>
                            ) : null}

                            {isRoot && !detail.roles.includes("ROLE_ROOT") ? (
                                <div className="flex flex-col gap-2 border-t border-line pt-3">
                                    <span className={LABEL}>Đặt vai trò toàn trường</span>
                                    <div className="flex gap-2">
                                        <select
                                            aria-label="Vai trò toàn trường"
                                            value={roleChoice || currentGlobalRole}
                                            disabled={busy}
                                            onChange={(event) => setRoleChoice(event.target.value)}
                                            className={INPUT}
                                        >
                                            {currentGlobalRole === "" ? <option value="">Chưa có</option> : null}
                                            {GLOBAL_ROLE_OPTIONS.map((item) => (
                                                <option key={item.value} value={item.value}>
                                                    {item.label}
                                                </option>
                                            ))}
                                        </select>
                                        <button
                                            type="button"
                                            disabled={busy || !roleChoice || roleChoice === currentGlobalRole}
                                            onClick={saveGlobalRole}
                                            className={BTN_PRIMARY}
                                        >
                                            Lưu
                                        </button>
                                    </div>
                                    <p className="text-xs text-muted">
                                        Đổi vai trò sẽ đăng xuất người dùng để họ nhận quyền mới khi đăng nhập lại.
                                    </p>
                                </div>
                            ) : null}

                            {tempPassword ? (
                                <div className="flex flex-col gap-2 rounded-md border border-line bg-tint p-3">
                                    <span className={LABEL}>Mật khẩu tạm</span>
                                    <code className="break-all font-mono text-base">{tempPassword}</code>
                                    <div className="flex flex-wrap gap-2">
                                        <button type="button" onClick={copyTempPassword} className={BTN_GHOST}>
                                            {copied ? "Đã sao chép" : "Sao chép"}
                                        </button>
                                    </div>
                                    <p className="text-xs text-muted">
                                        Chỉ hiển thị một lần. Gửi cho người dùng qua kênh an toàn; họ phải đổi mật khẩu khi đăng nhập.
                                    </p>
                                </div>
                            ) : null}

                            <div className="flex flex-wrap gap-2 pt-1">
                                {!editing ? (
                                    <button type="button" disabled={busy} onClick={startEdit} className={BTN_GHOST}>
                                        Sửa thông tin
                                    </button>
                                ) : null}
                                <button type="button" disabled={busy} onClick={resetPassword} className={BTN_GHOST}>
                                    Đặt lại mật khẩu
                                </button>
                                {detail.locked ? (
                                    <button type="button" disabled={busy} onClick={() => toggleLock(false)} className={BTN_PRIMARY}>
                                        Mở khóa
                                    </button>
                                ) : (
                                    <button type="button" disabled={busy} onClick={() => toggleLock(true)} className={BTN_GHOST}>
                                        Khóa tài khoản
                                    </button>
                                )}
                            </div>
                        </div>
                    ) : null}
                </aside>
            </div>
        </section>
    );
}