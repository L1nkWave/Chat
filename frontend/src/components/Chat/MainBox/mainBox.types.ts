import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";

export type MainBoxVariant = "chat" | "user-info" | "settings" | "empty";

export type MainBoxProps = {
  mainBoxVariant: MainBoxVariant;
  contact?: ContactParams;
  chat?: ChatParams;
  globalUser?: UserParams;
  onAddContactClick?: (userId: string, alias: string) => void;
  onRemoveContactClick?: (userId: string) => void;
};
