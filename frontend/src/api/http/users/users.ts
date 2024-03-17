import { instance } from "@/api/http";
import { ContactParams } from "@/api/http/users/users.types";

export async function getContacts() {
  const { data } = await instance.get<ContactParams[]>("users/contacts?username=&limit=10&offset=0");
  return data;
}

export async function getUsers() {
  const { data } = await instance.get("users?username=&limit=10&offset=0");
  return data;
}
