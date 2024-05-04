import { instance } from "@/api/http";
import { UserParams } from "@/api/http/contacts/contacts.types";
import { Contacts } from "@/components/Chat/InteractiveList/interactiveList.types";

export async function getContacts() {
  const { data } = await instance.get<Contacts>("users/contacts?username=&limit=10&offset=0");
  return data;
}

export async function searchContacts(username: string = "", limit: number = 10, offset: number = 0) {
  const { data } = await instance.get<UserParams[]>(`users?username=${username}&limit=${limit}&offset=${offset}`);
  return data;
}

export async function addContact(userId: string, alias: string) {
  const body = {
    userId,
    alias,
  };

  const { data } = await instance.post<UserParams>(`users/contacts`, body);
  return data;
}

export async function removeContact(userId: string) {
  const { data } = await instance.delete<UserParams>(`users/contacts/${userId}`);
  return data;
}
