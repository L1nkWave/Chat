import { instance } from "@/api/http";

export async function getContacts() {
  const { data } = await instance.get("users/contacts?username=&limit=10&offset=0");
  return data;
}

export async function getUsers() {
  const { data } = await instance.get("users?username=&limit=10&offset=0");
  return data;
}
