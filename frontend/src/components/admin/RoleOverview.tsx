"use client";

import { useEffect, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import {
    listRoleMembers,
    listRoleOverview,
    updateRoleDisplayName,
    updateRolePermissions,
} from "@/lib/admin/roles-api";
import type { RoleMember, RoleOverview as RoleItem } from "@/lib/admin/roles-api";
import { ROLE_LABEL, SCOPE_LABEL } from "./labels";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, PERMISSION_OPTIONS, permissionLabel } from "./shared";

function describeMember(member: RoleMember): string {
    if (member.source === "GLOBAL") return "Gán toàn trường";
    const scope = member.scopeType ? SCOPE_LABEL[member.scopeType] : "Phạm vi";
    return member.scopeId ? `${scope} #${member.scopeId}` : scope;
}

/** Tên hiển thị: ưu tiên tên đã đặt, rồi nhãn mặc định, cuối cùng là mã kỹ thuật */
function roleTitle(role: RoleItem): string {
    return role.displayName || ROLE_LABEL[role.name] || role.name;
}

type RoleOverviewProps = {
    /** Đổi giá trị này để tải lại danh sách (ví dụ sau khi thêm vai trò mới) */
    refreshKey: number;
};

/** Danh sách vai trò: xem người có vai trò, đổi tên, sửa bộ quyền. Chỉ ROOT. */
export function RoleOverview({ refreshKey }: RoleOverviewProps) {
    const [roles, setRoles] = useState<RoleItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [notice, setNotice] = useState<{ text: string; tone: "info" | "error" } | null>(null);
    const [openName, setOpenName] = useState<string | null>(null);
    const [members, setMembers] = useState<Record<string, RoleMember[]>>({});
    const [reloadKey, setReloadKey] = useState(0);
    const [busy, setBusy] = useState(false);

    const [renamingName, setRenamingName] = useState<string | null>(null);
    const [nameDraft, setNameDraft] = useState("");

    const [editingName, setEditingName] = useState<string | null>(null);
    const [editSelected, setEditSelected] = useState<string[]>([]);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        listRoleOverview()
            .then((list) => {
                if (alive) setRoles(list);
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
    }, [refreshKey, reloadKey]);

    function reload() {
        setMembers({});
        setReloadKey((key) => key + 1);
    }

    async function toggleMembers(name: string) {
        if (openName === name) {
            setOpenName(null);
            return;
        }
        setOpenName(name);
        if (members[name]) return;
        try {
            const list = await listRoleMembers(name);
            setMembers((prev) => ({ ...prev, [name]: list }));
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        }
    }

    function startRename(role: RoleItem) {
        setRenamingName(role.name);
        setNameDraft(role.displayName ?? "");
        setNotice(null);
    }

    async function saveRename(role: RoleItem) {
        const next = nameDraft.trim();
        if (!next) {
            setNotice({ text: "Tên hiển thị không được để trống.", tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await updateRoleDisplayName(role.name, next);
            setNotice({ text: `Đã đổi tên vai trò thành "${next}".`, tone: "info" });
            setRenamingName(null);
            reload();
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    function startEdit(role: RoleItem) {
        setEditingName(role.name);
        setEditSelected(role.permissions);
        setNotice(null);
    }

    function toggleEditPermission(code: string) {
        setEditSelected((prev) =>
            prev.includes(code) ? prev.filter((item) => item !== code) : [...prev, code],
        );
    }

    async function saveEdit(role: RoleItem) {
        if (editSelected.length === 0) {
            setNotice({ text: "Vai trò phải có ít nhất một quyền.", tone: "error" });
            return;
        }
        if (!window.confirm(`Sửa quyền của "${roleTitle(role)}"? Thay đổi có hiệu lực với mọi người đang giữ vai trò này.`)) {
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            await updateRolePermissions(role.name, editSelected);
            setNotice({ text: `Đã cập nhật quyền của "${roleTitle(role)}".`, tone: "info" });
            setEditingName(null);
            reload();
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <section aria-label="Tổng quan vai trò" className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-col">
                    <h3 className="text-sm font-semibold">Danh sách vai trò</h3>
                    <p className="text-xs text-muted">
                        Có {roles.length} vai trò. Bấm vào một vai trò để xem người có vai trò đó.
                    </p>
                </div>
                <button type="button" onClick={reload} className={BTN_GHOST}>
                    Làm mới
                </button>
            </div>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}
            {loading ? <p className="text-sm text-muted">Đang tải...</p> : null}

            <ul className="flex flex-col gap-3">
                {roles.map((role) => {
                    const open = openName === role.name;
                    const list = members[role.name];
                    const renaming = renamingName === role.name;
                    const editing = editingName === role.name;
                    return (
                        <li key={role.name} className={CARD}>
                            <div className="flex flex-wrap items-start justify-between gap-3">
                                <button
                                    type="button"
                                    onClick={() => toggleMembers(role.name)}
                                    aria-expanded={open}
                                    className="flex min-w-0 flex-1 flex-col gap-1 text-left"
                                >
                                    <span className="font-semibold">
                                        {roleTitle(role)}
                                        <span className="ml-2 font-mono text-xs font-normal text-muted">{role.name}</span>
                                    </span>
                                    <span className="text-xs leading-relaxed text-muted">
                                        {role.permissions.length > 0
                                            ? role.permissions.map((code) => permissionLabel(code)).join(" · ")
                                            : "Chưa có quyền nào"}
                                    </span>
                                </button>
                                <div className="flex shrink-0 flex-col items-end gap-2">
                                    <span className="flex flex-col items-end gap-1 text-xs text-muted">
                                        <span>{role.globalMembers} người gán toàn trường</span>
                                        <span>{role.scopedMembers} người theo phạm vi</span>
                                    </span>
                                    <div className="flex flex-wrap justify-end gap-2">
                                        {!renaming ? (
                                            <button type="button" disabled={busy} onClick={() => startRename(role)} className={BTN_GHOST}>
                                                Đổi tên
                                            </button>
                                        ) : null}
                                        {role.name !== "ROLE_ROOT" && !editing ? (
                                            <button type="button" disabled={busy} onClick={() => startEdit(role)} className={BTN_GHOST}>
                                                Sửa quyền
                                            </button>
                                        ) : null}
                                    </div>
                                </div>
                            </div>

                            {renaming ? (
                                <div className="flex flex-wrap items-center gap-2 border-t border-line pt-3">
                                    <input
                                        aria-label="Tên vai trò hiển thị"
                                        value={nameDraft}
                                        maxLength={128}
                                        disabled={busy}
                                        onChange={(event) => setNameDraft(event.target.value)}
                                        className={`${INPUT} max-w-sm`}
                                    />
                                    <button type="button" disabled={busy} onClick={() => saveRename(role)} className={BTN_PRIMARY}>
                                        Lưu tên
                                    </button>
                                    <button type="button" disabled={busy} onClick={() => setRenamingName(null)} className={BTN_GHOST}>
                                        Hủy
                                    </button>
                                </div>
                            ) : null}

                            {editing ? (
                                <div className="flex flex-col gap-3 border-t border-line pt-3">
                                    <span className="text-xs font-semibold uppercase tracking-wide text-muted">
                                        Chọn quyền cho vai trò
                                    </span>
                                    {PERMISSION_OPTIONS.map((item) => (
                                        <label
                                            key={item.code}
                                            className="flex cursor-pointer items-start gap-3 rounded-md border border-line bg-paper p-3 text-sm transition-colors hover:bg-tint"
                                        >
                                            <input
                                                type="checkbox"
                                                checked={editSelected.includes(item.code)}
                                                disabled={busy}
                                                onChange={() => toggleEditPermission(item.code)}
                                                className="mt-1 size-4 shrink-0 accent-primary"
                                            />
                                            <span className="flex min-w-0 flex-col">
                                                <span className="font-medium">{item.label}</span>
                                                <span className="text-xs leading-relaxed text-muted">{item.description}</span>
                                            </span>
                                        </label>
                                    ))}
                                    <div className="flex flex-wrap gap-2">
                                        <button type="button" disabled={busy} onClick={() => saveEdit(role)} className={BTN_PRIMARY}>
                                            Lưu quyền
                                        </button>
                                        <button type="button" disabled={busy} onClick={() => setEditingName(null)} className={BTN_GHOST}>
                                            Hủy
                                        </button>
                                    </div>
                                </div>
                            ) : null}

                            {open ? (
                                <div className="flex flex-col gap-2 border-t border-line pt-3">
                                    {!list ? (
                                        <p className="text-sm text-muted">Đang tải danh sách...</p>
                                    ) : list.length === 0 ? (
                                        <p className="text-sm text-muted">Chưa có ai có vai trò này.</p>
                                    ) : (
                                        list.map((member) => (
                                            <div
                                                key={`${member.source}-${member.grantId ?? member.userId}`}
                                                className="flex flex-wrap items-center justify-between gap-2 text-sm"
                                            >
                                                <span>
                                                    {member.name}{" "}
                                                    <span className="text-muted">(@{member.username})</span>
                                                </span>
                                                <span className="text-xs text-muted">{describeMember(member)}</span>
                                            </div>
                                        ))
                                    )}
                                </div>
                            ) : null}
                        </li>
                    );
                })}
            </ul>
        </section>
    );
}