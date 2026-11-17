"use client";

import { useState } from "react";
import type { FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage, createRole, grantRole, revokeGrant } from "@/lib/admin/api";
import type { ScopeType } from "@/lib/admin/api";
import { BTN_GHOST, BTN_PRIMARY, CARD, INPUT, LABEL, PERMISSION_OPTIONS } from "./shared";
import { RoleOverview } from "./RoleOverview";
import { EntityPicker } from "./EntityPicker";
import type { PickerOption } from "./EntityPicker";
import { searchClasses, searchFaculties, searchPeople } from "./pickers";

const CODE_PATTERN = /^[A-Z][A-Z0-9_]{2,31}$/;

type Feedback = { text: string; tone: "info" | "error" };

/** Chỉ hiển thị cho ROOT. Server vẫn kiểm tra lại quyền ở mọi thao tác. */
export function RolesTab() {
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    const [code, setCode] = useState("");
    const [selected, setSelected] = useState<string[]>([]);

    const [user, setUser] = useState<PickerOption | null>(null);
    const [role, setRole] = useState("");
    const [scope, setScope] = useState<ScopeType>("SCHOOL");
    const [scopeTarget, setScopeTarget] = useState<PickerOption | null>(null);

    const [grantId, setGrantId] = useState("");

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

    function parsePositive(value: string): number | null {
        const n = Number(value.trim());
        return Number.isInteger(n) && n > 0 ? n : null;
    }

    function togglePermission(permissionCode: string) {
        setSelected((prev) =>
            prev.includes(permissionCode)
                ? prev.filter((item) => item !== permissionCode)
                : [...prev, permissionCode],
        );
    }

    async function submitCreate(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const normalized = code.trim();
        if (!CODE_PATTERN.test(normalized)) {
            setNotice({
                text: "Mã vai trò chỉ gồm chữ hoa, số và gạch dưới, từ 3 đến 32 ký tự, bắt đầu bằng chữ.",
                tone: "error",
            });
            return;
        }
        if (selected.length === 0) {
            setNotice({ text: "Chọn ít nhất một quyền cho vai trò.", tone: "error" });
            return;
        }
        const ok = await run(
            () => createRole(normalized, selected),
            `Đã tạo vai trò ROLE_${normalized}.`,
        );
        if (ok) {
            setCode("");
            setSelected([]);
        }
    }

    async function submitGrant(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!user) {
            setNotice({ text: "Chọn người dùng cần cấp quyền.", tone: "error" });
            return;
        }
        const id = user.id;
        const roleName = role.trim();
        if (!roleName) {
            setNotice({ text: "Nhập tên vai trò đầy đủ, ví dụ ROLE_ADMIN.", tone: "error" });
            return;
        }
        let scopeValue: number | null = null;
        if (scope !== "SCHOOL") {
            if (!scopeTarget) {
                setNotice({ text: scope === "FACULTY" ? "Chọn khoa cần cấp quyền." : "Chọn lớp cần cấp quyền.", tone: "error" });
                return;
            }
            scopeValue = scopeTarget.id;
        }
        const ok = await run(
            () => grantRole(id, roleName, scope, scopeValue),
            `Đã cấp ${roleName} cho người dùng #${id}.`,
        );
        if (ok) {
            setRole("");
            setScopeTarget(null);
        }
    }

    async function submitRevoke(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const id = parsePositive(grantId);
        if (id === null) {
            setNotice({ text: "Mã lần cấp quyền phải là số nguyên dương.", tone: "error" });
            return;
        }
        if (!window.confirm(`Thu hồi lần cấp quyền #${id}?`)) return;
        const ok = await run(() => revokeGrant(id), `Đã thu hồi lần cấp quyền #${id}.`);
        if (ok) setGrantId("");
    }

    return (
        <section aria-label="Quản lý vai trò" className="flex flex-col gap-5">
            <h2 className="font-serif text-lg font-semibold">Vai trò và phân quyền</h2>
            <RoleOverview />
            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <form onSubmit={submitCreate} className={CARD}>
                <h3 className="text-sm font-semibold">Tạo vai trò mới</h3>
                <label htmlFor="role-code" className={LABEL}>
                    Mã vai trò
                </label>
                <input
                    id="role-code"
                    value={code}
                    onChange={(event) => setCode(event.target.value.toUpperCase())}
                    placeholder="Ví dụ: GIANG_VIEN"
                    spellCheck={false}
                    className={INPUT}
                />

                <fieldset className="flex flex-col gap-2">
                    <legend className={LABEL}>Quyền của vai trò</legend>
                    {PERMISSION_OPTIONS.map((item) => (
                        <label
                            key={item.code}
                            className="flex cursor-pointer items-start gap-3 rounded-md border border-line bg-paper p-3 text-sm transition-colors hover:bg-tint"
                        >
                            <input
                                type="checkbox"
                                checked={selected.includes(item.code)}
                                onChange={() => togglePermission(item.code)}
                                className="mt-1 size-4 shrink-0 accent-primary"
                            />
                            <span className="flex min-w-0 flex-col">
                                <span className="font-medium">{item.label}</span>
                                <span className="text-xs leading-relaxed text-muted">{item.description}</span>
                            </span>
                        </label>
                    ))}
                </fieldset>

                <button type="submit" disabled={busy || !code.trim()} className={`${BTN_PRIMARY} self-start`}>
                    Tạo vai trò
                </button>
            </form>

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
                <label htmlFor="grant-role" className={LABEL}>
                    Tên vai trò đầy đủ
                </label>
                <input
                    id="grant-role"
                    value={role}
                    onChange={(event) => setRole(event.target.value.toUpperCase())}
                    placeholder="Ví dụ: ROLE_ADMIN"
                    spellCheck={false}
                    className={INPUT}
                />
                <label htmlFor="grant-scope" className={LABEL}>
                    Phạm vi
                </label>
                <select
                    id="grant-scope"
                    value={scope}
                    onChange={(event) => {
                        setScope(event.target.value as ScopeType);
                        setScopeTarget(null);
                    }}
                    className={INPUT}
                >
                    <option value="SCHOOL">Toàn trường</option>
                    <option value="FACULTY">Khoa</option>
                    <option value="CLASS">Lớp</option>
                </select>
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

            <form onSubmit={submitRevoke} className={CARD}>
                <h3 className="text-sm font-semibold">Thu hồi lần cấp quyền</h3>
                <label htmlFor="revoke-grant" className={LABEL}>
                    Mã lần cấp quyền
                </label>
                <div className="flex gap-2">
                    <input
                        id="revoke-grant"
                        inputMode="numeric"
                        value={grantId}
                        onChange={(event) => setGrantId(event.target.value)}
                        className={INPUT}
                    />
                    <button type="submit" disabled={busy || !grantId.trim()} className={BTN_GHOST}>
                        Thu hồi
                    </button>
                </div>
            </form>
        </section>
    );
}