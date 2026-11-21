import { BOT_USERNAME, timeValue } from "@/lib/chat/sort";
import type { Attachment, ChatCategory, Conversation, Member, Message } from "./types";

// Kiểu JSON đúng như chat-service trả về
export type BackendCompanion = {
    id: number;
    username: string;
    name: string;
    online: boolean;
    deleted: boolean;
};

export type BackendMessage = {
    id: string;
    action: "MESSAGE" | "FILE" | "POLL" | "FORWARD" | "REMOVE" | "JOIN" | "LEAVE" | "ADD" | "KICK";
    createdAt: string;
    author?: { id: number; username: string; name: string; deleted?: boolean | null } | null;
    text?: string;
    isEdited?: boolean;
    filename?: string;
    contentType?: string;
    size?: number;
    edited?: boolean;
    editedAt?: string;
    removedAt?: string | null;
};

export type BackendChat = {
    id: string;
    type: "DUO" | "GROUP";
    createdAt: string;
    lastMessage?: BackendMessage | null;
    user?: BackendCompanion | null;
    name?: string | null;
    category?: ChatCategory | null;
    /** Đã ghim theo người đang đăng nhập */
    pinned?: boolean | null;
    pinnedAt?: string | null;
};

export type BackendMember = {
    id: number;
    role: "MEMBER" | "ADMIN";
    joinedAt: string;
    details: { username: string; name: string; isOnline: boolean } | null;
};

export type BackendGroupDetails = {
    description?: string | null;
    members: BackendMember[];
    membersLimit: number;
    isPrivate: boolean;
};

// Chỉ hiển thị tin nhắn văn bản và tệp; tin hệ thống (vào/rời nhóm) bỏ qua tạm thời
const SHOWN_ACTIONS: BackendMessage["action"][] = ["MESSAGE", "FILE"];

// Tin hệ thống không có nội dung, nên dùng câu mô tả ngắn thay vì để trống
const SYSTEM_PREVIEW: Partial<Record<BackendMessage["action"], string>> = {
    JOIN: "Có thành viên mới tham gia nhóm",
    ADD: "Có thành viên được thêm vào nhóm",
    LEAVE: "Có thành viên đã rời nhóm",
    KICK: "Có thành viên bị xóa khỏi nhóm",
};

function previewOf(message?: BackendMessage | null): string {
    if (!message) return "";
    if (message.action === "FILE") return `📎 ${message.filename ?? "Tệp đính kèm"}`;
    if (SYSTEM_PREVIEW[message.action]) return SYSTEM_PREVIEW[message.action] as string;
    return message.text ?? "";
}

export function toConversation(chat: BackendChat): Conversation {
    const isGroup = chat.type === "GROUP";
    const title = isGroup
        ? chat.name || "Nhóm"
        : chat.user?.name || chat.user?.username || "Người dùng";
    const isBot = !isGroup && chat.user?.username === BOT_USERNAME;

    return {
        id: chat.id,
        title,
        kind: isGroup ? "group" : "direct",
        category: chat.category ?? (isGroup ? "GROUP" : "PRIVATE"),
        lastMessage: previewOf(chat.lastMessage),
        lastTime: chat.lastMessage?.createdAt ?? chat.createdAt,
        unread: 0,
        // Chat bot luôn nằm trong mục ghim, nhưng không đổi được trạng thái ghim
        pinned: chat.pinned === true || isBot,
        pinnedAt: chat.pinnedAt ?? null,
        isBot,
    };
}

export function toMessage(message: BackendMessage, conversationId: string): Message | null {
    if (!SHOWN_ACTIONS.includes(message.action)) return null;

    const attachment: Attachment | null =
        message.action === "FILE"
            ? {
                id: message.id,
                filename: message.filename ?? "tệp",
                contentType: message.contentType ?? "application/octet-stream",
                size: message.size ?? 0,
            }
            : null;

    return {
        id: message.id,
        conversationId,
        senderId: message.author ? String(message.author.id) : "",
        senderName: message.author?.name || message.author?.username || "",
        text: message.removedAt ? "" : message.action === "MESSAGE" ? message.text ?? "" : "",
        edited: message.edited ?? false,
        editedAt: message.editedAt,
        removedAt: message.removedAt || undefined,
        time: message.createdAt,
        attachment,
    };
}

// Backend có thể trả tin mới nhất trước; sắp xếp lại theo thời gian tăng dần
export function toMessages(list: BackendMessage[], conversationId: string): Message[] {
    return list
        .map((message) => toMessage(message, conversationId))
        .filter((message): message is Message => message !== null)
        .sort((a, b) => timeValue(a.time) - timeValue(b.time));
}

export function toMembers(details: BackendGroupDetails): Member[] {
    return details.members.map((member) => ({
        userId: String(member.id),
        name: member.details?.name || member.details?.username || String(member.id),
        isOwner: member.role === "ADMIN",
    }));
}