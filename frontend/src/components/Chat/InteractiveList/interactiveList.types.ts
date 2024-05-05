import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { ListStateEnum } from "@/components/Chat/chat.types";

export type InteractiveListVariant = ListStateEnum.CONTACTS | ListStateEnum.CHATS | ListStateEnum.FIND_CONTACTS;

export type ContactsMap = Map<number, ContactParams>;
export type UserMap = Map<number, UserParams>;

export type InteractiveContactParams = {
  currentContact?: ContactParams;
  contacts?: ContactsMap;
  onContactClick?: (id: ContactParams) => void;
};

export type InteractiveChatParams = {
  currentChat?: ChatParams;
  chats?: ChatParams[];
  onChatClick?: (id: ChatParams) => void;
};

export type InteractiveGlobalContactParams = {
  currentGlobalUser?: UserParams;
  globalContacts?: UserMap;
  onGlobalContactClick?: (id: UserParams) => void;
};

export type InteractiveListProps = {
  interactiveListVariant: InteractiveListVariant;
  interactiveContact?: InteractiveContactParams;
  interactiveChat?: InteractiveChatParams;
  interactiveFindContacts?: InteractiveGlobalContactParams;
};
