"use client";

import React, { useCallback, useEffect, useRef, useState } from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { InteractiveGlobalContactParams } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ContactItem } from "@/components/Chat/UserItem/variants/ContactItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function GlobalContactList({
  globalContacts,
  onGlobalContactClick: handleGlobalContactClick,
  currentGlobalUser,
  loadGlobalContacts,
}: Readonly<InteractiveGlobalContactParams>) {
  const [loading, setLoading] = useState(false);
  const scrollListRef = useRef<HTMLDivElement>(null);
  let currentGlobalContact: ContactParams | undefined;

  const handleScroll = useCallback(() => {
    const scrollHeight = scrollListRef.current?.scrollHeight ?? 0;
    const scrollTop = scrollListRef.current?.scrollTop ?? 0;
    const clientHeight = scrollListRef.current?.clientHeight ?? 0;

    const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
    const loadThreshold = 450;

    if (distanceFromBottom < loadThreshold) {
      setLoading(true);
      if (loadGlobalContacts && globalContacts) {
        loadGlobalContacts("", globalContacts.size);
      }
    }
  }, [globalContacts, loadGlobalContacts, loading]);

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
    <ScrollList ref={scrollListRef}>
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
