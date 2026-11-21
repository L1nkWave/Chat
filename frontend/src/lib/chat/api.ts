import { ApiError, apiFetch } from "@/lib/api/client";
import type { Conversation, Invitation, MediaItem, Member, Message, UserSuggestion } from "./types";
import {
    toConversation,
    toMembers,
    toMessages,
    type BackendChat,
    type BackendGroupDetails,
    type BackendMessage,
} from "./mappers";

export function errorMessage(err: unknown): string {
    return err instanceof ApiError ? err.message : "Đã có lỗi xảy ra. Vui lòng thử lại.";
}

const enc = encodeURIComponent;
const BASE = "/api/v1/chats";

async function getGroupDetails(groupId: string): Promise<BackendGroupDetails> {
    return apiFetch<BackendGroupDetails>(`${BASE}/${enc(groupId)}/group`);
}

export const chatApi = {
    listConversations: async (): Promise<Conversation[]> => {
        const chats = await apiFetch<BackendChat[]>(`${BASE}?offset=0&limit=50`);
        return chats.map(toConversation);
    },

    // offset tính từ tin mới nhất; dùng để tải thêm tin cũ hơn
    listMessages: async (conversationId: string, offset = 0, limit = 50): Promise<Message[]> => {
        const list = await apiFetch<BackendMessage[]>(
            `${BASE}/${enc(conversationId)}/messages?offset=${offset}&limit=${limit}`,
        );
        return toMessages(list, conversationId);
    },

    listMembers: async (conversationId: string): Promise<Member[]> => {
        return toMembers(await getGroupDetails(conversationId));
    },

    // Ghim (PUT) hoặc bỏ ghim (DELETE). Server trả 422 khi đã ghim đủ 5 cuộc trò chuyện.
    setPinned: (conversationId: string, pinned: boolean) =>
        apiFetch<void>(`${BASE}/${enc(conversationId)}/pin`, {
            method: pinned ? "PUT" : "DELETE",
        }),

    // Ảnh hoặc tệp của cuộc trò chuyện, phân trang theo offset
    listMedia: (
        conversationId: string,
        kind: "IMAGE" | "FILE",
        offset = 0,
        limit = 30,
    ): Promise<MediaItem[]> =>
        apiFetch<MediaItem[]>(
            `${BASE}/${enc(conversationId)}/media?kind=${kind}&offset=${offset}&limit=${limit}`,
        ),

    createGroup: async (title: string): Promise<Conversation> => {
        const chat = await apiFetch<BackendChat>(`${BASE}/group`, {
            method: "POST",
            body: JSON.stringify({ name: title, isPrivate: false }),
        });
        return toConversation(chat);
    },

    // Backend PATCH bắt buộc đủ các trường, nên lấy giá trị hiện tại trước
    renameGroup: async (groupId: string, name: string): Promise<void> => {
        const current = await getGroupDetails(groupId);
        await apiFetch<void>(`${BASE}/${enc(groupId)}/group`, {
            method: "PATCH",
            body: JSON.stringify({
                name,
                description: current.description ?? null,
                membersLimit: current.membersLimit,
                isPrivate: current.isPrivate,
            }),
        });
    },

    inviteToGroup: (groupId: string, username: string) =>
        apiFetch<{ id: string }>(`${BASE}/${enc(groupId)}/invitations`, {
            method: "POST",
            body: JSON.stringify({ username }),
        }),

    removeMember: (groupId: string, userId: string) =>
        apiFetch<void>(`${BASE}/${enc(groupId)}/group/members/${enc(userId)}`, {
            method: "DELETE",
        }),

    leaveGroup: (groupId: string) =>
        apiFetch<void>(`${BASE}/${enc(groupId)}/group/members`, { method: "DELETE" }),

    // Backend nhận vai trò qua query param, chỉ ADMIN hoặc chủ nhóm mới được gọi
    changeRole: (groupId: string, userId: string, role: "MEMBER" | "ADMIN") =>
        apiFetch<void>(
            `${BASE}/${enc(groupId)}/group/members/${enc(userId)}/roles?role=${role}`,
            { method: "PATCH" },
        ),

    // Gợi ý người để nhắn riêng: khớp theo tên hoặc mã/tên đăng nhập (không phân biệt hoa thường, không dấu).
    // Server đã loại chính người đang đăng nhập và các liên hệ.
    searchUsers: async (keyword: string): Promise<UserSuggestion[]> => {
        const users = await apiFetch<{ id: number | string; username: string; name: string }[]>(
            `/api/v1/users?username=${enc(keyword)}&offset=0&limit=8`,
        );
        return users.map((u) => ({ id: String(u.id), username: u.username, name: u.name }));
    },

    // Tạo (hoặc mở lại) chat riêng với người đã chọn
    startDirectChat: async (recipientId: string): Promise<Conversation> => {
        const chat = await apiFetch<BackendChat>(BASE, {
            method: "POST",
            body: JSON.stringify({ recipient: Number(recipientId) }),
        });
        return toConversation(chat);
    },

    // Lấy một đoạn chat theo id, dùng cho đoạn chat đang bị ẩn ở server
    getConversation: async (conversationId: string): Promise<Conversation> => {
        return toConversation(await apiFetch<BackendChat>(`${BASE}/generic/${enc(conversationId)}`));
    },

    reportMessage: (messageId: string, category: string, reason?: string) =>
        apiFetch<void>(`${BASE}/messages/${enc(messageId)}/report`, {
            method: "POST",
            body: JSON.stringify({ category, reason: reason ?? null }),
        }),

    listInvitations: () => apiFetch<Invitation[]>(`${BASE}/invitations`),

    acceptInvitation: (id: string) =>
        apiFetch<void>(`${BASE}/invitations/${enc(id)}/accept`, { method: "POST" }),

    declineInvitation: (id: string) =>
        apiFetch<void>(`${BASE}/invitations/${enc(id)}/decline`, { method: "POST" }),
};