import React from "react";

import { ContactListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { UserItem } from "@/components/Chat/UserItem/UserItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";
import { setCurrentMainBox } from "@/lib/features/chat/chatSlice";
import { useAppDispatch } from "@/lib/hooks";

export function ContactList({ contacts }: Readonly<ContactListProps>) {
  const dispatch = useAppDispatch();

  const handleContactClick = () => {
    dispatch(setCurrentMainBox("user-info"));
  };

  if (contacts && contacts?.length === 0) {
    return <ScrollList>No contacts</ScrollList>;
  }

  return (
    <ScrollList>
      {contacts?.map(contact => (
        <UserItem onClick={handleContactClick} key={contact.user.id} contact={contact} variant="contact" />
      ))}
    </ScrollList>
  );
}
