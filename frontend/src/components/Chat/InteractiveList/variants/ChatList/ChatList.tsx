import React, { useCallback, useEffect, useRef } from "react";

import { InteractiveChatParams } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ChatItem } from "@/components/Chat/UserItem/variants/ChatItem";
import { ScrollList } from "@/components/ScrollList/ScrollList";

export function ChatList({ chats, onChatClick: handleChatClick, loadChats }: Readonly<InteractiveChatParams>) {
  const scrollListRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback(() => {
    const scrollHeight = scrollListRef.current?.scrollHeight ?? 0;
    const scrollTop = scrollListRef.current?.scrollTop ?? 0;
    const clientHeight = scrollListRef.current?.clientHeight ?? 0;

    const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
    const loadThreshold = 450;

    if (distanceFromBottom < loadThreshold && loadChats) {
      loadChats(chats?.size);
    }
  }, [chats, loadChats]);

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
  }, [handleScroll]);

  return (
    <ScrollList ref={scrollListRef}>
      {!chats || chats.size === 0 ? (
        <div className="h-full bg-dark-500 flex items-center justify-center">No chats</div>
      ) : (
        Array.from(chats.values()).map(chat => <ChatItem key={chat.id} chat={chat} onClick={handleChatClick} />)
      )}
    </ScrollList>
  );
}
