import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { ListStateEnum } from "@/components/Chat/chat.types";

export type InteractiveListVariant = ListStateEnum.CONTACTS | ListStateEnum.CHATS | ListStateEnum.FIND_CONTACTS;

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

export type InteractiveGlobalContactParams = {
  currentGlobalUser?: UserParams;
  globalContacts?: UserParams[];
  onGlobalContactClick?: (id: UserParams) => void;
};

export type InteractiveListProps = {
  interactiveListVariant: InteractiveListVariant;
  interactiveContact?: InteractiveContactParams;
  interactiveChat?: InteractiveChatParams;
  interactiveFindContacts?: InteractiveGlobalContactParams;
};
