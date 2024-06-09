import { ChatParams, MessageParams } from "@/api/http/contacts/contacts.types";

export interface MessageContainerProps {
  messages: MessageParams[];
  onScrollToBottomButtonClick: () => void;
  chat: ChatParams;
  showScrollDownButton: boolean;
}
