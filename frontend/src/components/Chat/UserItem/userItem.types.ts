import { ChatParams, ContactParams } from "@/api/http/users/users.types";

export type UserItemVariant = "chat" | "contact";

export type UserItemProps = {
  variant: UserItemVariant;
  chat?: ChatParams;
  contact?: ContactParams;
};

export type ChatItemProps = {
  chat?: ChatParams;
};

export type ContactItemProps = {
  contact?: ContactParams;
};
