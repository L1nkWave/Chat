import { instance } from "@/api/http";
import {
  ChatParams,
  FileHttpResponse,
  GroupChatDetails,
  GroupMember,
  MessageParams,
} from "@/api/http/contacts/contacts.types";
import { ChatType } from "@/api/socket/index.types";

export type AddDuoChatParams = {
  id: string;
  createdAt: number;
};

export async function addDuoChat(userId: string) {
  const body = {
    recipient: userId,
  };
  const { data } = await instance.post<AddDuoChatParams>(`chats`, body);
  return data;
}

export async function getChatByUserId(userId: string) {
  const chatId = await instance.get<string>(`chats/${userId}`);
  return chatId.data;
}

export async function getChats(offset: number = 0, limit: number = 10) {
  const { data } = await instance.get<ChatParams[]>(`chats?limit=${limit}&offset=${offset}`);
  const chats = new Map<string, ChatParams>();
  data.forEach(chat => {
    if (chat.type === ChatType.GROUP) {
      chats.set(chat.id, chat);
    } else {
      chats.set(chat.id, {
        ...chat,
        name: chat.user.name,
      });
    }
  });
  return chats;
}

export async function getMessagesByChatId(chatId: string, offset: number = 0, limit: number = 40) {
  const response = await instance.get<MessageParams[]>(`chats/${chatId}/messages?limit=${limit}&offset=${offset}`);
  const messages = new Map<string, MessageParams>();

  response.data.forEach(chat => {
    messages.set(chat.id, chat);
  });
  const totalCount = parseInt(response.headers["x-total-count"], 10);
  return { messages, totalCount };
}

export async function createGroupChat(name: string, description: string, isPrivate: boolean) {
  const body = {
    name,
    description,
    isPrivate,
  };
  const { data } = await instance.post<ChatParams>(`chats/group`, body);
  return data;
}

export async function getGroupChatDetailsById(chatId: string) {
  const { data } = await instance.get<GroupChatDetails>(`chats/${chatId}/group`);
  const members = new Map<number, GroupMember>();
  data.members.forEach(member => {
    members.set(member.id, member);
  });
  return { ...data, members };
}

export async function sendFile(chatId: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);

  const config = {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  };

  const { data } = await instance.post<FileHttpResponse>(`chats/${chatId}/messages/file`, formData, config);
  return data;
}

export async function getFile(messageId: string) {
  const { data } = await instance.get(`chats/messages/${messageId}/file`, { responseType: "blob" });
  return data;
}

export async function getChatById(chatId: string) {
  const { data } = await instance.get<ChatParams>(`chats/generic/${chatId}`);
  return data;
}
