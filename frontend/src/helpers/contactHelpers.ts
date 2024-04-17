import { ContactParams } from "@/api/http/users/users.types";

export const getContactName = (contact: ContactParams) => {
  return contact.alias ? contact.alias : contact.user.name;
};
