import { ContactParams } from "@/api/http/contacts/contacts.types";

export type UserInfoBoxProps = {
  contact: ContactParams;
  onAddContactClick?: (userId: string, alias: string) => void;
  onRemoveContactClick?: (userId: string) => void;
};
