import { ChatParams, ContactParams, MessageParams, UserParams } from "@/api/http/contacts/contacts.types";
import { MainBoxStateEnum } from "@/components/Chat/chat.types";
import { LoadMessagesHandler, SendMessageClickHandler } from "@/components/Chat/types/handlers.types";

export type MainBoxVariant = MainBoxStateEnum;

export type MainBoxProps = {
  chatId?: string;
  onSendMessageClick?: SendMessageClickHandler;
  mainBoxVariant: MainBoxVariant;
  contact?: ContactParams;
  chat?: ChatParams;
  globalUser?: UserParams;
  messages: MessageParams[];
  onAddContactClick?: (userId: string, alias: string) => void;
  onRemoveContactClick?: (userId: string) => void;
  onMessageButtonClick?: (userId: string) => void;
  onHeaderClick?: (contactParams: ContactParams) => void;
  loadMessages: LoadMessagesHandler;
};
