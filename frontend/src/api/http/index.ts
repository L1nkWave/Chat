import axios, { AxiosInstance } from "axios";

import { refreshToken } from "@/api/http/auth/auth";
import { AuthTypes } from "@/api/http/auth/auth.types";
import { isTokenExpired } from "@/helpers/DecodeToken/decodeToken";
import { store } from "@/lib/store";

const baseURL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/";

export const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

instance.interceptors.request.use(
  async config => {
    const newConfig = config;
    const { accessToken } = store.getState().user;
    let validToken = accessToken;
    if (!accessToken || (accessToken && isTokenExpired(accessToken))) {
      const newToken = await refreshToken();
      validToken = newToken.accessToken;
    }
    newConfig.headers.Authorization = `Bearer ${validToken}`;
    return newConfig;
  },
  () => {
    return Promise.reject(new Error("Network timeout"));
  }
);

export async function logout() {
  const { data } = await instance.post<AuthTypes>("auth/logout", {}, { withCredentials: true });
  return data;
}
