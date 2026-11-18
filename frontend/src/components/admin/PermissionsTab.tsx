"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage, grantRole, revokeGrant } from "@/lib/admin/api";
import type { ScopeType } from "@/lib/admin/api";
import { getUserDetail } from "@/lib/admin/users-api";
import type { ScopedGrant, UserDetail } from "@/lib/admin/users-api";
import { ROLE_LABEL, SCOPE_LABEL } from "./labels";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";
import { EntityPicker } from "./EntityPicker";
import type { PickerOption } from "./EntityPicker";
import { searchClasses, searchFaculties, searchPeople } from "./pickers";

type Feedback = { text: string; tone: "info" | "error" };

const GRANTABLE_ROLES: { value: string; label: string }[] = [
    { value: "ROLE_ADMIN", label: ROLE_LABEL.ROLE_ADMIN },
    { value: "ROLE_LECTURER", label: ROLE_LABEL.ROLE_LECTURER },
    { value: "ROLE_STUDENT", label: ROLE_LABEL.ROLE_STUDENT },
];

/** Cấp vai trò theo phạm vi và thu hồi. Chỉ ROOT. */
export function PermissionsTab() {
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    // Cấp quyền
    const [user, setUser] = useState<PickerOption | null>(null);
    const [role, setRole] = useState("ROLE_ADMIN");
    const [scope, setScope] = useState<ScopeType>("SCHOOL");
    const [scopeTarget, setScopeTarget] = useState<PickerOption | null>(null);

    // Thu hồi: chọn người dùng, rồi thu hồi từng lần cấp đang có
    const [revokeUser, setRevokeUser] = useState<PickerOption | null>(null);
    const [revokeDetail, setRevokeDetail] = useState<UserDetail | null>(null);
    const [revokeLoading, setRevokeLoading] = useState(false);
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        if (!revokeUser) {
            setRevokeDetail(null);
            return;
        }
        let alive = true;
        setRevokeLoading(true);
        getUserDetail(revokeUser.id)
            .then((data) => {
                if (alive) setRevokeDetail(data);
            })
            .catch((err: unknown) => {
                if (alive) {
                    setRevokeDetail(null);
                    setNotice({ text: adminErrorMessage(err), tone: "error" });
                }
            })
            .finally(() => {
                if (alive) setRevokeLoading(false);
            });
        return () => {
            alive = false;
        };
    }, [revokeUser, reloadKey]);

    async function run(action: () => Promise<void>, success: string): Promise<boolean> {
        setBusy(true);
        setNotice(null);
        try {
            await action();
            setNotice({ text: success, tone: "info" });
            return true;
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
            return false;
        } finally {
            setBusy(false);
        }
    }

    async function submitGrant(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!user) {
            setNotice({ text: "Chọn người dùng cần cấp quyền.", tone: "error" });
            return;
        }
        let scopeValue: number | null = null;
        if (scope !== "SCHOOL") {
            if (!scopeTarget) {
                setNotice({
                    text: scope === "FACULTY" ? "Chọn khoa cần cấp quyền." : "Chọn lớp cần cấp quyền.",
                    tone: "error",
                });
                return;
            }
            scopeValue = scopeTarget.id;
        }
        const label = ROLE_LABEL[role] ?? role;
        const ok = await run(
            () => grantRole(user.id, role, scope, scopeValue),
            `Đã cấp ${label} cho ${user.label}.`,
        );
        if (ok) {
            setScopeTarget(null);
            setReloadKey((key) => key + 1);
        }
    }

    async function revoke(grant: ScopedGrant) {
        const label = `${ROLE_LABEL[grant.role] ?? grant.role} · ${SCOPE_LABEL[grant.scopeType]}${
            grant.scopeId ? ` #${grant.scopeId}` : ""
        }`;
        if (!window.confirm(`Thu hồi "${label}" của ${revokeUser?.label ?? "người này"}?`)) return;
        const ok = await run(() => revokeGrant(grant.grantId), "Đã thu hồi quyền.");
        if (ok) setReloadKey((key) => key + 1);
    }

    const scopedGrants = revokeDetail?.scopedRoles ?? [];

    return (
        <section aria-label="Phân quyền" className="flex flex-col gap-5">
            <h2 className="font-serif text-lg font-semibold">Phân quyền</h2>
            <p className="text-sm text-muted">
                Cấp vai trò theo phạm vi: toàn trường, khoa hoặc lớp. Mọi thao tác được ghi nhật ký.
                Vai trò toàn trường (sinh viên, giảng viên, quản trị viên) đổi ở tab Người dùng.
            </p>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <form onSubmit={submitGrant} className={CARD}>
                <h3 className="text-sm font-semibold">Cấp vai trò cho người dùng</h3>
                <EntityPicker
                    id="grant-user"
                    label="Người dùng"
                    placeholder="Nhập tên hoặc tên đăng nhập"
                    search={searchPeople}
                    value={user}
                    onChange={setUser}
                />
                <div className="flex flex-col gap-1">
                    <label htmlFor="grant-role" className={LABEL}>Vai trò</label>
                    <select
                        id="grant-role"
                        value={role}
                        disabled={busy}
                        onChange={(event) => setRole(event.target.value)}
                        className={INPUT}
                    >
                        {GRANTABLE_ROLES.map((item) => (
                            <option key={item.value} value={item.value}>{item.label}</option>
                        ))}
                    </select>
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor="grant-scope" className={LABEL}>Phạm vi</label>
                    <select
                        id="grant-scope"
                        value={scope}
                        disabled={busy}
                        onChange={(event) => {
                            setScope(event.target.value as ScopeType);
                            setScopeTarget(null);
                        }}
                        className={INPUT}
                    >
                        <option value="SCHOOL">{SCOPE_LABEL.SCHOOL}</option>
                        <option value="FACULTY">{SCOPE_LABEL.FACULTY}</option>
                        <option value="CLASS">{SCOPE_LABEL.CLASS}</option>
                    </select>
                </div>
                {scope === "FACULTY" ? (
                    <EntityPicker
                        id="grant-scope-target"
                        label="Khoa"
                        placeholder="Nhập tên hoặc mã khoa"
                        search={searchFaculties}
                        value={scopeTarget}
                        onChange={setScopeTarget}
                    />
                ) : null}
                {scope === "CLASS" ? (
                    <EntityPicker
                        id="grant-scope-target"
                        label="Lớp"
                        placeholder="Nhập tên hoặc mã lớp"
                        search={searchClasses}
                        value={scopeTarget}
                        onChange={setScopeTarget}
                    />
                ) : null}
                <button type="submit" disabled={busy} className={`${BTN_PRIMARY} self-start`}>
                    Cấp quyền
                </button>
            </form>

            <div className={CARD}>
                <h3 className="text-sm font-semibold">Thu hồi vai trò theo phạm vi</h3>
                <EntityPicker
                    id="revoke-user"
                    label="Chọn người dùng"
                    placeholder="Nhập tên hoặc tên đăng nhập"
                    search={searchPeople}
                    value={revokeUser}
                    onChange={setRevokeUser}
                />

                {!revokeUser ? (
                    <p className="text-sm text-muted">Chọn người dùng để xem các vai trò theo phạm vi đang có.</p>
                ) : revokeLoading && !revokeDetail ? (
                    <p className="text-sm text-muted">Đang tải...</p>
                ) : scopedGrants.length === 0 ? (
                    <p className="text-sm text-muted">Người này không có vai trò theo phạm vi nào.</p>
                ) : (
                    <ul className="flex flex-col divide-y divide-line rounded-md border border-line">
                        {scopedGrants.map((grant) => (
                            <li key={grant.grantId} className="flex flex-wrap items-center justify-between gap-3 p-3 text-sm">
                                <span className="flex flex-col">
                                    <span className="font-medium">{ROLE_LABEL[grant.role] ?? grant.role}</span>
                                    <span className="text-xs text-muted">
                                        {SCOPE_LABEL[grant.scopeType]}
                                        {grant.scopeId ? ` #${grant.scopeId}` : ""} · cấp lúc{" "}
                                        {new Date(grant.grantedAt).toLocaleString("vi-VN")}
                                    </span>
                                </span>
                                <button type="button" disabled={busy} onClick={() => revoke(grant)} className={BTN_GHOST}>
                                    Thu hồi
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </section>
    );
}