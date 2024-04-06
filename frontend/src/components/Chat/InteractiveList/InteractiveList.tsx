import React from "react";

import { Avatar } from "@/components/Avatar/Avatar";
import { InteractiveListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ChatList } from "@/components/Chat/InteractiveList/variants/ChatList/chatList";
import { ContactList } from "@/components/Chat/InteractiveList/variants/ContactList/contactList";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Status } from "@/components/Status/Status";

export function InteractiveList({ interactiveListVariant, contacts, chats }: Readonly<InteractiveListProps>) {
  let interactiveList;

  if (interactiveListVariant === "contacts") {
    interactiveList = <ContactList contacts={contacts} />;
  } else {
    interactiveList = <ChatList chats={chats} />;
  }

  return (
    <div className="h-screen flex flex-col w-[55%]">
      <div className="h-screen flex flex-col">{interactiveList}</div>
      {interactiveListVariant === "chats" && (
        <div className="bg-dark-400 px-8 py-4 left-0 bottom-0 w-full flex justify-between items-center h-auto">
          <div className="flex items-center gap-2">
            <Avatar
              item={{
                id: 1,
                avatarPath: undefined,
              }}
              alt="Avatar"
              status={false}
            />
            <div className="flex flex-col gap-0">
              <p className="text-lg">Name</p>
              <div className="flex items-center text-gray-300">
                <Status />
                Active
              </div>
            </div>
          </div>
          <CustomButton
            className="px-1 py-1 text-blue-300 dark:bg-dark-250 w-10 text-2x"
            iconSize={32}
            variant="square"
            icon="sign-out-circle"
          />
        </div>
      )}
    </div>
  );
}
