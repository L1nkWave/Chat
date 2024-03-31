import React from "react";

import { ChatListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { UserItem } from "@/components/Chat/UserItem/UserItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function ChatList({ chats }: Readonly<ChatListProps>) {
  if (chats && chats?.length === 0) {
    return <div className="h-full bg-dark-500 flex items-center justify-center">No chats</div>;
  }
  return <ScrollList>{chats?.map(chat => <UserItem key={chat.id} variant="chat" chat={chat} />)}</ScrollList>;
}
