import axios, { AxiosInstance, AxiosResponse } from "axios";

const baseURL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1/";

export const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor
instance.interceptors.request.use(
  config => {
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Response interceptor
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    // You can modify the response here (e.g., handling global errors)
    return response;
  },
  error => {
    // You can handle errors globally here
    return Promise.reject(error);
  }
);
