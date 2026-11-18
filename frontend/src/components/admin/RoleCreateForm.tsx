"use client";

import { useState } from "react";
import type { FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage, createRole } from "@/lib/admin/api";
import { updateRoleDisplayName } from "@/lib/admin/roles-api";
import { BTN_PRIMARY, CARD, INPUT, LABEL, PERMISSION_OPTIONS } from "./shared";

const CODE_PATTERN = /^[A-Z][A-Z0-9_]{2,31}$/;

type RoleCreateFormProps = {
    onCreated: () => void;
};

/**
 * Tạo vai trò mới: đặt tên tiếng Việt để hiển thị, mã kỹ thuật (tự thêm ROLE_), và chọn quyền.
 * Form không có nút đóng riêng; đóng bằng nút Thêm vai trò ở đầu tab.
 */
export function RoleCreateForm({ onCreated }: RoleCreateFormProps) {
    const [displayName, setDisplayName] = useState("");
    const [code, setCode] = useState("");
    const [selected, setSelected] = useState<string[]>([]);
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<{ text: string; tone: "info" | "error" } | null>(null);

    function toggle(permissionCode: string) {
        setSelected((prev) =>
            prev.includes(permissionCode)
                ? prev.filter((item) => item !== permissionCode)
                : [...prev, permissionCode],
        );
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const name = displayName.trim();
        const normalized = code.trim();
        if (!name) {
            setNotice({ text: "Nhập tên vai trò hiển thị, ví dụ: Giảng viên khoa.", tone: "error" });
            return;
        }
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
        setBusy(true);
        setNotice(null);
        try {
            await createRole(normalized, selected);
            try {
                await updateRoleDisplayName(`ROLE_${normalized}`, name);
                setNotice({ text: `Đã tạo vai trò "${name}".`, tone: "info" });
            } catch (err) {
                setNotice({
                    text: `Đã tạo ROLE_${normalized} nhưng chưa đặt được tên hiển thị: ${adminErrorMessage(err)}`,
                    tone: "error",
                });
            }
            setDisplayName("");
            setCode("");
            setSelected([]);
            onCreated();
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <form onSubmit={submit} className={CARD} aria-label="Thêm vai trò">
            <h3 className="text-sm font-semibold">Thêm vai trò mới</h3>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor="role-display" className={LABEL}>Tên vai trò (hiển thị)</label>
                    <input
                        id="role-display"
                        value={displayName}
                        maxLength={128}
                        disabled={busy}
                        placeholder="Ví dụ: Giảng viên khoa"
                        onChange={(event) => setDisplayName(event.target.value)}
                        className={INPUT}
                    />
                </div>
                <div className="flex flex-col gap-1">
                    <label htmlFor="role-code" className={LABEL}>Mã kỹ thuật (tự thêm ROLE_)</label>
                    <input
                        id="role-code"
                        value={code}
                        disabled={busy}
                        spellCheck={false}
                        placeholder="Ví dụ: GIANG_VIEN_KHOA"
                        onChange={(event) => setCode(event.target.value.toUpperCase())}
                        className={INPUT}
                    />
                </div>
            </div>

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
                            disabled={busy}
                            onChange={() => toggle(item.code)}
                            className="mt-1 size-4 shrink-0 accent-primary"
                        />
                        <span className="flex min-w-0 flex-col">
                            <span className="font-medium">{item.label}</span>
                            <span className="text-xs leading-relaxed text-muted">{item.description}</span>
                        </span>
                    </label>
                ))}
            </fieldset>

            <button type="submit" disabled={busy || !displayName.trim() || !code.trim()} className={`${BTN_PRIMARY} self-start`}>
                {busy ? "Đang tạo..." : "Tạo vai trò"}
            </button>
        </form>
    );
}