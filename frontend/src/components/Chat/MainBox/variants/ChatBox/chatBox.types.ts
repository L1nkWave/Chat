import { ContactParams, MessageParams } from "@/api/http/contacts/contacts.types";
import { ContactClickHandler, SendMessageClickHandler } from "@/components/Chat/types/handlers.types";

export type ChatBoxProps = {
  contact: ContactParams;
  messages: MessageParams[];
  onSendMessageClick?: SendMessageClickHandler;
  onChatHeaderClick?: ContactClickHandler;
};
