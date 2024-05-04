import { ChatParams, ContactParams } from "@/api/http/contacts/contacts.types";
import {
  InteractiveChatParams,
  InteractiveContactParams,
} from "@/components/Chat/InteractiveList/interactiveList.types";

export type UserItemVariant = "chat" | "contact";

export type UserItemProps = {
  className?: string;
};

export type ChatItemProps = {
  chat?: ChatParams;
  onClick?: InteractiveChatParams["onChatClick"];
} & UserItemProps;

export type ContactItemProps = {
  currentContact?: ContactParams;
  contact?: ContactParams;
  onClick?: InteractiveContactParams["onContactClick"];
} & UserItemProps;
