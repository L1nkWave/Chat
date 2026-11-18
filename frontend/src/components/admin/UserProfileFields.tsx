"use client";

import { GENDER_LABEL, STATUS_OPTIONS_BY_ROLE } from "./labels";
import { BTN_GHOST, INPUT, LABEL } from "./shared";
import type { ProfilePayload } from "@/lib/admin/users-api";

/**
 * Bản nháp của form hồ sơ.
 * Trường thường là chuỗi (rỗng nghĩa là để trống).
 * Trường nhạy cảm (cccd, ethnicity, religion): null nghĩa là không đổi, chuỗi rỗng nghĩa là xóa.
 */
export type ProfileDraft = {
    gender: string;
    birthDate: string;
    status: string;
    phone: string;
    birthplace: string;
    nationality: string;
    address: string;
    cccd: string | null;
    ethnicity: string | null;
    religion: string | null;
};

export function emptyDraft(): ProfileDraft {
    return {
        gender: "",
        birthDate: "",
        status: "",
        phone: "",
        birthplace: "",
        nationality: "Việt Nam",
        address: "",
        cccd: null,
        ethnicity: null,
        religion: null,
    };
}

/** Đổi bản nháp thành dữ liệu gửi lên máy chủ */
export function draftToPayload(draft: ProfileDraft): ProfilePayload {
    const text = (value: string) => (value.trim() === "" ? null : value.trim());
    const sensitive = (value: string | null) => (value === null ? null : value.trim());
    return {
        gender: text(draft.gender),
        birthDate: text(draft.birthDate),
        status: text(draft.status),
        phone: text(draft.phone),
        birthplace: text(draft.birthplace),
        nationality: text(draft.nationality),
        address: text(draft.address),
        cccd: sensitive(draft.cccd),
        ethnicity: sensitive(draft.ethnicity),
        religion: sensitive(draft.religion),
    };
}

type ExistingSecrets = { cccd: boolean; ethnicity: boolean; religion: boolean };

type UserProfileFieldsProps = {
    /** ROLE_STUDENT, ROLE_LECTURER hoặc ROLE_ADMIN: quyết định danh sách tình trạng */
    role: string;
    value: ProfileDraft;
    onChange: (next: ProfileDraft) => void;
    disabled?: boolean;
    /** Chỉ có ở form sửa: cho biết trường nhạy cảm đã có giá trị lưu hay chưa */
    existing?: ExistingSecrets | null;
};

export function UserProfileFields({ role, value, onChange, disabled, existing }: UserProfileFieldsProps) {
    const statusOptions = STATUS_OPTIONS_BY_ROLE[role] ?? [];

    function set<K extends keyof ProfileDraft>(key: K, next: ProfileDraft[K]) {
        onChange({ ...value, [key]: next });
    }

    function secretPlaceholder(has: boolean | undefined, example: string): string {
        if (existing === null || existing === undefined) return example;
        return has ? "Đã có. Để trống nếu không đổi" : "Chưa có";
    }

    return (
        <div className="grid gap-3 sm:grid-cols-2">
            <div className="flex flex-col gap-1">
                <label htmlFor="pf-gender" className={LABEL}>Giới tính</label>
                <select
                    id="pf-gender"
                    disabled={disabled}
                    value={value.gender}
                    onChange={(event) => set("gender", event.target.value)}
                    className={INPUT}
                >
                    <option value="">Chưa chọn</option>
                    {Object.entries(GENDER_LABEL).map(([code, label]) => (
                        <option key={code} value={code}>{label}</option>
                    ))}
                </select>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-birth" className={LABEL}>Ngày sinh</label>
                <input
                    id="pf-birth"
                    type="date"
                    disabled={disabled}
                    value={value.birthDate}
                    onChange={(event) => set("birthDate", event.target.value)}
                    className={INPUT}
                />
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-status" className={LABEL}>
                    {role === "ROLE_STUDENT" ? "Tình trạng học tập" : "Tình trạng làm việc"}
                </label>
                <select
                    id="pf-status"
                    disabled={disabled}
                    value={value.status}
                    onChange={(event) => set("status", event.target.value)}
                    className={INPUT}
                >
                    <option value="">Chọn tình trạng</option>
                    {statusOptions.map((item) => (
                        <option key={item.value} value={item.value}>{item.label}</option>
                    ))}
                </select>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-phone" className={LABEL}>Số điện thoại</label>
                <input
                    id="pf-phone"
                    inputMode="tel"
                    disabled={disabled}
                    value={value.phone}
                    placeholder="Ví dụ: 0901234567"
                    onChange={(event) => set("phone", event.target.value)}
                    className={INPUT}
                />
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-birthplace" className={LABEL}>Nơi sinh</label>
                <input
                    id="pf-birthplace"
                    disabled={disabled}
                    value={value.birthplace}
                    onChange={(event) => set("birthplace", event.target.value)}
                    className={INPUT}
                />
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-nationality" className={LABEL}>Quốc tịch</label>
                <input
                    id="pf-nationality"
                    disabled={disabled}
                    value={value.nationality}
                    onChange={(event) => set("nationality", event.target.value)}
                    className={INPUT}
                />
            </div>

            <div className="flex flex-col gap-1 sm:col-span-2">
                <label htmlFor="pf-address" className={LABEL}>Địa chỉ thường trú</label>
                <input
                    id="pf-address"
                    disabled={disabled}
                    value={value.address}
                    onChange={(event) => set("address", event.target.value)}
                    className={INPUT}
                />
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-cccd" className={LABEL}>CCCD (12 chữ số)</label>
                <div className="flex gap-2">
                    <input
                        id="pf-cccd"
                        inputMode="numeric"
                        maxLength={12}
                        disabled={disabled}
                        value={value.cccd ?? ""}
                        placeholder={secretPlaceholder(existing?.cccd, "12 chữ số")}
                        onChange={(event) => set("cccd", event.target.value.replace(/\D/g, ""))}
                        className={INPUT}
                    />
                    {existing?.cccd ? (
                        <button type="button" disabled={disabled} onClick={() => set("cccd", "")} className={BTN_GHOST}>
                            Xóa
                        </button>
                    ) : null}
                </div>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-ethnicity" className={LABEL}>Dân tộc</label>
                <div className="flex gap-2">
                    <input
                        id="pf-ethnicity"
                        disabled={disabled}
                        value={value.ethnicity ?? ""}
                        placeholder={secretPlaceholder(existing?.ethnicity, "Ví dụ: Kinh")}
                        onChange={(event) => set("ethnicity", event.target.value)}
                        className={INPUT}
                    />
                    {existing?.ethnicity ? (
                        <button type="button" disabled={disabled} onClick={() => set("ethnicity", "")} className={BTN_GHOST}>
                            Xóa
                        </button>
                    ) : null}
                </div>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="pf-religion" className={LABEL}>Tôn giáo</label>
                <div className="flex gap-2">
                    <input
                        id="pf-religion"
                        disabled={disabled}
                        value={value.religion ?? ""}
                        placeholder={secretPlaceholder(existing?.religion, "Ví dụ: Không")}
                        onChange={(event) => set("religion", event.target.value)}
                        className={INPUT}
                    />
                    {existing?.religion ? (
                        <button type="button" disabled={disabled} onClick={() => set("religion", "")} className={BTN_GHOST}>
                            Xóa
                        </button>
                    ) : null}
                </div>
            </div>
        </div>
    );
}