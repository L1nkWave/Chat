import React from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { InteractiveGlobalContactParams } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ContactItem } from "@/components/Chat/UserItem/variants/ContactItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function GlobalContactList({
  globalContacts,
  onGlobalContactClick: handleGlobalContactClick,
  currentGlobalUser,
}: Readonly<InteractiveGlobalContactParams>) {
  let currentGlobalContact: ContactParams | undefined;

  if (!globalContacts || globalContacts.size === 0) {
    return <ScrollList className="justify-center items-center">No contacts</ScrollList>;
  }

  const handleContactClick = (currentContact: ContactParams) => {
    if (handleGlobalContactClick) {
      handleGlobalContactClick(currentContact.user);
    }
  };

  if (currentGlobalUser) {
    currentGlobalContact = {
      alias: undefined,
      addedAt: undefined,
      user: currentGlobalUser,
    };
  }

  return (
    <ScrollList>
      {Array.from(globalContacts.values()).map(globalUserParams => {
        const contact: ContactParams = {
          alias: undefined,
          addedAt: undefined,
          user: globalUserParams,
        };

        return (
          <ContactItem
            contact={contact}
            onClick={handleContactClick}
            key={contact.user.id}
            currentContact={currentGlobalContact}
          />
        );
      })}
    </ScrollList>
  );
}
