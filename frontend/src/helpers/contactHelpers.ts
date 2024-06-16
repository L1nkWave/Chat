import { ContactParams } from "@/api/http/contacts/contacts.types";

export const getContactName = (contact: ContactParams) => {
  return contact.alias ? contact.alias : contact.user.name;
};

export const lastSeenDateNow = () => {
  return Math.floor(Date.now() / 1000);
};
