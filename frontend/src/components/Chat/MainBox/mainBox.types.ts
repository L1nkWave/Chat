import { ChatParams, ContactParams } from "@/api/http/users/users.types";

export type MainBoxVariant = "chat" | "user-info" | "settings" | "empty";

export type MainBoxProps = {
  mainBoxVariant: MainBoxVariant;
  contact?: ContactParams;
  chat?: ChatParams;
};
