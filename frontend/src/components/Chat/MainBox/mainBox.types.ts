import {
  ChatParams,
  ContactParams,
  GroupChatDetails,
  MessageParams,
  UserParams,
} from "@/api/http/contacts/contacts.types";
import { MainBoxStateEnum } from "@/components/Chat/chat.types";
import { ContactsMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import { LoadMessagesHandler, SendMessageClickHandler } from "@/components/Chat/types/handlers.types";

export type MainBoxVariant = MainBoxStateEnum;

export type MainBoxProps = {
  onSendMessageClick?: SendMessageClickHandler;
  mainBoxVariant: MainBoxVariant;
  groupDetails?: GroupChatDetails;
  contact?: ContactParams;
  chat?: ChatParams;
  globalUser?: UserParams;
  messages: MessageParams[];
  onChangeAvatar?: (event: File) => void;
  onAddContactClick?: (userId: number, alias: string) => void;
  onRemoveContactClick?: (userId: number) => void;
  onMessageButtonClick?: (userId: number) => void;
  onHeaderClick?: (contactParams: ContactParams) => void;
  loadMessages: LoadMessagesHandler;
  onAddMemberClick?: (currentChat: ChatParams, currentContact: ContactParams) => void;
  contacts: ContactsMap;
};
