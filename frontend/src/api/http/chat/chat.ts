import { instance } from "@/api/http";
import { ChatParams, MessageParams } from "@/api/http/contacts/contacts.types";

export type AddDuoChatParams = {
  id: string;
  createdAt: number;
};

export async function addDuoChat(userId: string) {
  const body = {
    recipient: userId,
  };
  const { data } = await instance.post<AddDuoChatParams>(`chats`, body);
  console.log("add", data);
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
    chats.set(chat.id, chat);
  });
  console.log(chats);
  return chats;
}

export async function getMessagesByChatId(chatId: string, limit: number = 40, offset: number = 0) {
  const { data } = await instance.get<MessageParams[]>(`chats/${chatId}/messages?limit=${limit}&offset=${offset}`);
  const messages = new Map<string, MessageParams>();
  data.forEach(chat => {
    messages.set(chat.id, chat);
  });
  return messages;
}
