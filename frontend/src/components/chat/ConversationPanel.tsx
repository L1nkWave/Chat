"use client";

import { useState } from "react";
import { CATEGORY_LABEL } from "@/lib/chat/filters";
import type { Conversation, Member } from "@/lib/chat/types";
import { MediaPanel } from "./MediaPanel";
import { MembersPanel } from "./GroupPanels";
import { HELP, SECTION_TITLE } from "@/components/ui/styles";

type PanelTab = "info" | "media";

const TABS: { id: PanelTab; label: string }[] = [
    { id: "info", label: "Thông tin" },
    { id: "media", label: "Ảnh & file" },
];

type ConversationPanelProps = {
    conversation: Conversation;
    /** null khi danh sách thành viên đang tải */
    members: Member[] | null;
    currentUserId: string;
    isOwner: boolean;
    onClose: () => void;
    onLeft: () => void;
    onMembersChanged: () => void;
    onNotice: (text: string, tone: "info" | "error") => void;
    onDeleteGroup: () => void;
    /** Tăng khi có tin file mới hoặc tin bị xóa, để tab Ảnh & file tải lại */
    mediaVersion: number;
    onRemoveMessage: (messageId: string) => void;
};

/** Panel bên phải (trên điện thoại là ngăn kéo): tab Thông tin và tab Ảnh & file */
export function ConversationPanel({
    conversation,
    members,
    currentUserId,
    isOwner,
    onClose,
    onLeft,
    onMembersChanged,
    onNotice,
    onDeleteGroup,
    mediaVersion,
    onRemoveMessage,
}: ConversationPanelProps) {
    const [tab, setTab] = useState<PanelTab>("info");
    const group = conversation.kind === "group";

    return (
        <div className="flex h-full min-h-0 w-full flex-col bg-surface">
            <div className="flex h-16 shrink-0 items-center justify-between gap-2 border-b border-line bg-surface px-4">
                <h2 className={SECTION_TITLE}>Thông tin cuộc trò chuyện</h2>
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Đóng thông tin"
                    className="grid size-8 shrink-0 place-items-center rounded-md text-muted transition-colors hover:bg-tint hover:text-ink"
                >
                    <svg viewBox="0 0 24 24" width={16} height={16} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" aria-hidden="true">
                        <path d="M18 6L6 18M6 6l12 12" />
                    </svg>
                </button>
            </div>

            <div role="tablist" aria-label="Nội dung thông tin" className="flex gap-1 border-b border-line px-3 py-2">
                {TABS.map((t) => (
                    <button
                        key={t.id}
                        type="button"
                        role="tab"
                        aria-selected={tab === t.id}
                        onClick={() => setTab(t.id)}
                        className="h-8 flex-1 rounded-md px-3 text-sm text-muted transition-colors hover:bg-paper hover:text-ink aria-selected:bg-tint aria-selected:font-medium aria-selected:text-primary"
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            <div role="tabpanel" className="min-h-0 flex-1 overflow-y-auto">
                {tab === "info" ? (
                    <div className="flex flex-col">
                        <div className="flex flex-col gap-4 p-4">
                            <div className="flex items-center gap-3">
                                <span
                                    aria-hidden="true"
                                    className={`grid size-11 shrink-0 place-items-center rounded-md text-base font-semibold ${
                                        group ? "bg-primary text-primary-ink" : "bg-tint text-primary"
                                    }`}
                                >
                                    {conversation.title.trim().charAt(0).toUpperCase() || "?"}
                                </span>
                                <div className="min-w-0">
                                    <p className="truncate text-base font-semibold">{conversation.title}</p>
                                    <p className="text-xs text-muted">
                                        {group ? "Nhóm trò chuyện" : "Trò chuyện trực tiếp"}
                                    </p>
                                </div>
                            </div>
                            <dl className="grid grid-cols-2 gap-3 text-sm">
                                <div className="flex flex-col gap-0.5">
                                    <dt className={HELP}>Loại</dt>
                                    <dd>{CATEGORY_LABEL[conversation.category]}</dd>
                                </div>
                                {group ? (
                                    <div className="flex flex-col gap-0.5">
                                        <dt className={HELP}>Thành viên</dt>
                                        <dd>{members ? `${members.length} người` : "Đang tải..."}</dd>
                                    </div>
                                ) : null}
                            </dl>
                        </div>

                        {group ? (
                            members ? (
                                <MembersPanel
                                    groupId={conversation.id}
                                    members={members}
                                    isOwner={isOwner}
                                    currentUserId={currentUserId}
                                    onLeft={onLeft}
                                    onChanged={onMembersChanged}
                                    onNotice={onNotice}
                                    onDeleteGroup={onDeleteGroup}
                                />
                            ) : (
                                <p className="border-t border-line px-4 py-3 text-sm text-muted">Đang tải thành viên...</p>
                            )
                        ) : (
                            <p className="px-4 pb-4 text-sm text-muted">Trò chuyện riêng giữa hai người.</p>
                        )}
                    </div>
                ) : (
                    <MediaPanel
                        chatId={conversation.id}
                        version={mediaVersion}
                        currentUserId={currentUserId}
                        canModerate={group && isOwner}
                        onRemove={onRemoveMessage}
                    />
                )}
            </div>
        </div>
    );
}
