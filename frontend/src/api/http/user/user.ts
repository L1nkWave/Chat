import { instance, LIST_PAGINATION_LIMIT } from "@/api/http";
import { UserParams } from "@/api/http/contacts/contacts.types";

export async function searchUser(username: string = "", limit: number = LIST_PAGINATION_LIMIT, offset: number = 0) {
  const { data } = await instance.get<UserParams[]>(`users?username=${username}&limit=${limit}&offset=${offset}`);
  const users = new Map<string, UserParams>();
  data.forEach(user => {
    users.set(user.id, user);
  });
  return users;
}

export async function getUserById(userId: string) {
  const { data } = await instance.get<UserParams>(`users/${userId}`);
  return data;
}
