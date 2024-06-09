import { format } from "date-fns";
import React from "react";

import { ChatType } from "@/api/socket/index.types";
import { Avatar } from "@/components/Avatar/Avatar";
import { MessageType } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/Message/Message";
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
  const avatarSrc = "/avatars/group.png";

  return (
    <button
      onClick={handleClick}
      type="button"
      className={`w-full relative flex flex-row justify-center items-center outline-none bg-dark-300 p-4 rounded-lg hover:bg-dark-200 ${className ?? ""}`}
    >
      <div className="flex flex-row w-10/12 mr-4">
        <div className="min-w-max">
          {chat.type === ChatType.DUO ? (
            <Avatar
              item={{
                id: chat.user.id,
                avatarAvailable: chat.avatarAvailable,
              }}
              isAvatarAvailable={chat.avatarAvailable}
              alt="User Avatar"
              online={chat.user.online}
              width={64}
              height={64}
            />
          ) : (
            <Avatar
              item={chat}
              defaultAvatar={avatarSrc}
              isGroupAvatar
              isAvatarAvailable={chat.avatarAvailable}
              alt="Chat Avatar"
              width={64}
              height={64}
            />
          )}
        </div>
        <div className="flex relative w-4/6 flex-col ml-4 justify-center items-start">
          <span className="text-lg inline w-full text-start truncate">{chat.name}</span>
          <span className="text-gray-300 truncate w-full text-start">
            {chat.type === ChatType.DUO ? (
              <>
                {chat.lastMessage && chat.lastMessage.text && chat.lastMessage.text}{" "}
                <span className="text-blue-200">{chat.lastMessage?.action === MessageType.FILE && "<File>"}</span>
              </>
            ) : (
              chat.lastMessage && (
                <>
                  <span className="text-blue-600">{chat.lastMessage.author.name}</span>:{" "}
                  {chat.lastMessage?.action === MessageType.MESSAGE && chat.lastMessage.text}
                  <span className="text-blue-200">{chat.lastMessage?.action === MessageType.FILE && "<File>"}</span>
                </>
              )
            )}
          </span>
        </div>
      </div>

      <div className="min-w-10">
        <p className="text-gray-300 text-sm">
          {chat.lastMessage
            ? format(chat.lastMessage.createdAt * 1000, "HH:mm")
            : format(chat.createdAt * 1000, "HH:mm")}
        </p>
        <div className="flex w-full justify-center items-center bg-blue-300 rounded-lg mt-1">
          {!!chat.unreadMessages && <p className="text-white text-xs p-1">{chat.unreadMessages}</p>}
        </div>
      </div>
    </button>
  );
}
