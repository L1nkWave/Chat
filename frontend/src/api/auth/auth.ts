import { instance } from "@/api";
import { AuthTypes } from "@/api/auth/auth.types";

export async function signUp(name: string, username: string, password: string) {
  const body = {
    name,
    username,
    password,
  };

  const { data } = await instance.post("users/register", body);
  return data;
}

export async function signIn(username: string, password: string) {
  const body = {
    username,
    password,
  };

  const { data } = await instance.post<AuthTypes>("auth/login", body);
  return data;
}
