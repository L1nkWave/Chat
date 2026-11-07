"use client";

import { useState, type FormEvent } from "react";
import { chatApi, errorMessage } from "@/lib/chat/api";
import type { Invitation, Member } from "@/lib/chat/types";

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
        <form onSubmit={submit} className="space-y-2 border-b border-line p-3">
            <label htmlFor="group-title" className="text-sm font-medium">
                Tên nhóm mới
            </label>
            <input
                id="group-title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={64}
                className="h-10 w-full rounded-lg border border-line bg-paper px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-sky"
            />
            <div className="flex justify-end gap-2">
                <button
                    type="button"
                    onClick={onCancel}
                    className="rounded-lg px-3 py-1.5 text-sm text-muted hover:bg-paper"
                >
                    Hủy
                </button>
                <button
                    type="submit"
                    disabled={busy}
                    className="rounded-lg bg-primary px-3 py-1.5 text-sm text-primary-ink disabled:opacity-40"
                >
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
        <section aria-label="Lời mời vào nhóm" className="border-b border-line p-3">
            <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">
                Lời mời ({invitations.length})
            </h3>
            <ul className="space-y-2">
                {invitations.map((inv) => (
                    <li key={inv.id} className="rounded-lg bg-paper p-3">
                        <p className="text-sm">
                            <span className="font-medium">{inv.inviter}</span> mời bạn vào{" "}
                            <span className="font-medium">{inv.groupTitle}</span>
                        </p>
                        <div className="mt-2 flex gap-2">
                            <button
                                type="button"
                                disabled={busyId === inv.id}
                                onClick={() => respond(inv.id, true)}
                                className="rounded-lg bg-primary px-3 py-1.5 text-sm text-primary-ink disabled:opacity-40"
                            >
                                Chấp nhận
                            </button>
                            <button
                                type="button"
                                disabled={busyId === inv.id}
                                onClick={() => respond(inv.id, false)}
                                className="rounded-lg border border-line px-3 py-1.5 text-sm hover:bg-surface disabled:opacity-40"
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
            className="max-h-72 space-y-3 overflow-y-auto border-b border-line bg-surface px-4 py-3"
        >
            <ul className="space-y-1">
                {members.map((m) => (
                    <li key={m.userId} className="flex items-center justify-between gap-2 text-sm">
                        <span className="min-w-0 truncate">{m.name}</span>
                        <span className="flex shrink-0 items-center gap-2">
                            {m.isOwner ? (
                                <span className="rounded-full bg-sky/30 px-2 py-0.5 text-xs text-primary">
                                    Chủ nhóm
                                </span>
                            ) : null}
                            {isOwner && !m.isOwner ? (
                                <button
                                    type="button"
                                    onClick={() => remove(m)}
                                    disabled={busy}
                                    className="text-xs text-danger underline-offset-2 hover:underline disabled:opacity-40"
                                >
                                    Xóa
                                </button>
                            ) : null}
                        </span>
                    </li>
                ))}
            </ul>

            {isOwner ? (
                <form onSubmit={rename} className="flex gap-2">
                    <input
                        value={newTitle}
                        onChange={(e) => setNewTitle(e.target.value)}
                        placeholder="Tên nhóm mới"
                        aria-label="Tên nhóm mới"
                        maxLength={64}
                        className="h-9 min-w-0 flex-1 rounded-lg border border-line bg-paper px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-sky"
                    />
                    <button
                        type="submit"
                        disabled={busy || !newTitle.trim()}
                        className="h-9 rounded-lg border border-line px-3 text-sm hover:bg-paper disabled:opacity-40"
                    >
                        Đổi tên
                    </button>
                </form>
            ) : null}

            <form onSubmit={invite} className="flex gap-2">
                <input
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Tên đăng nhập cần mời"
                    aria-label="Tên đăng nhập cần mời"
                    className="h-9 min-w-0 flex-1 rounded-lg border border-line bg-paper px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-sky"
                />
                <button
                    type="submit"
                    disabled={busy || !username.trim()}
                    className="h-9 rounded-lg bg-primary px-3 text-sm text-primary-ink disabled:opacity-40"
                >
                    Mời
                </button>
            </form>

            {isOwner ? (
                <p className="text-xs text-muted">Bạn là chủ nhóm nên không thể rời nhóm.</p>
            ) : (
                <button
                    type="button"
                    onClick={leave}
                    disabled={busy}
                    className="text-sm text-danger underline-offset-2 hover:underline disabled:opacity-40"
                >
                    Rời nhóm
                </button>
            )}
        </section>
    );
}