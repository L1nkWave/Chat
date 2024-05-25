import React, { useCallback, useEffect, useRef, useState } from "react";

import { InteractiveContactParams } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ContactItem } from "@/components/Chat/UserItem/variants/ContactItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function ContactList({
  contacts,
  onContactClick: handleContactClick,
  currentContact,
  loadContacts,
}: Readonly<InteractiveContactParams>) {
  const [loading, setLoading] = useState(false);
  const scrollListRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback(() => {
    const scrollHeight = scrollListRef.current?.scrollHeight ?? 0;
    const scrollTop = scrollListRef.current?.scrollTop ?? 0;
    const clientHeight = scrollListRef.current?.clientHeight ?? 0;

    const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
    const loadThreshold = 450;

    if (distanceFromBottom < loadThreshold && !loading) {
      setLoading(true);
      if (loadContacts && contacts) {
        loadContacts("", contacts.size);
      }
    }
  }, [contacts, loadContacts, loading]);

  useEffect(() => {
    const scrollList = scrollListRef.current;
    if (scrollList) {
      scrollList.addEventListener("scroll", handleScroll);
    }
    return () => {
      if (scrollList) {
        scrollList.removeEventListener("scroll", handleScroll);
      }
    };
  }, [handleScroll, loading]);

  if (!contacts || contacts.size === 0) {
    return <ScrollList className="justify-center items-center">No contacts</ScrollList>;
  }

  return (
    <ScrollList ref={scrollListRef}>
      {Array.from(contacts.values()).map(contact => (
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
