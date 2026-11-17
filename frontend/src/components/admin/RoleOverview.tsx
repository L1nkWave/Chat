"use client";

import { useEffect, useState } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { listRoleMembers, listRoleOverview } from "@/lib/admin/roles-api";
import type { RoleMember, RoleOverview as RoleItem } from "@/lib/admin/roles-api";
import { BTN_GHOST, CARD, PERMISSION_OPTIONS } from "./shared";

const PERMISSION_LABEL: Record<string, string> = Object.fromEntries(
    PERMISSION_OPTIONS.map((item) => [item.code, item.label]),
);

const SCOPE_LABEL: Record<"SCHOOL" | "FACULTY" | "CLASS", string> = {
    SCHOOL: "Toàn trường",
    FACULTY: "Khoa",
    CLASS: "Lớp",
};

function describeMember(member: RoleMember): string {
    if (member.source === "GLOBAL") return "Gán toàn trường";
    const scope = member.scopeType ? SCOPE_LABEL[member.scopeType] : "Phạm vi";
    return member.scopeId ? `${scope} #${member.scopeId}` : scope;
}

/** Hiển thị ở đầu tab Vai trò: mỗi vai trò có bao nhiêu người, bấm để xem danh sách. */
export function RoleOverview() {
    const [roles, setRoles] = useState<RoleItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [notice, setNotice] = useState<string | null>(null);
    const [openName, setOpenName] = useState<string | null>(null);
    const [members, setMembers] = useState<Record<string, RoleMember[]>>({});
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        let alive = true;
        listRoleOverview()
            .then((list) => {
                if (alive) setRoles(list);
            })
            .catch((err: unknown) => {
                if (alive) setNotice(adminErrorMessage(err));
            })
            .finally(() => {
                if (alive) setLoading(false);
            });
        return () => {
            alive = false;
        };
    }, [reloadKey]);

    function reload() {
        setLoading(true);
        setMembers({});
        setReloadKey((key) => key + 1);
    }

    async function toggle(name: string) {
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
            setNotice(adminErrorMessage(err));
        }
    }

    return (
        <section aria-label="Tổng quan vai trò" className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-col">
                    <h3 className="text-sm font-semibold">Tổng quan vai trò</h3>
                    <p className="text-xs text-muted">
                        Có {roles.length} vai trò. Bấm vào một vai trò để xem danh sách người có vai trò đó.
                    </p>
                </div>
                <button type="button" onClick={reload} className={BTN_GHOST}>
                    Làm mới
                </button>
            </div>

            {notice ? <Notice text={notice} tone="error" onClose={() => setNotice(null)} /> : null}
            {loading ? <p className="text-sm text-muted">Đang tải...</p> : null}

            <ul className="flex flex-col gap-3">
                {roles.map((role) => {
                    const open = openName === role.name;
                    const list = members[role.name];
                    return (
                        <li key={role.name} className={CARD}>
                            <button
                                type="button"
                                onClick={() => toggle(role.name)}
                                aria-expanded={open}
                                className="flex flex-wrap items-start justify-between gap-3 text-left"
                            >
                                <span className="flex min-w-0 flex-col gap-1">
                                    <span className="font-semibold">{role.name}</span>
                                    <span className="text-xs leading-relaxed text-muted">
                                        {role.permissions.length > 0
                                            ? role.permissions.map((code) => PERMISSION_LABEL[code] ?? code).join(" · ")
                                            : "Chưa có quyền nào"}
                                    </span>
                                </span>
                                <span className="flex shrink-0 flex-col items-end gap-1 text-xs text-muted">
                                    <span>{role.globalMembers} người gán toàn trường</span>
                                    <span>{role.scopedMembers} người theo phạm vi</span>
                                </span>
                            </button>

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