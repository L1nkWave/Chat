import { jwtDecode } from "jwt-decode";

import { DecodedToken } from "@/helpers/DecodeToken/decodeToken.types";

export const decodeToken = (token: string) => {
  try {
    return jwtDecode<DecodedToken>(token);
  } catch (error) {
    return null;
  }
};

export const isTokenExpired = (token: string) => {
  const decodedToken = decodeToken(token);
  if (!decodedToken) return true;

  return Date.now() >= decodedToken.exp * 1000;
};
