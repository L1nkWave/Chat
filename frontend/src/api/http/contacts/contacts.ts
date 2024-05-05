import { instance } from "@/api/http";
import { ContactParams, UserParams } from "@/api/http/contacts/contacts.types";

export async function getContacts() {
  const { data } = await instance.get<ContactParams[]>("users/contacts?username=&limit=10&offset=0");
  const contacts = new Map<number, ContactParams>();
  data.forEach(contact => {
    contacts.set(contact.user.id, contact);
  });

  return contacts;
}

export async function searchContacts(username: string = "", limit: number = 10, offset: number = 0) {
  const { data } = await instance.get<UserParams[]>(`users?username=${username}&limit=${limit}&offset=${offset}`);
  const users = new Map<number, UserParams>();
  data.forEach(user => {
    users.set(user.id, user);
  });
  return users;
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
