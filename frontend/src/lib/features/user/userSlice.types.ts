import { JwtPayload } from "jwt-decode";

import { UserParams } from "@/api/http/contacts/contacts.types";

export enum UserStatus {
  ONLINE = "Connected",
  OFFLINE = "Disconnected",
}

export type TokenParams = {
  "token-id": string;
  "user-id": string;
  authorities: string[];
} & JwtPayload;

export type UserStateParams = {
  accessToken: string | null;
  currentUser: UserParams | null;
};
