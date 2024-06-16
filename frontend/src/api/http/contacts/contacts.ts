import { instance, LIST_PAGINATION_LIMIT } from "@/api/http";
import { ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { ContactsMap } from "@/components/Chat/InteractiveList/interactiveList.types";

export async function getContacts(search: string = "", limit = LIST_PAGINATION_LIMIT, offset = 0) {
  const { data } = await instance.get<ContactParams[]>(
    `users/contacts?search=${search}&limit=${limit}&offset=${offset}`
  );
  const contacts = new Map() as ContactsMap;
  data.forEach(contact => {
    contacts.set(contact.user.id, contact);
  });

  return contacts;
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
