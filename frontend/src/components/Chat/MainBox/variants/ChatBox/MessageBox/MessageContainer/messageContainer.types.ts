import { MessageParams } from "@/api/http/contacts/contacts.types";

export interface MessageContainerProps {
  messages: MessageParams[];
  onScrollToBottomButtonClick: () => void;
  showScrollDownButton: boolean;
}
