import { ChatParams, ContactParams, GroupChatDetails } from "@/api/http/contacts/contacts.types";
import { ContactsMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ContactClickHandler } from "@/components/Chat/types/handlers.types";

export type ChatHeaderProps = {
  contact?: ContactParams;
  chat: ChatParams;
  onChatHeaderClick?: ContactClickHandler;
  groupDetails?: GroupChatDetails;
  onAddMemberClick?: (currentChat: ChatParams, currentContact: ContactParams) => void;
  contacts: ContactsMap;
};
