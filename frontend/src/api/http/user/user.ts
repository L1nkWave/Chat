import { instance, LIST_PAGINATION_LIMIT } from "@/api/http";
import { UserParams } from "@/api/http/contacts/contacts.types";
import { UserMap } from "@/components/Chat/InteractiveList/interactiveList.types";

export async function searchUser(username: string = "", limit: number = LIST_PAGINATION_LIMIT, offset: number = 0) {
  const { data } = await instance.get<UserParams[]>(`users?username=${username}&limit=${limit}&offset=${offset}`);
  const users = new Map() as UserMap;
  data.forEach(user => {
    users.set(user.id, user);
  });
  return users;
}

export async function getUserById(userId: string) {
  const { data } = await instance.get<UserParams>(`users/${userId}`);

  return data;
}

export async function uploadAvatar(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await instance.post("users/avatar", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
    },
  });
  return data;
}

export async function uploadGroupAvatar(file: File, id: string) {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await instance.post(`chats/${id}/group/avatar`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
    },
  });
  return data;
}

export async function getAvatar(id: string) {
  const { data } = await instance.get(`users/${id}/avatar`, { responseType: "blob" });
  return data;
}

export async function getGroupAvatar(id: string) {
  const { data } = await instance.get(`chats/${id}/group/avatar`, { responseType: "blob" });
  return data;
}
