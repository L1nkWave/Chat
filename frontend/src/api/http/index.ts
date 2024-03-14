import axios, { AxiosInstance } from "axios";

import { store } from "@/lib/store";

const baseURL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/";

export const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    "ngrok-skip-browser-warning": true,
  },
});

// Request interceptor for authInstance
instance.interceptors.request.use(
  config => {
    const newConfig = config;

    newConfig.headers.Authorization = `Bearer ${store.getState().auth.accessToken}`;
    return newConfig;
  },
  error => {
    return Promise.reject(error);
  }
);
