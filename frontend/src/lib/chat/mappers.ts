import type { Attachment, Conversation, Member, Message } from "./types";

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
};

export type BackendChat = {
    id: string;
    type: "DUO" | "GROUP";
    createdAt: string;
    lastMessage?: BackendMessage | null;
    user?: BackendCompanion | null;
    name?: string | null;
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

    return {
        id: chat.id,
        title,
        kind: isGroup ? "group" : "direct",
        lastMessage: previewOf(chat.lastMessage),
        lastTime: chat.lastMessage?.createdAt ?? chat.createdAt,
        unread: 0,
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
        text: message.action === "MESSAGE" ? message.text ?? "" : "",
        time: message.createdAt,
        attachment,
    };
}

// Backend có thể trả tin mới nhất trước; sắp xếp lại theo thời gian tăng dần
export function toMessages(list: BackendMessage[], conversationId: string): Message[] {
    return list
        .map((message) => toMessage(message, conversationId))
        .filter((message): message is Message => message !== null)
        .sort((a, b) => Date.parse(a.time) - Date.parse(b.time));
}

export function toMembers(details: BackendGroupDetails): Member[] {
    return details.members.map((member) => ({
        userId: String(member.id),
        name: member.details?.name || member.details?.username || String(member.id),
        isOwner: member.role === "ADMIN",
    }));
}