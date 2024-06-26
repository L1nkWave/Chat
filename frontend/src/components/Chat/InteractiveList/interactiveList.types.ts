import { ChatParams, ContactParams, MessageParams, UserParams } from "@/api/http/contacts/contacts.types";
import { ListStateEnum } from "@/components/Chat/chat.types";
import {
  ChatClickHandler,
  ContactClickHandler,
  LoadChatsHandler,
  LoadContactsHandler,
  UserClickHandler,
} from "@/components/Chat/types/handlers.types";

export type InteractiveListVariant = ListStateEnum;

export type ContactsMap = Map<number, ContactParams>;
export type UserMap = Map<number, UserParams>;
export type ChatMap = Map<string, ChatParams>;
export type MessagesMap = Map<string, MessageParams>;

export type InteractiveContactParams = {
  currentContact?: ContactParams;
  contacts?: ContactsMap;
  onContactClick?: ContactClickHandler;
  loadContacts?: LoadContactsHandler;
};

export type InteractiveChatParams = {
  currentChat?: ChatParams;
  chats?: ChatMap;
  onChatClick?: ChatClickHandler;
  loadChats?: LoadChatsHandler;
};

export type InteractiveGlobalContactParams = {
  currentGlobalUser?: UserParams;
  globalContacts?: UserMap;
  onGlobalContactClick?: UserClickHandler;
  loadGlobalContacts?: LoadContactsHandler;
};

export type InteractiveListProps = {
  searchValue?: string;
  onChangeSearchValue: (value: string) => void;
  interactiveListVariant: InteractiveListVariant;
  interactiveContact?: InteractiveContactParams;
  interactiveChat?: InteractiveChatParams;
  interactiveFindContacts?: InteractiveGlobalContactParams;
};
