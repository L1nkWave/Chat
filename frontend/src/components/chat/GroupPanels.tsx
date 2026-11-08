"use client";

import { useState, type FormEvent } from "react";
import { chatApi, errorMessage } from "@/lib/chat/api";
import type { Invitation, Member } from "@/lib/chat/types";

const INPUT =
    "h-10 min-w-0 w-full rounded-md border border-line bg-paper px-3 text-sm text-ink outline-none transition-colors placeholder:text-muted focus:border-primary";
const BTN_PRIMARY =
    "h-10 shrink-0 rounded-md bg-primary px-4 text-sm font-medium text-primary-ink transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-40";
const BTN_GHOST =
    "h-10 shrink-0 rounded-md border border-line px-4 text-sm text-ink transition-colors hover:bg-tint disabled:cursor-not-allowed disabled:opacity-40";
const LABEL = "text-xs font-semibold uppercase tracking-wide text-muted";

function initial(name: string): string {
    return name.trim().charAt(0).toUpperCase() || "?";
}

type CreateGroupFormProps = {
    onCreated: (groupId: string) => void;
    onCancel: () => void;
    onError: (text: string) => void;
};

export function CreateGroupForm({ onCreated, onCancel, onError }: CreateGroupFormProps) {
    const [title, setTitle] = useState("");
    const [busy, setBusy] = useState(false);

    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        const name = title.trim();
        if (name.length < 3 || name.length > 64) {
            onError("Tên nhóm cần từ 3 đến 64 ký tự.");
            return;
        }
        setBusy(true);
        try {
            const created = await chatApi.createGroup(name);
            onCreated(created.id);
        } catch (err) {
            onError(errorMessage(err));
        } finally {
            setBusy(false);
        }
    }

    return (
        <form onSubmit={submit} className="flex flex-col gap-2 border-b border-line px-4 py-4">
            <label htmlFor="group-title" className={LABEL}>
                Tên nhóm mới
            </label>
            <input
                id="group-title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={64}
                placeholder="Ví dụ: Nhóm đồ án DCT1222"
                className={INPUT}
            />
            <div className="flex justify-end gap-2 pt-1">
                <button type="button" onClick={onCancel} className={BTN_GHOST}>
                    Hủy
                </button>
                <button type="submit" disabled={busy} className={BTN_PRIMARY}>
                    {busy ? "Đang tạo..." : "Tạo nhóm"}
                </button>
            </div>
        </form>
    );
}

type InvitationListProps = {
    invitations: Invitation[];
    onChanged: () => void;
    onError: (text: string) => void;
};

export function InvitationList({ invitations, onChanged, onError }: InvitationListProps) {
    const [busyId, setBusyId] = useState<string | null>(null);

    async function respond(id: string, accept: boolean) {
        setBusyId(id);
        try {
            if (accept) await chatApi.acceptInvitation(id);
            else await chatApi.declineInvitation(id);
            onChanged();
        } catch (err) {
            onError(errorMessage(err));
        } finally {
            setBusyId(null);
        }
    }

    return (
        <section aria-label="Lời mời vào nhóm" className="flex flex-col gap-2 border-b border-line px-4 py-4">
            <h3 className={LABEL}>Lời mời ({invitations.length})</h3>
            <ul className="flex flex-col gap-2">
                {invitations.map((inv) => (
                    <li key={inv.id} className="flex flex-col gap-3 rounded-md border border-line bg-paper p-3">
                        <p className="text-sm leading-relaxed">
                            <span className="font-medium">{inv.inviter}</span> mời bạn vào nhóm{" "}
                            <span className="font-medium">{inv.groupTitle}</span>
                        </p>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                disabled={busyId === inv.id}
                                onClick={() => respond(inv.id, true)}
                                className={`${BTN_PRIMARY} h-9 flex-1`}
                            >
                                Chấp nhận
                            </button>
                            <button
                                type="button"
                                disabled={busyId === inv.id}
                                onClick={() => respond(inv.id, false)}
                                className={`${BTN_GHOST} h-9 flex-1`}
                            >
                                Từ chối
                            </button>
                        </div>
                    </li>
                ))}
            </ul>
        </section>
    );
}

type MembersPanelProps = {
    groupId: string;
    currentUserId: string;
    members: Member[];
    isOwner: boolean;
    onLeft: () => void;
    onChanged: () => void;
    onNotice: (text: string, tone: "info" | "error") => void;
};

export function MembersPanel({
                                 groupId,
                                 members,
                                 isOwner,
                                 onLeft,
                                 onChanged,
                                 onNotice,
                                 currentUserId,
                             }: MembersPanelProps) {
    const [username, setUsername] = useState("");
    const [newTitle, setNewTitle] = useState("");
    const [busy, setBusy] = useState(false);

    async function invite(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        const target = username.trim();
        if (!target) return;
        setBusy(true);
        try {
            await chatApi.inviteToGroup(groupId, target);
            setUsername("");
            onNotice(`Đã gửi lời mời tới ${target}. Người này cần chấp nhận để vào nhóm.`, "info");
        } catch (err) {
            onNotice(errorMessage(err), "error");
        } finally {
            setBusy(false);
        }
    }

    async function rename(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        const title = newTitle.trim();
        if (title.length < 3 || title.length > 64) {
            onNotice("Tên nhóm cần từ 3 đến 64 ký tự.", "error");
            return;
        }
        setBusy(true);
        try {
            await chatApi.renameGroup(groupId, title);
            setNewTitle("");
            onNotice("Đã đổi tên nhóm.", "info");
        } catch (err) {
            onNotice(errorMessage(err), "error");
        } finally {
            setBusy(false);
        }
    }

    async function remove(member: Member) {
        if (!window.confirm(`Xóa ${member.name} khỏi nhóm?`)) return;
        setBusy(true);
        try {
            await chatApi.removeMember(groupId, member.userId);
            onNotice(`Đã xóa ${member.name} khỏi nhóm.`, "info");
            onChanged();
        } catch (err) {
            onNotice(errorMessage(err), "error");
        } finally {
            setBusy(false);
        }
    }

    async function changeRole(member: Member, role: "MEMBER" | "ADMIN") {
        const question =
            role === "ADMIN"
                ? `Đặt ${member.name} làm quản trị viên?`
                : `Gỡ quyền quản trị của ${member.name}?`;
        if (!window.confirm(question)) return;
        setBusy(true);
        try {
            await chatApi.changeRole(groupId, member.userId, role);
            onNotice(
                role === "ADMIN"
                    ? `Đã đặt ${member.name} làm quản trị viên.`
                    : `Đã gỡ quyền quản trị của ${member.name}.`,
                "info",
            );
            onChanged();
        } catch (err) {
            onNotice(errorMessage(err), "error");
        } finally {
            setBusy(false);
        }
    }

    async function leave() {
        if (!window.confirm("Bạn có chắc muốn rời nhóm này?")) return;
        setBusy(true);
        try {
            await chatApi.leaveGroup(groupId);
            onLeft();
        } catch (err) {
            onNotice(errorMessage(err), "error");
            setBusy(false);
        }
    }

    return (
        <section
            aria-label="Thành viên nhóm"
            className="flex max-h-80 flex-col gap-5 overflow-y-auto border-b border-line bg-surface px-4 py-4"
        >
            <div className="flex flex-col gap-2">
                <h3 className={LABEL}>Thành viên ({members.length})</h3>
                <ul className="flex flex-col gap-0.5">
                    {members.map((m) => (
                        <li
                            key={m.userId}
                            className="flex items-center justify-between gap-2 rounded-md px-2 py-2 transition-colors hover:bg-paper"
                        >
                            <span className="flex min-w-0 items-center gap-2.5">
                                <span
                                    aria-hidden="true"
                                    className="grid size-8 shrink-0 place-items-center rounded-md bg-tint text-sm font-semibold text-primary"
                                >
                                    {initial(m.name)}
                                </span>
                                <span className="min-w-0">
                                    <span className="block truncate text-sm">
                                        {m.name}
                                        {m.userId === currentUserId ? (
                                            <span className="text-muted"> (bạn)</span>
                                        ) : null}
                                    </span>
                                    {m.isOwner ? (
                                        <span className="text-xs text-primary">Quản trị viên</span>
                                    ) : null}
                                </span>
                            </span>

                            {isOwner && m.userId !== currentUserId ? (
                                <span className="flex shrink-0 items-center gap-3 text-xs">
                                    {!m.isOwner ? (
                                        <button
                                            type="button"
                                            onClick={() => changeRole(m, "ADMIN")}
                                            disabled={busy}
                                            className="text-primary underline-offset-4 hover:underline disabled:opacity-40"
                                        >
                                            Làm quản trị
                                        </button>
                                    ) : (
                                        <button
                                            type="button"
                                            onClick={() => changeRole(m, "MEMBER")}
                                            disabled={busy}
                                            className="text-muted underline-offset-4 hover:text-ink hover:underline disabled:opacity-40"
                                        >
                                            Gỡ quyền
                                        </button>
                                    )}
                                    {!m.isOwner ? (
                                        <button
                                            type="button"
                                            onClick={() => remove(m)}
                                            disabled={busy}
                                            className="text-danger underline-offset-4 hover:underline disabled:opacity-40"
                                        >
                                            Xóa
                                        </button>
                                    ) : null}
                                </span>
                            ) : null}
                        </li>
                    ))}
                </ul>
            </div>

            <form onSubmit={invite} className="flex flex-col gap-2">
                <label htmlFor="invite-username" className={LABEL}>
                    Mời thành viên
                </label>
                <div className="flex gap-2">
                    <input
                        id="invite-username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="Tên đăng nhập"
                        autoCapitalize="none"
                        spellCheck={false}
                        className={INPUT}
                    />
                    <button type="submit" disabled={busy || !username.trim()} className={BTN_PRIMARY}>
                        Mời
                    </button>
                </div>
            </form>

            {isOwner ? (
                <form onSubmit={rename} className="flex flex-col gap-2">
                    <label htmlFor="rename-group" className={LABEL}>
                        Đổi tên nhóm
                    </label>
                    <div className="flex gap-2">
                        <input
                            id="rename-group"
                            value={newTitle}
                            onChange={(e) => setNewTitle(e.target.value)}
                            placeholder="Tên nhóm mới"
                            maxLength={64}
                            className={INPUT}
                        />
                        <button type="submit" disabled={busy || !newTitle.trim()} className={BTN_GHOST}>
                            Đổi
                        </button>
                    </div>
                </form>
            ) : null}

            <div className="border-t border-line pt-4">
                {isOwner ? (
                    <p className="text-xs text-muted">Bạn là chủ nhóm nên không thể rời nhóm.</p>
                ) : (
                    <button
                        type="button"
                        onClick={leave}
                        disabled={busy}
                        className="text-sm text-danger underline-offset-4 hover:underline disabled:opacity-40"
                    >
                        Rời nhóm
                    </button>
                )}
            </div>
        </section>
    );
}