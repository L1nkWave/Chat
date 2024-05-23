import { ChatParams, ContactParams, MessageParams, UserParams } from "@/api/http/contacts/contacts.types";
import { ListStateEnum } from "@/components/Chat/chat.types";
import { ChatClickHandler, ContactClickHandler, UserClickHandler } from "@/components/Chat/types/handlers.types";

export type InteractiveListVariant = ListStateEnum.CONTACTS | ListStateEnum.CHATS | ListStateEnum.FIND_CONTACTS;

export type ContactsMap = Map<number, ContactParams>;
export type UserMap = Map<number, UserParams>;
export type ChatMap = Map<string, ChatParams>;
export type MessagesMap = Map<string, MessageParams>;

export type InteractiveContactParams = {
  currentContact?: ContactParams;
  contacts?: ContactsMap;
  onContactClick?: ContactClickHandler;
};

export type InteractiveChatParams = {
  currentChat?: ChatParams;
  chats?: ChatMap;
  onChatClick?: ChatClickHandler;
};

export type InteractiveGlobalContactParams = {
  currentGlobalUser?: UserParams;
  globalContacts?: UserMap;
  onGlobalContactClick?: UserClickHandler;
};

export type InteractiveListProps = {
  interactiveListVariant: InteractiveListVariant;
  interactiveContact?: InteractiveContactParams;
  interactiveChat?: InteractiveChatParams;
  interactiveFindContacts?: InteractiveGlobalContactParams;
};
