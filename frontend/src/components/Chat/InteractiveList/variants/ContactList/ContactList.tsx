import React from "react";

import { InteractiveContactParams } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ContactItem } from "@/components/Chat/UserItem/variants/ContactItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function ContactList({
  contacts,
  onContactClick: handleContactClick,
  currentContact,
}: Readonly<InteractiveContactParams>) {
  if (!contacts || contacts.length === 0) {
    return <ScrollList className="justify-center items-center">No contacts</ScrollList>;
  }

  return (
    <ScrollList>
      {contacts?.map(contact => (
        <ContactItem
          contact={contact}
          onClick={handleContactClick}
          key={contact.user.id}
          currentContact={currentContact}
        />
      ))}
    </ScrollList>
  );
}
