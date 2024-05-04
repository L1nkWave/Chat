import { ContactParams } from "@/api/http/contacts/contacts.types";

export const getContactName = (contact: ContactParams) => {
  return contact.alias ? contact.alias : contact.user.name;
};
