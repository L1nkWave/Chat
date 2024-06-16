import { ChatParams, ContactParams, GroupChatDetails, MessageParams } from "@/api/http/contacts/contacts.types";
import { ContactsMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import {
  ContactClickHandler,
  LoadMessagesHandler,
  SendMessageClickHandler,
} from "@/components/Chat/types/handlers.types";

export type ChatBoxProps = {
  contact?: ContactParams;
  messages: MessageParams[];
  onSendMessageClick?: SendMessageClickHandler;
  onChatHeaderClick?: ContactClickHandler;
  loadMessages: LoadMessagesHandler;
  chat: ChatParams;
  groupDetails?: GroupChatDetails;
  onAddMemberClick?: (currentChat: ChatParams, currentContact: ContactParams) => void;
  contacts: ContactsMap;
};
