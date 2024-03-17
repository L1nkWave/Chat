import { ChatParams, ContactParams } from "@/api/http/users/users.types";

export type InteractiveListVariant = "contacts" | "chats";

export type ContactListProps = {
  contacts?: ContactParams[];
};

export type ChatListProps = {
  chats?: ChatParams[];
};

export type InteractiveListProps = {
  interactiveListVariant: InteractiveListVariant;
  contacts?: ContactParams[];
  chats?: ChatParams[];
};
