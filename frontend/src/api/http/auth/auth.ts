import { instance } from "@/api/http";
import { AuthTypes } from "@/api/http/auth/auth.types";

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

  const { data } = await instance.post<AuthTypes>("auth/login", body, { withCredentials: true });
  return data;
}

export async function refreshToken() {
  const { data } = await instance.post<AuthTypes>("auth/refresh-tokens", {}, { withCredentials: true });
  return data;
}

export async function logout() {
  const { data } = await instance.post<AuthTypes>("auth/logout", {}, { withCredentials: true });
  return data;
}
