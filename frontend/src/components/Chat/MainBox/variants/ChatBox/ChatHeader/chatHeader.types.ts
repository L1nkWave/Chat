import { ContactParams } from "@/api/http/contacts/contacts.types";
import { ContactClickHandler } from "@/components/Chat/types/handlers.types";

export type ChatHeaderProps = {
  contact: ContactParams;
  onChatHeaderClick?: ContactClickHandler;
};
