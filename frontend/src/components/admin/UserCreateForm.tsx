"use client";

import { useCallback, useState } from "react";
import type { FormEvent } from "react";

import { Notice } from "@/components/ui/Notice";
import { adminErrorMessage } from "@/lib/admin/api";
import { listClasses } from "@/lib/admin/catalog-api";
import { createUser } from "@/lib/admin/users-api";
import type { CreateUserPayload } from "@/lib/admin/users-api";
import { BTN_PRIMARY, CARD, INPUT, LABEL } from "./shared";
import { ROLE_LABEL } from "./labels";
import { EntityPicker } from "./EntityPicker";
import type { PickerOption } from "./EntityPicker";
import { searchFaculties } from "./pickers";
import { draftToPayload, emptyDraft, UserProfileFields } from "./UserProfileFields";
import type { ProfileDraft } from "./UserProfileFields";

const USERNAME_PATTERN = /^[A-Za-z0-9._-]{3,32}$/;
const DEFAULT_STATUS: Record<string, string> = {
    ROLE_STUDENT: "STUDYING",
    ROLE_LECTURER: "WORKING",
    ROLE_ADMIN: "WORKING",
};

type UserCreateFormProps = {
    isRoot: boolean;
    onCreated: () => void;
};

/**
 * Tạo một tài khoản. Sinh viên chọn khoa rồi chọn lớp từ dropdown.
 * Lớp phải có trong danh mục (máy chủ kiểm tra lại).
 */
export function UserCreateForm({ isRoot, onCreated }: UserCreateFormProps) {
    const [role, setRole] = useState("ROLE_STUDENT");
    const [username, setUsername] = useState("");
    const [name, setName] = useState("");
    const [faculty, setFaculty] = useState<PickerOption | null>(null);
    const [classOption, setClassOption] = useState<PickerOption | null>(null);
    const [draft, setDraft] = useState<ProfileDraft>({ ...emptyDraft(), status: DEFAULT_STATUS.ROLE_STUDENT });
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<{ text: string; tone: "info" | "error" } | null>(null);

    const facultyId = faculty?.id ?? null;

    // Lớp chỉ hiện theo khoa đã chọn. Hàm tìm được tạo lại khi khoa đổi, để EntityPicker tải lại.
    const searchClassesInFaculty = useCallback(
        async (query: string): Promise<PickerOption[]> => {
            if (facultyId === null) return [];
            const items = await listClasses(query, facultyId);
            return items.map((item) => ({
                id: item.id,
                label: item.name,
                hint: [item.code, item.facultyName, item.cohort].filter(Boolean).join(" · "),
            }));
        },
        [facultyId],
    );

    function changeRole(next: string) {
        setRole(next);
        setDraft((prev) => ({ ...prev, status: DEFAULT_STATUS[next] ?? "" }));
        if (next !== "ROLE_STUDENT") {
            setFaculty(null);
            setClassOption(null);
        }
    }

    function classCodeOf(option: PickerOption | null): string | null {
        if (!option?.hint) return null;
        return option.hint.split(" · ")[0] ?? null;
    }

    function validate(): string | null {
        const code = username.trim();
        if (!USERNAME_PATTERN.test(code)) {
            return "Mã tài khoản chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang, từ 3 đến 32 ký tự.";
        }
        if (!name.trim()) return "Họ tên không được để trống.";
        if (role === "ROLE_STUDENT" && !classOption) return "Sinh viên phải thuộc một lớp.";
        if (!draft.status) return "Chọn tình trạng.";
        if (draft.cccd !== null && draft.cccd.length > 0 && draft.cccd.length !== 12) {
            return "CCCD phải gồm đúng 12 chữ số.";
        }
        return null;
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const problem = validate();
        if (problem) {
            setNotice({ text: problem, tone: "error" });
            return;
        }
        const payload: CreateUserPayload = {
            ...draftToPayload(draft),
            username: username.trim(),
            name: name.trim(),
            role,
            classCode: role === "ROLE_STUDENT" ? classCodeOf(classOption) : null,
            status: draft.status,
        };
        setBusy(true);
        setNotice(null);
        try {
            const created = await createUser(payload);
            setNotice({
                text: `Đã tạo tài khoản ${created.username}. Mật khẩu mặc định Dhsg@123, người dùng phải đổi khi đăng nhập lần đầu.`,
                tone: "info",
            });
            setUsername("");
            setName("");
            setFaculty(null);
            setClassOption(null);
            setDraft({ ...emptyDraft(), status: DEFAULT_STATUS[role] ?? "" });
            onCreated();
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    return (
        <form onSubmit={submit} className={CARD} aria-label="Thêm người dùng">
            <h3 className="text-sm font-semibold">Thêm người dùng</h3>

            {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

            <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1">
                    <label htmlFor="uc-role" className={LABEL}>Vai trò</label>
                    <select
                        id="uc-role"
                        value={role}
                        disabled={busy}
                        onChange={(event) => changeRole(event.target.value)}
                        className={INPUT}
                    >
                        <option value="ROLE_STUDENT">{ROLE_LABEL.ROLE_STUDENT}</option>
                        <option value="ROLE_LECTURER">{ROLE_LABEL.ROLE_LECTURER}</option>
                        {isRoot ? <option value="ROLE_ADMIN">{ROLE_LABEL.ROLE_ADMIN}</option> : null}
                    </select>
                </div>

                <div className="flex flex-col gap-1">
                    <label htmlFor="uc-username" className={LABEL}>
                        {role === "ROLE_STUDENT" ? "Mã sinh viên (tên đăng nhập)" : "Mã tài khoản (tên đăng nhập)"}
                    </label>
                    <input
                        id="uc-username"
                        value={username}
                        disabled={busy}
                        spellCheck={false}
                        placeholder={role === "ROLE_STUDENT" ? "Ví dụ: sv001" : "Ví dụ: gv001"}
                        onChange={(event) => setUsername(event.target.value.trim())}
                        className={INPUT}
                    />
                </div>

                <div className="flex flex-col gap-1">
                    <label htmlFor="uc-name" className={LABEL}>Họ và tên</label>
                    <input
                        id="uc-name"
                        value={name}
                        maxLength={64}
                        disabled={busy}
                        onChange={(event) => setName(event.target.value)}
                        className={INPUT}
                    />
                </div>

                {role === "ROLE_STUDENT" ? (
                    <>
                        <EntityPicker
                            id="uc-faculty"
                            label="Khoa"
                            placeholder="Nhập tên hoặc mã khoa"
                            search={searchFaculties}
                            value={faculty}
                            onChange={(option) => {
                                setFaculty(option);
                                setClassOption(null);
                            }}
                        />
                        <EntityPicker
                            id="uc-class"
                            label="Lớp"
                            placeholder={faculty ? "Nhập tên hoặc mã lớp" : "Chọn khoa trước"}
                            search={searchClassesInFaculty}
                            value={classOption}
                            onChange={setClassOption}
                        />
                    </>
                ) : null}
            </div>

            <UserProfileFields role={role} value={draft} onChange={setDraft} disabled={busy} />

            <div className="flex flex-wrap gap-2 pt-1">
                <button type="submit" disabled={busy} className={BTN_PRIMARY}>
                    {busy ? "Đang tạo..." : "Tạo tài khoản"}
                </button>
            </div>
        </form>
    );
}