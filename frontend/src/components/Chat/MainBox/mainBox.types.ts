import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { MainBoxStateEnum } from "@/components/Chat/chat.types";

export type MainBoxVariant = MainBoxStateEnum;

export type MainBoxProps = {
  mainBoxVariant: MainBoxVariant;
  contact?: ContactParams;
  chat?: ChatParams;
  globalUser?: UserParams;
  onAddContactClick?: (userId: string, alias: string) => void;
  onRemoveContactClick?: (userId: string) => void;
  onMessageButtonClick?: (userId: string) => void;
};
