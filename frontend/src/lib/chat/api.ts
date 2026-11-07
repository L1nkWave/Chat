import { ApiError, apiFetch } from "@/lib/api/client";
import type { Conversation, Invitation, Member, Message } from "./types";

export function errorMessage(err: unknown): string {
    return err instanceof ApiError ? err.message : "Đã có lỗi xảy ra. Vui lòng thử lại.";
}

const enc = encodeURIComponent;

// Đường dẫn khớp với backend chat-service (/api/v1/chats, số nhiều).
// Các hàm lời mời (invitations) chưa có ở backend, sẽ nối sau khi thêm module invitation.
export const chatApi = {
    listConversations: () =>
        apiFetch<Conversation[]>("/api/v1/chats?offset=0&limit=50"),

    listMessages: (conversationId: string) =>
        apiFetch<Message[]>(`/api/v1/chats/${enc(conversationId)}/messages`),

    listMembers: (conversationId: string) =>
        apiFetch<Member[]>(`/api/v1/chats/${enc(conversationId)}/group`),

    // Backend nhận "name" (không phải "title")
    createGroup: (title: string) =>
        apiFetch<Conversation>("/api/v1/chats/group", {
            method: "POST",
            body: JSON.stringify({ name: title, isPrivate: false }),
        }),

    // Backend PATCH yêu cầu đủ name, membersLimit (0-1000), isPrivate
    renameGroup: (groupId: string, name: string, membersLimit = 128, isPrivate = false) =>
        apiFetch<void>(`/api/v1/chats/${enc(groupId)}/group`, {
            method: "PATCH",
            body: JSON.stringify({ name, membersLimit, isPrivate }),
        }),

    // Chưa có ở backend: thêm trực tiếp không cần đồng ý, cần đổi khi có invitation
    inviteToGroup: (groupId: string, username: string) =>
        apiFetch<{ id: string }>(`/api/v1/chats/${enc(groupId)}/invitations`, {
            method: "POST",
            body: JSON.stringify({ username }),
        }),

    removeMember: (groupId: string, userId: string) =>
        apiFetch<void>(
            `/api/v1/chats/${enc(groupId)}/group/members/${enc(userId)}`,
            { method: "DELETE" },
        ),

    leaveGroup: (groupId: string) =>
        apiFetch<void>(`/api/v1/chats/${enc(groupId)}/group/members`, { method: "DELETE" }),

    // Chưa có ở backend
    listInvitations: () => apiFetch<Invitation[]>("/api/v1/chats/invitations"),

    acceptInvitation: (id: string) =>
        apiFetch<void>(`/api/v1/chats/invitations/${enc(id)}/accept`, { method: "POST" }),

    declineInvitation: (id: string) =>
        apiFetch<void>(`/api/v1/chats/invitations/${enc(id)}/decline`, { method: "POST" }),
};