import React from "react";

import { ContactListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { UserItem } from "@/components/Chat/UserItem/UserItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function ContactList({ contacts }: Readonly<ContactListProps>) {
  if (contacts && contacts?.length === 0) {
    return <ScrollList>No contacts</ScrollList>;
  }
  return (
    <ScrollList>
      {contacts?.map(contact => <UserItem key={contact.user.id} contact={contact} variant="contact" />)}
    </ScrollList>
  );
}
