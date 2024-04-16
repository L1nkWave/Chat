import { ChatParams, ContactParams } from "@/api/http/users/users.types";

export type InteractiveListVariant = "contacts" | "chats";

export type Contacts = ContactParams[];

export type InteractiveContactParams = {
  currentContact?: ContactParams;
  contacts?: Contacts;
  onContactClick?: (id: ContactParams) => void;
};

export type InteractiveChatParams = {
  currentChat?: ChatParams;
  chats?: ChatParams[];
  onChatClick?: (id: ChatParams) => void;
};

export type InteractiveListProps = {
  interactiveListVariant: InteractiveListVariant;
  interactiveContact?: InteractiveContactParams;
  interactiveChat?: InteractiveChatParams;
};
