import axios from "axios";

import {AuthTypes} from "@/api/http/auth/auth.types";

export const authInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

export async function signUp(name: string, username: string, password: string) {
  const body = {
    name,
    username,
    password,
  };

  const { data } = await authInstance.post("users/register", body);
  return data;
}

export async function signIn(username: string, password: string) {
  const body = {
    username,
    password,
  };

  const data = await authInstance.post<AuthTypes>("auth/login", body, { withCredentials: true });
  return data.data;
}

export async function refreshToken() {
  const { data } = await authInstance.post<AuthTypes>(
    "auth/refresh-tokens",
    {},
    {
      withCredentials: true,
    }
  );
  return data;
}
