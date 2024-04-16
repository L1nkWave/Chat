import React from "react";

import { Avatar } from "@/components/Avatar/Avatar";
import { InteractiveChatParams } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ChatItem } from "@/components/Chat/UserItem/variants/ChatItem";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { ScrollList } from "@/components/ScrollList/ScrollList";
import { Status } from "@/components/Status/Status";

export function ChatList({ chats }: Readonly<InteractiveChatParams>) {
  if (!chats || chats.length === 0) {
    return <div className="h-full bg-dark-500 flex items-center justify-center">No chats</div>;
  }
  return (
    <ScrollList>
      {chats.map(chat => (
        <ChatItem key={chat.id} chat={chat} />
      ))}
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
    </ScrollList>
  );
}
