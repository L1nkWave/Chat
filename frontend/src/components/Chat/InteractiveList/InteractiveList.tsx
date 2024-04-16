import React from "react";

import { InteractiveListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ChatList } from "@/components/Chat/InteractiveList/variants/ChatList/ChatList";
import { ContactList } from "@/components/Chat/InteractiveList/variants/ContactList/ContactList";

export function InteractiveList({
  interactiveListVariant,
  interactiveChat,
  interactiveContact,
}: Readonly<InteractiveListProps>) {
  let interactiveList;

  if (interactiveListVariant === "contacts") {
    interactiveList = (
      <ContactList
        contacts={interactiveContact?.contacts}
        onContactClick={interactiveContact?.onContactClick}
        currentContact={interactiveContact?.currentContact}
      />
    );
  } else if (interactiveListVariant === "chats") {
    interactiveList = <ChatList chats={interactiveChat?.chats} />;
  } else {
    interactiveList = <div>No data</div>;
  }

  return (
    <div className="h-screen flex flex-col min-w-72 w-[55%]">
      <div className="h-screen flex flex-col">{interactiveList}</div>
    </div>
  );
}
