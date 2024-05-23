import { format } from "date-fns";
import React from "react";

import { Avatar } from "@/components/Avatar/Avatar";
import { ChatItemProps } from "@/components/Chat/UserItem/userItem.types";

export function ChatItem({ chat, onClick, className }: Readonly<ChatItemProps>) {
  if (!chat) {
    return null;
  }
  const handleClick = () => {
    if (onClick) {
      onClick(chat);
    }
  };
  return (
    <button
      onClick={handleClick}
      type="button"
      className={`w-full relative flex flex-row justify-center items-center outline-none bg-dark-300 p-4 rounded-lg hover:bg-dark-200 ${className ?? ""}`}
    >
      <div className="flex flex-row w-10/12 mr-4">
        <div className="min-w-max">
          <Avatar item={chat.user} alt="User Avatar" online={chat.user.online} />
        </div>
        <div className="flex relative w-4/6 flex-col ml-4 justify-center items-start">
          <span className="text-lg inline w-full text-start truncate">{chat.user.name}</span>
          <span className="text-gray-300 truncate w-full text-start">{chat.lastMessage?.text}</span>
        </div>
      </div>

      <div className="min-w-10">
        <p className="text-gray-300 text-sm">{format(chat.createdAt, "HH:mm")}</p>
        <div className="flex w-full justify-center items-center bg-blue-300 rounded-lg mt-1">
          {!!chat.unreadMessages && <p className="text-white text-xs p-1">{chat.unreadMessages}</p>}
        </div>
      </div>
    </button>
  );
}
