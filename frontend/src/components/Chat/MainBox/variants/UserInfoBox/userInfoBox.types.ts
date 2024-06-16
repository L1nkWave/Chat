import { ContactParams } from "@/api/http/contacts/contacts.types";

export type UserInfoBoxProps = {
  contact: ContactParams;
  onAddContactClick?: (userId: number, alias: string) => void;
  onRemoveContactClick?: (userId: number) => void;
  onMessageButtonClick?: (userId: number) => void;
};
