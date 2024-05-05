import { instance } from "@/api/http";
import { ChatParams } from "@/api/http/contacts/contacts.types";

export async function addDuoChat(userId: string) {
  const body = {
    recipient: userId,
  };
  const { data } = await instance.post(`chats`, body);
  return data;
}

export async function getChats(offset: number = 0, limit: number = 10) {
  const { data } = await instance.get<ChatParams[]>(`chats?limit=${limit}&offset=${offset}`);
  return data;
}
