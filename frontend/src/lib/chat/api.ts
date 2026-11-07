import { ApiError, apiFetch } from "@/lib/api/client";
import type { Conversation, Invitation, Member, Message } from "./types";

export function errorMessage(err: unknown): string {
    return err instanceof ApiError ? err.message : "Đã có lỗi xảy ra. Vui lòng thử lại.";
}

const enc = encodeURIComponent;

export const chatApi = {
    listConversations: () =>
        apiFetch<Conversation[]>("/api/v1/chat/conversations"),

    listMessages: (conversationId: string) =>
        apiFetch<Message[]>(`/api/v1/chat/conversations/${enc(conversationId)}/messages`),

    listMembers: (conversationId: string) =>
        apiFetch<Member[]>(`/api/v1/chat/conversations/${enc(conversationId)}/members`),

    createGroup: (title: string) =>
        apiFetch<Conversation>("/api/v1/chat/groups", {
            method: "POST",
            body: JSON.stringify({ title }),
        }),

    inviteToGroup: (groupId: string, username: string) =>
        apiFetch<{ id: string }>(`/api/v1/chat/groups/${enc(groupId)}/invitations`, {
            method: "POST",
            body: JSON.stringify({ username }),
        }),

    removeMember: (groupId: string, userId: string) =>
        apiFetch<void>(
            `/api/v1/chat/groups/${enc(groupId)}/members/${enc(userId)}`,
            { method: "DELETE" },
        ),

    leaveGroup: (groupId: string) =>
        apiFetch<void>(`/api/v1/chat/groups/${enc(groupId)}/leave`, { method: "POST" }),

    listInvitations: () => apiFetch<Invitation[]>("/api/v1/chat/invitations"),

    acceptInvitation: (id: string) =>
        apiFetch<void>(`/api/v1/chat/invitations/${enc(id)}/accept`, { method: "POST" }),

    declineInvitation: (id: string) =>
        apiFetch<void>(`/api/v1/chat/invitations/${enc(id)}/decline`, { method: "POST" }),
};