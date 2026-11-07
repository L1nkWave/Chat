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

    listInvitations: () => apiFetch<Invitation[]>(`${BASE}/invitations`),

    acceptInvitation: (id: string) =>
        apiFetch<void>(`${BASE}/invitations/${enc(id)}/accept`, { method: "POST" }),

    declineInvitation: (id: string) =>
        apiFetch<void>(`${BASE}/invitations/${enc(id)}/decline`, { method: "POST" }),
};