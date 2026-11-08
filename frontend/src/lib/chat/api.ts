import { ApiError, apiFetch } from "@/lib/api/client";
import type { Conversation, Invitation, Member, Message } from "./types";
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

    listMessages: async (conversationId: string): Promise<Message[]> => {
        const list = await apiFetch<BackendMessage[]>(
            `${BASE}/${enc(conversationId)}/messages?offset=0&limit=50`,
        );
        return toMessages(list, conversationId);
    },

    listMembers: async (conversationId: string): Promise<Member[]> => {
        return toMembers(await getGroupDetails(conversationId));
    },

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

    // Tìm người nhận theo tên đăng nhập rồi tạo (hoặc mở lại) chat riêng
    startDirectChat: async (username: string): Promise<Conversation> => {
        const users = await apiFetch<{ id: number | string; username: string }[]>(
            `/api/v1/users?username=${enc(username)}&offset=0&limit=10`,
        );
        const target = users.find((u) => u.username === username);
        if (!target) throw new ApiError(404, "Không tìm thấy người dùng này.");
        const chat = await apiFetch<BackendChat>(BASE, {
            method: "POST",
            body: JSON.stringify({ recipient: Number(target.id) }),
        });
        return toConversation(chat);
    },

    listInvitations: () => apiFetch<Invitation[]>(`${BASE}/invitations`),

    acceptInvitation: (id: string) =>
        apiFetch<void>(`${BASE}/invitations/${enc(id)}/accept`, { method: "POST" }),

    declineInvitation: (id: string) =>
        apiFetch<void>(`${BASE}/invitations/${enc(id)}/decline`, { method: "POST" }),
};